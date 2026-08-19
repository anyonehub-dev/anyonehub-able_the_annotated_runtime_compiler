package com.anyonehub.abl.plugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSetContainer

class AblCompilerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("abl", AblExtension::class.java)
        
        val ablConfiguration = project.configurations.create("abl", object : Action<Configuration> {
            override fun execute(config: Configuration) {
                config.isTransitive = true
                config.isCanBeResolved = true
                config.isCanBeConsumed = false
            }
        })

        project.tasks.register("compileAbl", CompileAblModulesTask::class.java, object : Action<CompileAblModulesTask> {
            override fun execute(task: CompileAblModulesTask) {
                task.group = "abl"
                task.description = "Compiles, dexes, and packages Kotlin ABL modules into an APK."
                
                task.aapt2BinaryPath.set(extension.aapt2BinaryPath)
                task.androidJarPath.set(extension.androidJarPath)
                task.manifestFile.set(extension.manifestFile)
                task.resDir.set(extension.resDir)
                
                task.keystoreFile.set(extension.keystoreFile)
                task.keystorePass.set(extension.keystorePass)
                task.keyAlias.set(extension.keyAlias)
                task.keyPass.set(extension.keyPass)
                
                task.ablDependencies.from(ablConfiguration)
                
                project.afterEvaluate {
                    val sourceSets = project.extensions.findByName("sourceSets") as? SourceSetContainer
                    val mainSourceSet = sourceSets?.findByName("main")
                    if (mainSourceSet != null) {
                        task.sourceFiles.from(mainSourceSet.allSource.filter { it.extension == "kt" })
                    }
                }
            }
        })
    }
}
