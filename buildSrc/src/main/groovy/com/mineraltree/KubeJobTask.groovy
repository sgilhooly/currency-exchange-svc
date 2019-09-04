package com.mineraltree

import org.gradle.api.tasks.InputFile

class KubeJobTask extends CliTask {

    @InputFile
    def jobConfiguration

    @Override
    def taskApply() {
        if (jobConfiguration.hasProperty('name')) {
            logger.info("Starting job ${jobConfiguration.name}")
        } else {
            logger.info("Starting job from file ${jobConfiguration}")
        }
        kubectl("apply", false, ["-f", project.file(jobConfiguration).toString()])
    }
}
