package com.mineraltree

import java.nio.file.Files
import java.nio.file.Paths

class ReleaseConfigLocator {

    public static final String CFG_FILE_NAME = "mt-release.conf"

    final URL configLocation
    final boolean usingUserConfig

    ReleaseConfigLocator() {
        def locations = [System.getenv("MT_HOME"), ".", Paths.get(System.getenv("HOME"), ".config", "mineraltree").toString()]

        // Strip out non-existent options
        def fileLocation = locations.findAll { i -> i != null }
        // Convert them to 'Path' objects
                .collect { d -> Paths.get(d, CFG_FILE_NAME) }
        // Find the first one that exists
                .find { f -> Files.exists(f) }

        if (!fileLocation) {
            usingUserConfig = false
            configLocation = getClass().getResource("/release-template.conf")
        }
        else {
            usingUserConfig = true
            configLocation = fileLocation.toUri().toURL()
        }
    }
}
