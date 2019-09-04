package com.mineraltree

import org.gradle.api.Project
import org.gradle.api.Plugin

class Persist implements Plugin<Project> {

    /**
     * Sets up defaults, declares tasks and executes.
     * @param project - The gradle project to which this plugin was applied
     */
    void apply(Project project) {

        String defaultDbPath = "${project.projectDir}/src/main/resources/com/mineraltree/" + project.name + "/db.changelog-main.groovy"
        String defaultGeneratedSrcDir ="${project.buildDir}/generated/src/main/"
        String defaultPackageName = "com.mineraltree.auth." + project.name

        project.sourceSets.main.java.srcDirs += defaultGeneratedSrcDir

        if (project.idea) {
            project.idea.module.generatedSourceDirs += new File(defaultGeneratedSrcDir)
        }


        /**
         * Defines a task that will generate Jooq code from the database changelogs
         */
        def generateSchemaDefault = project.task(type: GenerateJooqSchema, 'generateSchema') {
            group 'build'
            description 'Pulls from locally stored changelogs to generate jooq code to allow for database management'

            dbPath = defaultDbPath
            generatedSrcDir = defaultGeneratedSrcDir
            packageName = defaultPackageName
        }

        /**
         * Ensures that the schema is generated before the rest of the gradle build
         */
        project.getTasks().findByName('compileJava').dependsOn(generateSchemaDefault)
    }
}