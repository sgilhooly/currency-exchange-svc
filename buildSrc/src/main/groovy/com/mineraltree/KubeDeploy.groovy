package com.mineraltree

import groovy.io.FileType
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Plugin

/**
 * Settings for the KubeDeploy plugin
 */
class KubeDeployExtensions {

  private final Project project

  def serviceConfigDir = "services"
  def outputDestination = "${project.buildDir}/kube-configs/"

  KubeDeployExtensions(Project project) {
    this.project = project
  }
}

/**
 * Plugin to manage deployment of services in a Kubernetes instance. This plugin accepts input files
 * which describe the available services. It will then generate kubectl deployment descriptors for them
 * and launch them into the instance.
 */
class KubeDeploy implements Plugin<Project> {

  /**
   * Main plugin entry point
   */
  @Override
  void apply(Project project) {
    // Add the extension object which allows buildscripts to configure this plugin
    def extension = project.extensions.create('k8deploy', KubeDeployExtensions, project)

    // The main top task which performs the full deployment of the requested services
    def topTask = project.task(type: DeployTask, "k8deploy") {
      group "Deployment"
      description "Deploy the project services to a kubernetes cluster"
      outputDestination = extension.outputDestination
    }

    def configTask = project.task("all-configs") {
      group "Deployment"
      description "Generate the kubernetes deployment descriptor files"
    }
    topTask.dependsOn(configTask)

    def namespaceConfigTask  = project.task(type: GenerateNamespaceDescriptor, "gen-namespace") {
      moduleName = "namespace"
      outputDestination = extension.outputDestination
    }
    def namespaceDeployTask = project.task(type: DeployTask, "deployNamespace") {
      dependsOn(namespaceConfigTask)
      group "Deployment"
      description "Create the Kubernetes namespace used to deploy the project to"
      deployConfigs = ["namespace.json"]
      outputDestination = extension.outputDestination
    }
    topTask.dependsOn(namespaceDeployTask)

    def appConfigTask = project.task(type: GenerateConfigMap, "gen-app-config") {
      moduleName = "config"
      propertyName = "application.conf"
      secret = false
      outputName = "config.json"
      configSection = "app"
      outputDestination = extension.outputDestination
    }

    def secretTask = project.task(type: GenerateConfigMap, "gen-secret-config") {
      moduleName = "secrets"
      propertyName = "secrets.conf"
      secret = true
      outputName = "secrets.json"
      configSection = "secret"
      outputDestination = extension.outputDestination
    }
    configTask.dependsOn(namespaceConfigTask, appConfigTask, secretTask)

    def configDeployTask = project.task(type: DeployTask, "deployConfigs") {
      dependsOn(namespaceDeployTask, appConfigTask, secretTask)
      group "Deployment"
      description "Deploy the configuration and secrets resources"
      deployConfigs = ["config.json", "secrets.json"]
      outputDestination = extension.outputDestination
    }
    topTask.dependsOn(configDeployTask)

    // Scan the input directory and create a task for each one which will generate its
    // deployment descriptor
    def allConfigs = []
    project.file(extension.serviceConfigDir).eachFileMatch(FileType.FILES, ~/.*\.conf$/) { conf ->
      eachConf:
      {
        def parsedFile = (conf.name =~ /(.*)\.conf$/)
        if (!parsedFile.matches()) {
          throw new InvalidUserDataException("Invalid service definition filename. Expected \"servicename.conf\" (e.g. \"myservice.conf\") but found ${conf.name}.")
        }
        def filenameModule = parsedFile[0][1]

        def genTask = project.task(type: GenerateServiceDescriptor, "gen-${conf.name[0..-6]}") {
          moduleName = filenameModule
          inputDefinition = conf.toString()
          outputDestination = extension.outputDestination
        }
        allConfigs += genTask.outputs.files
        configTask.dependsOn(genTask)
      }
    }
    topTask.deployConfigs = allConfigs

    // Create a task rule so callers can individually deploy a configuraiton using "deploy-<configName>"
    project.tasks.addRule("Pattern: deploy-<configName>", { String name ->
      if (name.startsWith("deploy-")) {
        project.task(type: DeployTask, name) {
          dependsOn configDeployTask
          dependsOn configTask
          deployConfigs = ["${name - 'deploy-'}.json"]
          outputDestination = extension.outputDestination
        }
      }
    })

    // Define a top-level task which can generate the sample build configuration file
    project.task("sampleConfig") {
      group "Deployment"
      description "Generates a sample user configuration file for performing deployments"
      doLast {
        final InputStream resource = getClass().getClassLoader().getResourceAsStream("release-template.conf")
        File result = new File("sample-config.conf")
        result.text = resource.text
        println "Generated 'sample-config.conf' Modify this file to your needs and save it as ~/.config/mineraltree/mt-release.conf"
      }
    }

    // Define a top-level task which can delete certain or all services in the current context, used outside
    // of generation/deployment phase
    project.task(type: CleanTask, "cleanServices") {
      group "Deployment"
      description "Delete Auth-Service services in the current context"
      dependsOn configTask
      outputDestination = extension.outputDestination
    }
  }
}
