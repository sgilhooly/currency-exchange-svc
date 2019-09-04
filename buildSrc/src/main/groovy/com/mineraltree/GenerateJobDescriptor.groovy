package com.mineraltree

import org.gradle.api.InvalidUserDataException

/**
 * Generates a Kubernetes deployment descriptor from a job configuration file. A job is an object in Kubernetes
 * that starts up, performs a set of actions, and shuts itself down. This task can generate the kubernetes deployment
 * information using the information from the configuration file, applying some conventions, and constructing
 * it all together with the boilerplate templates required.
 */
class GenerateJobDescriptor extends ControllerGenerator {

  def args
  def image
  def env

  @Override
  def loadContainerInfo() {
    ConfigObject containerInfo
    if (inputDefinition) {
      containerInfo = super.loadContainerInfo()
    } else {
      if (!image) {
        throw new InvalidUserDataException("Job generation task must specify 'inputDefinition' or 'image'")
      }
      containerInfo = new ConfigObject()
    }
    if (args) {
      containerInfo.put("args", args)
    }
    if (image) {
      containerInfo.put("image", image)
    }
    if (env) {
      containerInfo.put("env", env)
    }
    return containerInfo
  }

  @Override
  def generate() {
    super.generate()
    def document = createServiceKind("batch/v1", "Job")
    def container = generateContainer()

    def spec = [
        template : [
            spec : [
                containers : [container],
                restartPolicy : "Never",
                volumes : generateVolumes()
            ]
        ],
        backoffLimit: 0
    ]
    document.spec = spec
    renderJsonOutput(document)
  }
}
