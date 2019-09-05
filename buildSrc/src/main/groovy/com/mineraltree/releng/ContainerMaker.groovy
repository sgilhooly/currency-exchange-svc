package com.mineraltree.releng

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync

/**
 * Custom gradle plugin which generates a standard MineralTree container image. This plugin
 * repackages a 3rd party docker plugin and configures it to generate a container image which
 * follows the MineralTree conventions for controlling a container environment.
 * </p>
 * In particular, it creates a {@code /templates} directory in the image and installs a
 * {@code /usr/bin/service-run.sh} script which are used together to enable the container
 * runtime to configure itself with the necessary environment settings.
 * <p>
 * Configuring this plugin requires a {@code container} section of the project build which declares
 * the base image to use (as the "FROM" instruction) for the container. For example:
 * <pre>
 *     container {*         baseImage = "ubuntu:latest"
 *}* </pre>
 */
class ContainerMaker implements Plugin<Project> {

  /**
   * Main plugin entry point. This method is called when this plugin is applied to a project.
   *
   * @param project
   *      the project to which this plugin was applied
   */
  @Override
  void apply(Project project) {

    // Load the 3rd party docker plugin
    project.pluginManager.apply('com.bmuschko.docker-remote-api')

    // Declare the 'container' configuraton block and attach it to a ContainerMakerExtension class
    ContainerMakerExtension extension = project.extensions.create('container', ContainerMakerExtension)

    // Create the task which generates the Dockerfile
    def dockerfileTask = project.task(type: Dockerfile, 'buildDockerfile') {
      destFile = project.layout.buildDirectory.file("docker/Dockerfile")
      from extension.baseImage.map { i -> new Dockerfile.From(i) }
      label(['maintainer': 'MineralTree Inc. <info@mineraltree.com>]'])

      copyFile 'templates', '/templates'
      copyFile 'service-run.sh', '/usr/bin/'
      runCommand "chmod 755 /usr/bin/service-run.sh"
      entryPoint "/usr/bin/service-run.sh"
      defaultCommand extension.defaultCommand
      outputs.upToDateWhen { false }
    }

    // Create the task which assembles the container build directory
    def stageTask = project.task(type: Sync, "stageContainer") {
      into(dockerfileTask.destDir)

      from(project.layout.projectDirectory.file("src/templates")) {
        into "templates/"
      }
      doLast {
        // Copy the service-run.sh script
        final InputStream resource = getClass().getClassLoader().getResourceAsStream('service-run.sh')
        File destination = new File(dockerfileTask.destDir.get().toString(), 'service-run.sh')
        destination.text = resource.text
      }
    }
    // the stage task wipes out the container build directory so need to kick off the dockerfile
    // generation task to make sure that is in the stage
    stageTask.finalizedBy(dockerfileTask)

    // Create the task which generates the container image.
    def buildTask = project.task(type: DockerBuildImage, dependsOn: [dockerfileTask, stageTask], 'buildContainer') {

      def ecrRegistry = System.getenv("ECR_REGISTRY")
      def ecrRepo = System.getenv("ECR_REPO")

      dependsOn dockerfileTask, stageTask
      description "Builds this project as a Docker container image"
      group "Docker"
      inputDir = dockerfileTask.destDir
      tags.add "mineraltree/${project.name}:${project.version}"
      tags.add "${ecrRegistry}/${ecrRepo}:${project.version}"
    }
  }
}

