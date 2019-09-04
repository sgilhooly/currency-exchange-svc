package com.mineraltree

/**
 * Generates a Kubernetes deployment descriptor from a service configuration file. Service
 * configuration files define basic attributes about a service in the MineralTree world.
 * Kubernetes, however, needs a set of additional information about each service which is
 * mostly boilerplate. This task can generate the kubernetes deployment information using
 * the information from the configuration file, applying some conventions, and constructing
 * it all together with the boilerplate templates required.
 */
class GenerateServiceDescriptor extends ControllerGenerator {

  @Override
  def generate() {
    super.generate()
    def jsonDoc
    if (inputInfo.linkTo) {
      jsonDoc = generateExternalName()
    } else {
      jsonDoc = generateMicroService()
    }
    renderJsonOutput(jsonDoc)
  }

  /**
   * Generates the descriptor for a standard microservice. This involves creating both a
   * service and a deployment which represent the service as a whole in the kubernetes
   * environment.
   */
  private def generateMicroService() {
    logger.debug "Generating microservice {}", getModuleFullName()
    // The 'List' kind allows you to specify multiple items in a single JSON tree
    def document = createServiceKind("v1", "List")

    document.items = [generateService(), generateDeployment()]
    return document
  }

  /**
   * Generates the Deployment half of a microservice. The deployment is paired with a
   * service to build a single microservice. The Deployment is the compute half of the
   * service definition.
   */
  private def generateDeployment() {
    def document = createServiceKind("apps/v1", "Deployment")
    def container = generateContainer()
    container.ports = inputInfo.ports.collect { key, info -> [name: key, containerPort: info.internal] }

    def spec = [
        replicas: 1,
        selector: [
            matchLabels: [
                svc: getModuleFullName()
            ]
        ],
        template: [
            metadata: [
                labels: [
                    svc: getModuleFullName()
                ]
            ],
            spec    : [
                containers: [container],
                volumes   : generateVolumes()
            ]
        ]
    ]
    document.spec = spec
    return document
  }
}
