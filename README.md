<p align="center">
  <img src="assets/logo.svg" alt="ABL Logo" width="200"/>
</p>

# ABL Annotated Runtime Compiler

ABL is a revolutionary, fully on-device Android compilation engine designed to dynamically parse, compile, and execute annotated Kotlin code entirely on the metal. It strictly bypasses traditional build-time constraints like KSP and Gradle, outputting fully signed, deployable APKs directly from an Android device memory buffer.

## Architecture Overview: The Hybrid W^X Bypass
To conform to modern Android 10+ W^X (Write XOR Execute) security restrictions while maximizing extreme memory efficiency, ABL uses a **Hybrid Execution Pipeline**:

1. **Native AAPT2 Linking**: 
   Because AAPT2 requires native OS execution to link resources, ABL securely bypasses W^X memory restrictions by disguising the compiler as an NDK `libaapt2.so` binary within the host app's `jniLibs`. `ProcessBuilder` dynamically executes this binary safely.
   
2. **Embeddable JVM Bridge**: 
   The `kotlin-compiler-embeddable` leverages a strict Zero Footprint Policy, temporarily writing raw `.kt` buffers to a fast memory-mapped file system just long enough to harvest the compiled `.class` bytes natively into memory, before instantaneously executing an atomic wipe.
   
3. **In-Memory D8 Dexing**: 
   To transpile the JVM bytes to Dalvik bytecode, the R8 `DexIndexedConsumer` is dynamically overridden to pipe the generated DEX bytes directly into a `ByteArrayOutputStream`. **Zero disk I/O occurs during D8 execution.**
   
4. **Ephemeral Signing & Alignment**:
   The `classes.dex` and `resources.arsc` payloads are merged using a pure-Kotlin stream Zipaligner. If a keystore is not provided, the engine utilizes standard Android API reflection to invoke `AndroidKeyStore` and dynamically generate an ephemeral 2048-bit RSA Keypair and self-signed X.509 certificate on the fly.

## Getting Started

ABL provides a seamless Gradle DSL wrapper for developers aiming to automate the execution pipeline.

### Native Toolchain Provisioning (CMake & AAPT2)
Because ABL compiles resources directly on the Android device, it requires native binaries (like CMake and AAPT2) for execution.

The ABL Gradle plugin automatically downloads, extracts, and provisions the native toolchain for the `arm64-v8a` architecture during project configuration. The toolchain is bundled securely and paths are managed internally by the plugin, meaning you no longer need to manually manage `jniLibs` assets or DSL properties.

### Adding the Plugin
In your consumer project's `build.gradle.kts`, apply the plugin and define the ABL configurations:

```kotlin
plugins {
    id("abl.runtime.compiler") version "1.0-SNAPSHOT"
}

dependencies {
    // 1. The runtime engine dependency (analogous to hilt-android)
    implementation("com.anyonehub:abl-runtime-compiler:1.0-SNAPSHOT")

    // 2. The dedicated ABL compiler/processor dependency (analogous to hilt-compiler / ksp)
    abl("com.anyonehub:abl-compiler:1.0-SNAPSHOT")
}

abl {
    manifestFile.set(file("src/main/AndroidManifest.xml"))
    resDir.set(file("src/main/res"))
}
```

When you execute the `compileAbl` Gradle task, the plugin harvests your `src/main/kotlin` directory, wires your dependencies, and initiates the ABL `ApkBuildPipeline`.
