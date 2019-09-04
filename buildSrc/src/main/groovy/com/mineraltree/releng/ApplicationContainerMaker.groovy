package com.mineraltree.releng

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Compression

/**
 * Custom plugin to create a Java application and bundle it into a container image.
 *
 * This plugin includes and configures the Application plugin as well as the custom
 * {@link ContainerMaker} plugin. They are configured with the common settings needed
 * to build mineraltree containers from a Java-based application.
 *
 * <h3>Configuring this plugin</h3>
 * Configuration of this plugin at a minimum should include the basic configuration of
 * the application plugin. This should include the application name and the main class name
 * (the name of the class which has the {@code static void main(String args[])} method to call)
 * For example:
 * <pre>
 *     mainClassName = "com.mineraltree.example.SomeMainClass"
 *     applicationName = "sample-app"
 *     applicationDefaultJvmArgs = [ '-Xms1g', '-Xmx2g' ]
 * </pre>
 *
 * To configure additional start scripts in the application, define an {@code entryPoint} map which
 * declares the additional main classes and their names:
 * <pre>
 *     entryPoints {
 *        "example-app-one"  {
 *            mainClass = "com.mineraltree.example.AnotherMainClass"
 *            jvmArgs = [ '-Xms2g', '-Xmx4g' ]
 *         }
 *         "second-example" {
 *            mainClass = "com.mineraltree.example.ExampleThing"
 *         }
 *         quotesNotAlwaysNeeded {
 *            mainClass = "com.mineraltree.example.LastExample"
 *         }
 *     }
 * </pre>
 *
 * If {@code jvmArgs} is not given for an entry point it will default to the base default application's
 * JVM arguments.
 */
class ApplicationContainerMaker implements Plugin<Project> {

  /**
   * Main entry point for the container. This method is called when the {@code build.gradle}
   * applies this plugin to a project
   *
   * @param project
   *      the gradle project to which this plugin was applied
   */
  @Override
  void apply(Project project) {

    // automatically apply the ContainerMaker and application plugins.
    project.pluginManager.apply(ContainerMaker.class)
    project.pluginManager.apply('application')

    // Create a container map which will allow users to define additional entry points for
    // this application + container. An "entry point" in this context is an additional
    // shell script in the application which launches a java class
    def entryPoints = project.container(EntryPoint)
    entryPoints.all {
      mainClass = "NO_MAIN_CLASS"
      jvmArgs = project.convention.getPlugins().get('application').applicationDefaultJvmArgs
    }
    project.extensions.entryPoints = entryPoints

    // helper routine to modify a start script to configure the classpath as needed
    def overrideScriptFiles = {
      // Now we can find the placeholder we inserted earlier and remove the extra 'lib' before it
      def windowsScriptFile = project.file getWindowsScript()
      def unixScriptFile = project.file getUnixScript()
      windowsScriptFile.text = windowsScriptFile.text.replace('%APP_HOME%\\lib\\SLASH_ETC', '%APP_HOME%\\etc')
      unixScriptFile.text = unixScriptFile.text.replace('$APP_HOME/lib/SLASH_ETC', '$APP_HOME/etc')
    }

    // get a reference to the application plugin's 'startScript' task and modify it to fix up the classpath
    def startScriptTask = project.tasks.findByPath('startScripts')
    startScriptTask.with {
      classpath = project.files(project.file("SLASH_ETC")).plus(project.files(project.file("*")))
      doLast overrideScriptFiles
    }

    // Create a task for each requested EntryPoint (start script) that will generate the start script for it.
    // This creates a task per entry point where each task creates start scripts for an individual entry point
    entryPoints.all { entry ->
      def scriptTask = project.task(type: CreateStartScripts, "create-script-${entry.name}") { t ->
        mainClassName = "this gets overridden by the 'doFirst' block"
        // the CreateStartScripts task does not support lazy eval properties, so we have to use this
        // doFirst block to set these properties to their runtime values
        doFirst {
          t.mainClassName = entryPoints[entry.name].mainClass
          defaultJvmOpts = entryPoints[entry.name].jvmArgs
        }
        applicationName = entry.name
        classpath = project.files(project.file("SLASH_ETC")).plus(project.files(project.file("*")))
        outputDir = startScriptTask.outputDir
        doLast overrideScriptFiles
      }
      startScriptTask.dependsOn(scriptTask)
    }

    // get a reference to the 'distTar' task which was created by the application plugin. Then modify
    // this task to configure the settings needed to deploy this as a container
    def distTarTask = project.tasks.findByPath('distTar')
    distTarTask.with {
      compression = Compression.GZIP
      archiveFileName = "${project.applicationName}.tgz"
    }

    // get a reference to the ContainerMaker plugin's stageContainer task and add the application TAR
    // file to the image build staging area
    project.tasks.findByPath('stageContainer').with {
      from distTarTask
    }

    // Set the default command for the container to the default application class.
    // "project.container" references the "container" extension added to the project by
    // the ContainerMaker plugin
    project.container.defaultCommand.set(["${project.applicationName}/bin/${project.applicationName}"])

    // get a reference to the ContainerMaker plugin's buildDockerfile task and add the Dockerfile
    // instruction to unpack the distribution TAR file into the image. Also set the PATH environment
    // variable to include the application 'bin' directory
    project.tasks.findByPath('buildDockerfile').with {
      addFile "${project.applicationName}.tgz", '/'
      environmentVariable "PATH", '${PATH}:' + "/${project.applicationName}/bin"
    }
  }
}
