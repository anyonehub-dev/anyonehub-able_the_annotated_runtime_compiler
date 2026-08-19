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

        val downloadCmake = project.tasks.register("downloadCmake", DownloadCmakeTask::class.java, object : Action<DownloadCmakeTask> {
            override fun execute(task: DownloadCmakeTask) {
                task.group = "abl-native"
                task.description = "Downloads CMake for arm64"
                task.outputTarGz.set(java.io.File(project.layout.buildDirectory.get().asFile, "tmp/native_tools/cmake.tar.gz"))
            }
        })

        val downloadAapt2 = project.tasks.register("downloadAapt2", DownloadAapt2Task::class.java, object : Action<DownloadAapt2Task> {
            override fun execute(task: DownloadAapt2Task) {
                task.group = "abl-native"
                task.description = "Downloads AAPT2 for arm64"
                task.outputJar.set(java.io.File(project.layout.buildDirectory.get().asFile, "tmp/native_tools/aapt2.jar"))
            }
        })

        val bundleNativeTools = project.tasks.register("bundleNativeTools", BundleNativeToolsTask::class.java, object : Action<BundleNativeToolsTask> {
            override fun execute(task: BundleNativeToolsTask) {
                task.group = "abl-native"
                task.description = "Extracts and bundles CMake and AAPT2"
                task.cmakeTarGz.set(downloadCmake.flatMap { it.outputTarGz })
                task.aapt2Jar.set(downloadAapt2.flatMap { it.outputJar })
                task.cmakeBinaryOut.set(java.io.File(project.layout.buildDirectory.get().asFile, "abl-native-tools/cmake"))
                task.aapt2BinaryOut.set(java.io.File(project.layout.buildDirectory.get().asFile, "abl-native-tools/libaapt2.so"))
            }
        })

        val bundleNativeAssets = project.tasks.register("bundleNativeAssets", BundleNativeAssetsTask::class.java, object : Action<BundleNativeAssetsTask> {
            override fun execute(task: BundleNativeAssetsTask) {
                task.group = "abl-native"
                task.description = "Bundles other native assets"
            }
        })

        project.tasks.register("compileAbl", CompileAblModulesTask::class.java, object : Action<CompileAblModulesTask> {
            override fun execute(task: CompileAblModulesTask) {
                task.group = "abl"
                task.description = "Compiles, dexes, and packages Kotlin ABL modules into an APK."
                
                val defaultAapt2 = bundleNativeTools.flatMap { it.aapt2BinaryOut }.map { it.asFile.absolutePath }
                task.aapt2BinaryPath.set(extension.aapt2BinaryPath.orElse(defaultAapt2))
                
                task.androidJarPath.set(extension.androidJarPath)
                task.manifestFile.set(extension.manifestFile)
                task.resDir.set(extension.resDir)
                
                task.keystoreFile.set(extension.keystoreFile)
                task.keystorePass.set(extension.keystorePass)
                task.keyAlias.set(extension.keyAlias)
                task.keyPass.set(extension.keyPass)
                
                task.ablDependencies.from(ablConfiguration)
                task.dependsOn(bundleNativeTools, bundleNativeAssets)
                
                task.sourceFiles.from(project.provider {
                    val sourceSets = project.extensions.findByName("sourceSets") as? SourceSetContainer
                    val mainSourceSet = sourceSets?.findByName("main")
                    mainSourceSet?.allSource?.filter { it.extension == "kt" || it.extension == "java" } ?: emptyList<java.io.File>()
                })
            }
        })
    }
}
