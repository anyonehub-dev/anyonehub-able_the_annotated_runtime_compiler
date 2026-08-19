package com.anyonehub.abl.plugin

import com.anyonehub.abl.compiler.AblCompilerBridgeImpl
import com.anyonehub.abl.compiler.ApkBuildPipeline
import com.anyonehub.abl.compiler.ExecutionPipeline
import com.anyonehub.abl.compiler.RuntimeDexProcessor
import com.anyonehub.abl.packaging.ApkPackager
import com.anyonehub.abl.packaging.ZipAligner
import com.anyonehub.abl.resources.NativeAapt2Wrapper
import com.anyonehub.abl.scanner.AblRuntimeScanner
import com.anyonehub.abl.scanner.DependencyGraphBuilder
import com.anyonehub.abl.signing.ApkSignerWrapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CompileAblModulesTask : DefaultTask() {

    @get:Input
    abstract val aapt2BinaryPath: Property<String>

    @get:Input
    abstract val androidJarPath: Property<String>

    @get:InputFiles
    abstract val manifestFile: RegularFileProperty

    @get:InputFiles
    abstract val resDir: DirectoryProperty

    @get:InputFiles
    @get:Optional
    abstract val keystoreFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val keystorePass: Property<String>

    @get:Input
    @get:Optional
    abstract val keyAlias: Property<String>

    @get:Input
    @get:Optional
    abstract val keyPass: Property<String>

    @get:InputFiles
    abstract val sourceFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val ablDependencies: ConfigurableFileCollection

    @TaskAction
    fun compile() {
        val sourcesMap = mutableMapOf<String, String>()
        for (file in sourceFiles.files) {
            val content = file.readText()
            val packageName = extractPackageName(content)
            val className = file.nameWithoutExtension
            val fqName = if (packageName.isNotEmpty()) "$packageName.$className" else className
            sourcesMap[fqName] = content
        }

        val scanner = AblRuntimeScanner()
        val graphBuilder = DependencyGraphBuilder()
        val compilerBridge = AblCompilerBridgeImpl()
        val dexProcessor = RuntimeDexProcessor()
        
        val executionPipeline = ExecutionPipeline(scanner, graphBuilder, compilerBridge, dexProcessor)
        
        val aapt2Wrapper = NativeAapt2Wrapper(aapt2BinaryPath.get())
        val packager = ApkPackager()
        val zipAligner = ZipAligner()
        val signer = ApkSignerWrapper()
        
        val pipeline = ApkBuildPipeline(executionPipeline, aapt2Wrapper, packager, zipAligner, signer)
        
        val outputApk = File(project.layout.buildDirectory.get().asFile, "outputs/apk/abl/app-release.apk")
        outputApk.parentFile.mkdirs()
        val androidJar = File(androidJarPath.get())
        
        pipeline.buildApk(
            manifestFile = manifestFile.get().asFile,
            resDir = resDir.get().asFile,
            androidJar = androidJar,
            keystoreFile = keystoreFile.orNull?.asFile,
            keystorePass = keystorePass.orNull,
            keyAlias = keyAlias.orNull,
            keyPass = keyPass.orNull,
            kotlinSources = sourcesMap,
            classpath = ablDependencies.files.toList(),
            outputApk = outputApk
        )
        
        println("ABL Engine successfully bundled APK at: ${outputApk.absolutePath}")
    }
    
    private fun extractPackageName(content: String): String {
        val match = Regex("^\\s*package\\s+([a-zA-Z0-9_.]+)", RegexOption.MULTILINE).find(content)
        return match?.groupValues?.get(1) ?: ""
    }
}
