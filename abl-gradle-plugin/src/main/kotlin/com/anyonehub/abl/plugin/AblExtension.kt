package com.anyonehub.abl.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class AblExtension {
    abstract val aapt2BinaryPath: Property<String>
    abstract val cmakeBinaryPath: Property<String>
    abstract val androidJarPath: Property<String>
    abstract val manifestFile: RegularFileProperty
    abstract val resDir: DirectoryProperty
    
    // Optional Keystore properties
    abstract val keystoreFile: RegularFileProperty
    abstract val keystorePass: Property<String>
    abstract val keyAlias: Property<String>
    abstract val keyPass: Property<String>
}
