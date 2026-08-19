package com.anyonehub.abl.compiler

import com.anyonehub.abl.exceptions.AblCompilationException
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.nio.file.Files

class AblCompilerBridgeImpl : AblCompilerBridge {

    override fun compile(sources: Map<String, String>, classpath: List<File>): Map<String, ByteArray> {
        val tempDir = Files.createTempDirectory("abl_compiler").toFile()
        val srcDir = File(tempDir, "src").apply { mkdirs() }
        val outDir = File(tempDir, "out").apply { mkdirs() }
        
        val errors = mutableListOf<String>()

        try {
            val javaFiles = mutableListOf<File>()
            val kotlinFiles = mutableListOf<File>()

            // Write sources to the temporary workspace
            sources.forEach { (className, code) ->
                val isJava = className.endsWith(".java")
                val fqName = if (isJava) className.removeSuffix(".java") else className
                val relativePath = fqName.replace('.', '/') + if (isJava) ".java" else ".kt"
                val file = File(srcDir, relativePath)
                file.parentFile?.mkdirs()
                file.writeText(code)
                if (isJava) {
                    javaFiles.add(file)
                } else {
                    kotlinFiles.add(file)
                }
            }

            if (kotlinFiles.isNotEmpty() || javaFiles.isNotEmpty()) {
                val args = K2JVMCompilerArguments().apply {
                    freeArgs = (kotlinFiles + javaFiles).map { it.absolutePath }
                    destination = outDir.absolutePath
                    if (classpath.isNotEmpty()) {
                        this.classpath = classpath.joinToString(File.pathSeparator) { it.absolutePath }
                    }
                    noStdlib = true
                    noReflect = true
                    jvmTarget = "21"
                }
    
                val collector = object : MessageCollector {
                    override fun clear() { errors.clear() }
                    override fun hasErrors(): Boolean = errors.isNotEmpty()
                    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                        val formatted = "[$severity] $message at $location"
                        if (severity.isError) {
                            errors.add(formatted)
                            System.err.println("Compiler Error: $formatted")
                        } else if (severity.isWarning) {
                            System.err.println("Compiler Warning: $formatted")
                        }
                    }
                }
    
                val compiler = K2JVMCompiler()
                val exitCode = compiler.exec(collector, Services.EMPTY, args)
    
                if (exitCode != org.jetbrains.kotlin.cli.common.ExitCode.OK || errors.isNotEmpty()) {
                    throw AblCompilationException("Kotlin Compilation failed with exit code $exitCode.\nErrors:\n${errors.joinToString("\n")}")
                }
            }

            if (javaFiles.isNotEmpty()) {
                val ecjArgs = mutableListOf<String>()
                val fullClasspath = classpath.map { it.absolutePath }.toMutableList()
                fullClasspath.add(outDir.absolutePath) // To resolve Kotlin classes
                
                if (fullClasspath.isNotEmpty()) {
                    ecjArgs.add("-cp")
                    ecjArgs.add(fullClasspath.joinToString(File.pathSeparator))
                }
                ecjArgs.add("-d")
                ecjArgs.add(outDir.absolutePath)
                ecjArgs.add("-21") // JVM Target 21
                ecjArgs.addAll(javaFiles.map { it.absolutePath })
                
                val outWriter = java.io.PrintWriter(System.out)
                val errWriter = java.io.PrintWriter(System.err)
                val ecjMain = org.eclipse.jdt.internal.compiler.batch.Main(outWriter, errWriter, false, null, null)
                val success = ecjMain.compile(ecjArgs.toTypedArray())
                if (!success) {
                    throw AblCompilationException("ECJ Compilation failed for Java sources.")
                }
            }

            // Harvest the compiled bytecode directly into memory
            val resultMap = mutableMapOf<String, ByteArray>()
            outDir.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { classFile ->
                val relativePath = classFile.relativeTo(outDir).path
                val fqName = relativePath.removeSuffix(".class").replace(File.separatorChar, '.')
                resultMap[fqName] = classFile.readBytes()
            }
            return resultMap
        } finally {
            // ZERO FOOTPRINT POLICY: guarantee cleanup to prevent disk bloat
            tempDir.deleteRecursively()
        }
    }
}
