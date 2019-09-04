package com.mineraltree

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Base for tasks which generate a kubernetes deployment descriptor file
 */
abstract class BaseGenerator extends DefaultTask {

  /**
   * The base name of the service module.
   */
  def moduleName

  /**
   * The build configuration settings.
   */
  protected ReleaseInfo release

  /**
   * Whether or not the output JSON file should be pretty-printed
   */
  def pretty = false

  /**
   * The target directory of the generated output files
   */
  def outputDestination

  def namespaceGroup = ""

  def staticImage = ""
  def staticVersion = ""

  @OutputFile
  File getOutputFile() {
    initReleaseInfo()
    return new File("${outputDestination}/${release.getNamespace(namespaceGroup)}", getOutputFileName())
  }

  abstract String getOutputFileName()

  BaseGenerator() {
    outputs.upToDateWhen { false }
  }

  @TaskAction
  def generate() {
    initReleaseInfo()
    File dir = project.file("${outputDestination}/${release.getNamespace(namespaceGroup)}")
    dir.mkdirs()
  }

  private def initReleaseInfo() {
    if (! release) {
      release = new ReleaseInfo(project)
    }
  }

  /**
   * Builds and returns the fully qualified (with version info) container image.
   */
  protected String getImageName(String image) {
    def imagePrefix = release.getServiceSetting(moduleName, "imagePrefix")
    def imageVersion = release.getServiceSetting(moduleName, "version")
    def containerImage = staticImage ? staticImage : "${imagePrefix}${image}"
    def containerVersion = staticVersion ? staticVersion : imageVersion
    return "${containerImage}:${containerVersion}"
  }

  /**
   * Gets the fully decorated name of the service module. Currently this just returns the
   * module's name.
   */
  protected String getModuleFullName() {
    return getModuleFullName(moduleName)
  }

  /**
   * Gets the fully decorated name of a named service. Currently we do not decorate names so
   * this just returns the baseName as-is.
   */
  protected String getModuleFullName(String baseName) {
    return baseName
  }

  protected String getServiceSetting(key) {
    return release.getServiceSetting(moduleName, key)
  }
  /**
   * Performs basic argument substitution based on configuration settings. For example, given
   * an input string of "This is my ${version}" and assuming the configuration settings contain
   * a {@code version=xyz}, this method would return "This is my xyz"
   */
  protected String resolveString(String template) {
    return release.resolveString(moduleName, template)
  }

  /**
   * Creates the basic structure of a kubernetes deployment descriptor. This is the basic set
   * of fields required by all entries in a deployment file.
   *
   * @param schemaVersion
   *      the schema version that this {@code Kind} is defined in
   * @param serviceKind
   *      the "kind" of service/record to create (such as "Pod", "Service", etc...)
   */
  protected def createServiceKind(def schemaVersion, def serviceKind) {
    return [
        kind      : serviceKind,
        apiVersion: schemaVersion,
        metadata  : [
            name: getModuleFullName(),
            namespace: release.getNamespace(namespaceGroup)
        ]
    ]
  }

  /**
   * Given the document tree, will generate a JSON representation of it which will be optionally
   * in "pretty" format
   */
  def renderJsonOutput(document) {
    def output = project.file(getOutputFile())
      logger.warn("Writing resource to {}", output)
    def content = JsonOutput.toJson(document)
    if (pretty) {
      content = JsonOutput.prettyPrint(content)
    }
    output.text = content
  }

}
