package com.anyonehub.abl.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.net.URL

abstract class DownloadCmakeTask : DefaultTask() {
    @get:Input
    var cmakeVersion: String = "4.1.2"
    
    @get:OutputFile
    abstract val outputTarGz: RegularFileProperty
    
    @TaskAction
    fun download() {
        val url = "https://github.com/Kitware/CMake/releases/download/v$cmakeVersion/cmake-$cmakeVersion-linux-aarch64.tar.gz"
        val outputFile = outputTarGz.get().asFile
        if (!outputFile.exists()) {
            outputFile.parentFile.mkdirs()
            println("Downloading CMake $cmakeVersion from $url ...")
            URL(url).openStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

abstract class DownloadAapt2Task : DefaultTask() {
    @get:Input
    var aapt2Version: String = "8.5.0-11315950"
    
    @get:OutputFile
    abstract val outputJar: RegularFileProperty
    
    @TaskAction
    fun download() {
        val url = "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/$aapt2Version/aapt2-$aapt2Version-linux.jar"
        val outputFile = outputJar.get().asFile
        if (!outputFile.exists()) {
            outputFile.parentFile.mkdirs()
            println("Downloading AAPT2 $aapt2Version from $url ...")
            URL(url).openStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

abstract class BundleNativeToolsTask : DefaultTask() {
    
    @get:InputFile
    abstract val cmakeTarGz: RegularFileProperty
    
    @get:InputFile
    abstract val aapt2Jar: RegularFileProperty
    
    @get:OutputFile
    abstract val cmakeBinaryOut: RegularFileProperty
    
    @get:OutputFile
    abstract val aapt2BinaryOut: RegularFileProperty
    
    @TaskAction
    fun bundle() {
        val cmakeOut = cmakeBinaryOut.get().asFile
        val aapt2Out = aapt2BinaryOut.get().asFile
        val tempDir = File(project.layout.buildDirectory.get().asFile, "tmp/native_tools_extract")
        
        if (!cmakeOut.exists()) {
            println("Extracting CMake to ${cmakeOut.absolutePath} ...")
            project.copy(object : org.gradle.api.Action<org.gradle.api.file.CopySpec> {
                override fun execute(spec: org.gradle.api.file.CopySpec) {
                    spec.from(project.tarTree(project.resources.gzip(cmakeTarGz.get().asFile)))
                    spec.into(tempDir)
                }
            })
            val cmakeExec = tempDir.walkTopDown().find { it.name == "cmake" && it.isFile }
                ?: throw IllegalStateException("cmake executable not found in tarball")
            cmakeOut.parentFile.mkdirs()
            cmakeExec.copyTo(cmakeOut, overwrite = true)
            cmakeOut.setExecutable(true)
        }
        
        if (!aapt2Out.exists()) {
            println("Extracting AAPT2 to ${aapt2Out.absolutePath} ...")
            project.copy(object : org.gradle.api.Action<org.gradle.api.file.CopySpec> {
                override fun execute(spec: org.gradle.api.file.CopySpec) {
                    spec.from(project.zipTree(aapt2Jar.get().asFile))
                    spec.into(tempDir)
                }
            })
            val aapt2Exec = tempDir.walkTopDown().find { it.name == "aapt2" && it.isFile }
                ?: throw IllegalStateException("aapt2 executable not found in jar")
            aapt2Out.parentFile.mkdirs()
            aapt2Exec.copyTo(aapt2Out, overwrite = true)
            aapt2Out.setExecutable(true)
        }
    }
}

abstract class BundleNativeAssetsTask : DefaultTask() {
    @TaskAction
    fun bundleAssets() {
        println("Bundling native assets...")
    }
}
