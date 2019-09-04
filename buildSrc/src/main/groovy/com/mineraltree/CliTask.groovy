package com.mineraltree

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

abstract class CliTask extends DefaultTask {

    /**
     * The build configuration settings.
     */
    protected ReleaseInfo release

    /**
     * The target directory of the generated output files
     */
    def outputDestination

    def namespaceGroup = ""

    CliTask() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    def taskMain() {
        release = new ReleaseInfo(project)
        release.requireUserConfig()
        taskApply()
    }

    abstract def taskApply()

    /**
     * Execute the given kubectl command in order to interact with the kubernetes cluster. The directory in which
     * the command is executed is the directory where the relevant descriptor for this object is located.
     *
     * @param exec
     *      the kubectl command to execute
     * @param ignoreExitFlag
     *      the flag to determine whether to ignore the exit value of the kubectl command; default is false
     * @param options
     *      a list of additional command line options for the given kubectl command
     * @return
     */
    protected def kubectl(String exec, Boolean ignoreExitFlag = false, List<String> options = []) {
        def command = ["kubectl"]
        command.add(exec)
        command.addAll(options)
        project.exec {
            workingDir "${outputDestination}/${release.getNamespace(namespaceGroup)}"
            commandLine command
            ignoreExitValue ignoreExitFlag
        }
    }
}
