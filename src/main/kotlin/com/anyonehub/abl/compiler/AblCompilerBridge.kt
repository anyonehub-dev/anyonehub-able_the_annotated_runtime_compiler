package com.anyonehub.abl.compiler

import java.io.File

interface AblCompilerBridge {
    /**
     * Compiles Kotlin source files into JVM bytecode (.class files) using kotlin-compiler-embeddable.
     * Uses a fast temporary directory approach to satisfy Kotlin's file system constraints,
     * immediately reading results into memory and deleting the workspace.
     *
     * @param sources A map of fully qualified class names to raw Kotlin source code strings.
     * @param classpath External dependencies required for compilation.
     * @return A map of compiled class names to their JVM bytecode (ByteArray).
     */
    fun compile(sources: Map<String, String>, classpath: List<File>): Map<String, ByteArray>
}
