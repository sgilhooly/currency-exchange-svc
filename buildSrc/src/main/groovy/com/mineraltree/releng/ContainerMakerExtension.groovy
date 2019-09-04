package com.mineraltree.releng


import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * Extension class to configure the ContainerMaker plugin. Contains all the configuration settings
 * exposed for the ContainerMaker plugin.
 */
class ContainerMakerExtension {

  /**
   * The image to base the container from. A 'Property' type is used here to allow this value to
   * be evaluated lazily
   */
  Property<String> baseImage

  /**
   * The command to launch the container with
   */
  ListProperty<String> defaultCommand

  @Inject
  ContainerMakerExtension(ObjectFactory factory) {
    baseImage = factory.property(String.class)
    defaultCommand = factory.listProperty(String.class)
  }
}
