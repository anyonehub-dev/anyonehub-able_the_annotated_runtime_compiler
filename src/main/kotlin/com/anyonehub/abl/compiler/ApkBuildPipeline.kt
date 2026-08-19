package com.anyonehub.abl.compiler

import com.anyonehub.abl.packaging.ApkPackager
import com.anyonehub.abl.packaging.ZipAligner
import com.anyonehub.abl.resources.NativeAapt2Wrapper
import com.anyonehub.abl.signing.ApkSignerWrapper
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

class ApkBuildPipeline(
    private val executionPipeline: ExecutionPipeline,
    private val aapt2Wrapper: NativeAapt2Wrapper,
    private val packager: ApkPackager,
    private val zipAligner: ZipAligner,
    private val signer: ApkSignerWrapper
) {
    /**
     * Master Orchestrator for full APK build on-device utilizing the Hybrid Architecture with Zipalignment.
     */
    fun buildApk(
        manifestFile: File,
        resDir: File,
        androidJar: File,
        keystoreFile: File?,
        keystorePass: String?,
        keyAlias: String?,
        keyPass: String?,
        kotlinSources: Map<String, String>,
        classpath: List<File> = emptyList(),
        outputApk: File
    ) {
        val workspace = File(outputApk.parentFile, "build_workspace").apply { mkdirs() }
        
        try {
            // Step 1: AAPT2 Native Execution (libaapt2.so via W^X bypass)
            println("Compiling and linking resources natively...")
            val compiledResDir = File(workspace, "compiled_res").apply { mkdirs() }
            aapt2Wrapper.compile(resDir, compiledResDir)
            
            val generatedJavaDir = File(workspace, "generated_java").apply { mkdirs() }
            val linkedApk = File(workspace, "linked.apk")
            aapt2Wrapper.link(manifestFile, compiledResDir, androidJar, linkedApk, generatedJavaDir)
            
            val combinedSources = kotlinSources.toMutableMap()
            generatedJavaDir.walkTopDown().filter { it.extension == "java" }.forEach { 
                val relativePath = it.relativeTo(generatedJavaDir).path
                val fqNameWithExt = relativePath.replace(File.separatorChar, '.')
                combinedSources[fqNameWithExt] = it.readText()
            }
            
            // Step 2 & 3: Kotlin Compilation & In-Memory D8 DEXing
            println("Compiling Kotlin sources and transpiling to DEX in-memory...")
            val fullClasspath = mutableListOf(androidJar)
            fullClasspath.addAll(classpath)
            val dexBytes = executionPipeline.compileAndRun(emptyList(), combinedSources, fullClasspath)
            
            // Step 4: Java ZipOutputStream Packaging (Unaligned)
            println("Packaging Unaligned APK...")
            val unalignedApk = File(workspace, "unaligned.apk")
            packager.packageApk(dexBytes, linkedApk, unalignedApk)
            
            // Step 5: Zipalign (Memory/Stream Alignment)
            println("Performing Zipalign...")
            val alignedApk = File(workspace, "aligned.apk")
            zipAligner.align(unalignedApk, alignedApk)
            
            // Step 6: Load Keystore and Sign with apksig
            println("Signing APK...")
            var privateKey: PrivateKey? = null
            var certificate: X509Certificate? = null
            
            if (keystoreFile != null && keystoreFile.exists() && keystorePass != null && keyAlias != null && keyPass != null) {
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                keystoreFile.inputStream().use { keyStore.load(it, keystorePass.toCharArray()) }
                privateKey = keyStore.getKey(keyAlias, keyPass.toCharArray()) as PrivateKey
                certificate = keyStore.getCertificate(keyAlias) as X509Certificate
            }
            
            signer.sign(alignedApk, outputApk, privateKey, certificate)
            
            println("Hybrid Build complete! APK located at: ${outputApk.absolutePath}")
        } finally {
            // Ensure no intermediate artifacts remain on device storage
            workspace.deleteRecursively()
        }
    }

    /**
     * Resets the compilation workspace and purges all internal compiler caches.
     * Use this between distinct project sessions to flush caches without restarting the app or leaking memory.
     */
    fun resetWorkspace(outputApk: File) {
        val workspace = File(outputApk.parentFile, "build_workspace")
        if (workspace.exists()) {
            workspace.deleteRecursively()
        }
        com.anyonehub.abl.utils.AnnotationCache.clear()
    }
}
