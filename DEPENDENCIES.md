# Dependency Manifest

The ABL Runtime Compiler engine and its Gradle Plugin wrapper rely on the following exact coordinates for execution. The heavy toolchain components (Kotlin, R8, apksig) are successfully bundled into our Fat JAR via the `com.gradleup.shadow` plugin for complete standalone operability on-device.

### Official Shipped Dependencies

**Engine (`com.anyonehub:abl-runtime-compiler:1.0.0`)**
- `org.jetbrains.kotlin:kotlin-stdlib:2.4.10`
- `org.jetbrains.kotlin:kotlin-reflect:2.4.10`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0`
- `org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.10` (Embedded Compiler)
- `org.eclipse.jdt:ecj:3.36.0` (Eclipse Compiler for Java)
- `com.android.tools:r8:9.4.12` (D8 Dexing)
- `com.android.tools.build:apksig:8.5.0` (v2/v3 Signing)

**Plugin Wrapper (`com.anyonehub:abl-gradle-plugin:1.0.0`)**
- `gradle-api` (Injected via `java-gradle-plugin`)
- `kotlin-dsl` (Version bound to Gradle environment)
- Depends strictly on `:abl-runtime-compiler` Engine artifact.

**Build Infrastructure**
- `com.gradleup.shadow:8.3.0` (Shadow Jar / Fat Jar packaging)

### Integration Coordinates & Usage

When integrating the ABL Engine into a host project (e.g., Anyone-Hub), use the published coordinates. The Gradle Plugin dynamically registers the custom `abl` dependency configuration bucket.

> **DSL & Dependency Separation:** Much like how Hilt utilizes `implementation` for standard runtime APIs alongside `ksp` for its processor, ABL utilizes the custom `abl("...")` configuration. This strictly isolates the dynamic on-device modules from the host app's standard `implementation` classpath. The `CompileAblModulesTask` harvests these specific dependencies to feed into the embedded compiler.

```kotlin
plugins {
    id("abl.runtime.compiler") version "1.0.0"
}

dependencies {
    // 1. The runtime engine dependency (analogous to hilt-android)
    implementation("com.anyonehub:abl-runtime-compiler:1.0.0")

    // 2. The dedicated ABL compiler/processor dependency (analogous to hilt-compiler / ksp)
    abl("com.anyonehub:abl-compiler:1.0.0")
}

abl {
    manifestFile.set(file("src/main/AndroidManifest.xml"))
    resDir.set(file("src/main/res"))
}
```

**Direct Engine Consumption (Optional):**
If a project needs to interact with the engine pipeline programmatically without the Gradle Plugin:
```kotlin
implementation("com.anyonehub:abl-runtime-compiler:1.0.0")
```
