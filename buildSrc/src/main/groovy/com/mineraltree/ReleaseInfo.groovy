package com.mineraltree

import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Information configured about a release. This is the user-customized release information that is applied
 * to the deployment to tailor it to the individual environment.
 * <p>
 * The configuration file named {@code mt-release.conf} and is searched for in the following order (first
 * location found wins):
 * <ol>
 *     <li>The directory specified in the {@code MT_HOME} environment variable</li>
 *     <li>The current directory</li>
 *     <li>{@code $HOME/.config/mineraltree/}</li>
 * </ol>
 *
 * The configuration file itself uses the Groovy ConfigSlurper syntax and will allow for different environments
 * using the {@code deployEnv} project property. For example:
 * <pre>
 *     gradle -PdeployEnv=local deploy...
 * </pre>
 */
class ReleaseInfo {

  private static final String ENVIRONMENT_PROPERTY = "deployEnv"
  private static final TemplateEngine TEMPLATES = new SimpleTemplateEngine();
  private static final Logger logger = Logging.getLogger(ReleaseInfo.class);

  private ConfigObject configInfo
  private Project project
  private final ReleaseConfigLocator configLocator = new ReleaseConfigLocator()

  ReleaseInfo(Project project) {

    this.project = project

    def fileLocation = configLocator.configLocation
    ConfigSlurper cfg = new ConfigSlurper(project.properties[ENVIRONMENT_PROPERTY]?.toString())
    configInfo = cfg.parse(fileLocation)
    logger.info("Parsed ${fileLocation} as: ${configInfo}")
  }

  void requireUserConfig() {
    if ( ! configLocator.usingUserConfig) {
      throw new GradleException("Deploy tasks cannot be run when a '${ReleaseConfigLocator.CFG_FILE_NAME}' configuration file is not supplied. " +
              "You must provide this file which contains information about the deployment target environment. " +
              "Use the '${project.name}:sampleConfig' task to generate a sample file. ")
    }
  }

ConfigObject getAppBindings() {
    return configInfo.app
  }

  String getNamespace(String namespaceGroup) {
    if (configInfo.isSet('troop')) {
      logger.warn("Using deprecated configuration setting 'troop'. Use 'namespace' instead.")
      return configInfo.troop
    }
    if (namespaceGroup && configInfo.get(namespaceGroup).get("namespace")) {
      return configInfo.get(namespaceGroup).get("namespace")
    }
    return configInfo.namespace
  }

  String getServiceSetting(String service, String key) {

    if (configInfo.get(service)?.isSet(key)) {
      String r = configInfo.get(service).get(key)
      return r
    }
    return configInfo.get(key)
  }

  String resolveString(String service, String template) {
    Map properties = configInfo.flatten()
    if (service && configInfo.isSet(service)) {
      Map overlay = configInfo.get(service).flatten()
      properties.putAll(overlay)
    }
    TEMPLATES.createTemplate(template).make(properties);
  }

  Map getProperties(String prefix) {
    logger.info("Getting propreties from section ${prefix} from ${configInfo}")
    return configInfo.get(prefix).flatten()
  }
}
