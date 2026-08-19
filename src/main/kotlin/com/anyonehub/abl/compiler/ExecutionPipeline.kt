package com.anyonehub.abl.compiler

import com.anyonehub.abl.scanner.AblRuntimeScanner
import com.anyonehub.abl.scanner.DependencyGraphBuilder
import java.io.File
import kotlin.reflect.KClass

class ExecutionPipeline(
    private val scanner: AblRuntimeScanner,
    private val graphBuilder: DependencyGraphBuilder,
    private val compilerBridge: AblCompilerBridge,
    private val dexProcessor: RuntimeDexProcessor
) {
    /**
     * Master orchestrator function.
     * Scans classes, builds the dependency graph, compiles the requested sources into JVM bytecode,
     * and transpiles the bytecode into Dalvik DEX entirely in-memory.
     * 
     * @param targetClasses Classes to scan for configuration and entry points.
     * @param sourceFiles A map of fully qualified class names to source code for compilation.
     * @param classpath List of dependencies for the compiler bridge.
     * @return A byte array of the resulting classes.dex.
     */
    fun compileAndRun(targetClasses: List<KClass<*>>, sourceFiles: Map<String, String>, classpath: List<File> = emptyList()): ByteArray {
        // Step 1: Reflective Scanning (Identify Modules, Injectables, Entry Points)
        val scannedMetadata = scanner.scanMultiple(targetClasses)
        
        // Step 2: Assemble Dependency Graph
        val executionMap = graphBuilder.buildGraph(scannedMetadata)
        
        println("Execution Map generated with ${executionMap.modules.size} modules and ${executionMap.entryPoints.size} entry points.")
        
        // Step 3: Embeddable JVM Compilation (Outputs .class Bytes natively into memory)
        val jvmBytecodeMap = compilerBridge.compile(sourceFiles, classpath)
        println("Successfully compiled ${jvmBytecodeMap.size} classes to JVM bytecode.")
        
        // Step 4: D8 / R8 DEX Transpilation (In-Memory Dalvik conversion)
        val dexBytes = dexProcessor.transpileToDex(jvmBytecodeMap)
        println("Transpiled to Dalvik bytecode. DEX size: ${dexBytes.size} bytes.")
        
        return dexBytes
    }
}
