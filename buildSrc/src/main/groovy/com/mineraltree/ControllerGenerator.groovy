package com.mineraltree

/**
 * An abstract class for tasks that generate Kubernetes descriptor files Kubernetes controller resources
 */
abstract class ControllerGenerator extends BaseGenerator {

  /**
   * The full path location of the input file
   */
  def inputDefinition

  /**
   * Information about the Kubernetes object to create
   */
  def inputInfo

  @Override
  String getOutputFileName() {
    return "${moduleName}.json"
  }

  @Override
  def generate() {
    super.generate()
    inputInfo = loadContainerInfo()
  }

  def loadContainerInfo() {
    // load the information from the input file (.conf)
    def inputFile = project.file(inputDefinition)
    def slurper = new ConfigSlurper()
    slurper.setBinding(["app"    : release.getAppBindings(),
                        "release": release])
    return slurper.parse(inputFile.toURI().toURL())
  }

  /**
   * Builds the environment variables requested by the deployment. This will optionally add an
   * {@code env} section to the pod definition of this deployment.
   */
  private def getPodEnvironment() {
    def env = []
    if (inputInfo.isSet('env')) {
      env += inputInfo.env.collect { key, entry -> [name: key, value: entry] }
    }
    return env
  }

  /**
   * Builds a map of key-value pairs that defines the container deployment
   */
  def generateContainer() {
    def container = [
        name           : getModuleFullName(),
        image          : getImageName(inputInfo.image),
        imagePullPolicy: getServiceSetting("imagePullPolicy"),
        env            : getPodEnvironment(),
        volumeMounts   : generateVolumeMounts()
    ]
    // If a command is explicitly given, add it to the container definition
    if (inputInfo.command) {
      container['command'] = inputInfo.command
    }
    if (inputInfo.args) {
      container['args'] = inputInfo.args
    }
    return container
  }

  /**
   * Generates the descriptor for an "ExternalName" service. This type of
   * service is a wrapper for an external service. For example, an
   * ExternalService can be configured to reference an external actual DB
   * server so that other containers don't have to change anything when
   * the DB server is swapped out for a replica.
   */
  def generateExternalName() {

    def json = createServiceKind("v1", "Service")
    json['spec'] = [
        type        : "ExternalName",
        externalName: inputInfo.linkTo
    ]
    return json
  }

  /**
   * Generates the service half of the microservice definition. This service is
   * paired with a Deployment to build a single microservice. The service is the
   * networking half of the definition.
   * @return
   */
  def generateService() {

    def specMap = [:]
    def portList = []

    // Create entries for each port defined in the input file
    inputInfo.ports.each { key, portInfo ->
      // each defined port has a name and a target port
      def portMapEntry = [name: key, targetPort: portInfo.internal]

      // to keep things simple, we require the service's port to match the pod's port
      // ...unless this is a public service then we use the port they wish to publicly expose
      if (portInfo.isSet('exposed')) {
        portMapEntry['port'] = portInfo.exposed
        // Create a load balancer to make this port public
        specMap['type'] = "LoadBalancer"
      } else {
        portMapEntry['port'] = portInfo.internal
      }
      // Add this port entry to the list of ports
      portList << portMapEntry
    }
    specMap['ports'] = portList
    specMap['selector'] = [svc: getModuleFullName()]

    def serviceDocument = createServiceKind("v1", "Service")
    serviceDocument['spec'] = specMap

    return serviceDocument
  }

  /**
   * Builds a list of volume definitions, where each definition is a list of key-value pairs
   */
  def generateVolumes() {
    def volumes = [
        [
            name     : "config-volume",
            configMap: [
                name: getModuleFullName("config")
            ]
        ],
        [
            name  : "secret-volume",
            secret: [
                secretName: getModuleFullName("secrets")
            ]
        ]
    ]
    return volumes
  }

  /**
   * Builds a list of volume mount definitions, where each definition is a list of key-value pairs
   */
  def static generateVolumeMounts() {
    def volumeMounts = [
        [
            name     : "config-volume",
            mountPath: "/etc/mineraltree/app",
            readOnly : true
        ],
        [
            name     : "secret-volume",
            mountPath: "/etc/mineraltree/.config",
            readOnly : true
        ]
    ]
    return volumeMounts
  }
}
