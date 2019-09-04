package com.mineraltree

import org.gradle.api.tasks.InputFile

/**
 * Task to generate a configuration map. A kubernetes ConfigMap is a type of service which
 * provides pods with configuration information. This will generate either a configuration
 * map or a secret map which will contain the requested configuration settings (as key/value
 * pairs).
 */
class GenerateConfigMap extends BaseGenerator {

  /**
   * The name of the field in the ConfigMap to store this configuration under. This is
   * typically a filename-looking thing (like "myconfig.properties") which will be
   * mapped to a file with this name in the container. For example, code in the container
   * would reference this configuration information using (perhaps) {@code /etc/app/myconfig.properties}.
   */
  def propertyName
  /**
   * If {@code true} will generate a Secret instead of a ConfigMap. Secrets can be
   * administered separately allowing for additional security measures which deter
   * users from viewing the secret contents.
   */
  def secret = false
  /**
   * The name of the Kubernetes service to create
   */
  def outputName
  /**
   * The name of the property to pull the key/values FROM. This should match a "section"
   * of the build configuration settings which will be extracted and placed into the
   * config-map.
   */
  def configSection

  GenerateConfigMap() {
    if (project.hasProperty('k8deploy.genSettings') && !project.properties['k8deploy.genSettings'].toBoolean()) {
      logger.debug("Skipping ${project.name}:${name} since genSettings is false")
      enabled = false
    }
  }

  @Override
  String getOutputFileName() {
    return outputName
  }


  /**
   * Performs the generation
   */
  @Override
  def generate() {
    super.generate()
    // Create the json document representing this configmap
    def configDoc = createServiceKind("v1", secret ? "Secret" : "ConfigMap")
    String propContent = getProperties(configSection)
    if (secret) {
      // Secrets are base64-encoded and have an additional 'type' field
      propContent = propContent.bytes.encodeBase64().toString()
      configDoc['type'] = "Opaque"
    }
    configDoc.data = [(propertyName.toString()): propContent]
    renderJsonOutput(configDoc)
  }

  /**
   * Reads all the properties from a given section of the build configuration file
   * @param section
   *      The section of the configuration to read the settings from
   */
  private String getProperties(def section) {
    Map properties = release.getProperties(section)
    return properties.collect { key, value ->
        "${key}=\"${value}\""
    }.join("\n").plus("\n")
  }
}
