package com.anyonehub.abl.signing

import com.android.apksig.ApkSigner
import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

class ApkSignerWrapper {

    /**
     * Signs the aligned APK using v2/v3 signatures.
     * If no keystore credentials are provided, dynamically generates an ephemeral key on-device.
     */
    fun sign(
        alignedApk: File, 
        finalApk: File, 
        privateKey: PrivateKey? = null, 
        certificate: X509Certificate? = null
    ) {
        val (finalKey, finalCert) = if (privateKey != null && certificate != null) {
            Pair(privateKey, certificate)
        } else {
            generateEphemeralKeyAndCert()
        }

        val signerConfigs = listOf(
            ApkSigner.SignerConfig.Builder("debug_signer", finalKey, listOf(finalCert)).build()
        )
        
        val signer = ApkSigner.Builder(signerConfigs)
            .setInputApk(alignedApk)
            .setOutputApk(finalApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            
        signer.sign()
    }

    /**
     * Dynamically generates a 2048-bit RSA KeyPair and a basic self-signed X509Certificate 
     * using standard Android APIs (AndroidKeyStore).
     */
    private fun generateEphemeralKeyAndCert(): Pair<PrivateKey, X509Certificate> {
        try {
            val kpg = java.security.KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
            
            // Reflection to access android.security.keystore.KeyGenParameterSpec without compiling against Android SDK
            val specBuilderClass = Class.forName("android.security.keystore.KeyGenParameterSpec\$Builder")
            val purpose = 4 or 8 // KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY
            val specBuilder = specBuilderClass.getConstructor(String::class.java, Int::class.javaPrimitiveType)
                .newInstance("AblEphemeralKey", purpose)
                
            val setSubjectMethod = specBuilderClass.getMethod("setCertificateSubject", X500Principal::class.java)
            setSubjectMethod.invoke(specBuilder, X500Principal("CN=AblEphemeral"))
            
            val buildMethod = specBuilderClass.getMethod("build")
            val spec = buildMethod.invoke(specBuilder)
            
            kpg.initialize(spec as java.security.spec.AlgorithmParameterSpec)
            val keyPair = kpg.generateKeyPair()
            
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate("AblEphemeralKey") as X509Certificate
            
            return Pair(keyPair.private, cert)
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate ephemeral key. Ensure this executes on an Android device.", e)
        }
    }
}
