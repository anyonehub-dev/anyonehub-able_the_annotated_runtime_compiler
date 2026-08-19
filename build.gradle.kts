import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
    `maven-publish`
}

group = "com.anyonehub"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    
    // Embeddable compiler and R8 toolchain for dynamic compilation
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.10")
    implementation("com.android.tools:r8:9.4.12")
    
    // Eclipse Compiler for Java (ECJ)
    implementation("org.eclipse.jdt:ecj:3.36.0")
    
    // APK Signature Scheme
    implementation("com.android.tools.build:apksig:8.5.0")
    
    // FlatBuffers for zero-copy AST serialization
    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
}

val generateFlatBuffers = tasks.register<Exec>("generateFlatBuffers") {
    group = "flatbuffers"
    description = "Generates Kotlin/Java classes from ABL FlatBuffers schemas using flatc"
    
    val schemaDir = file("src/main/flatbuffers")
    val outputDir = file("src/main/java")
    
    outputs.dir(outputDir)
    inputs.dir(schemaDir)
    
    // Assumes flatc is available in system PATH or configured locally; adjust path if bundled
    executable = "C:\\Users\\tomsl\\Downloads\\Windows.flatc.binary\\flatc.exe"
    args("--java", "-o", outputDir.absolutePath, "$schemaDir/abl_ast.fbs")
    
    doFirst {
        outputDir.mkdirs()
    }
    
    doLast {
        // The flatc executable (25.12.19) differs from the Java dependency (25.2.10).
        // Patching the version check to avoid compilation errors.
        outputDir.walkTopDown().filter { it.extension == "java" }.forEach { file ->
            val content = file.readText()
            val patched = content.replace("Constants.FLATBUFFERS_25_12_19();", "/* patched */")
            file.writeText(patched)
        }
    }
}

// Hook code generation into Java/Kotlin compilation flow
tasks.named("compileKotlin") {
    dependsOn(generateFlatBuffers)
}

// 1. Configure shadowJar to ONLY process dependencies (no source code)
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dependencies")
    
    // EXCLUDE OUR CLASSES FROM THE SHADOW JAR ENTIRELY to bypass the remapper crash
    exclude("com/anyonehub/abl/**")
    
    configurations = listOf(project.configurations.runtimeClasspath.get())
    
    relocate("org.jetbrains.org.objectweb.asm", "com.anyonehub.abl.internal.asm")
    relocate("org.jetbrains.kotlin", "com.anyonehub.abl.internal.kotlin")
    
    // New Relocations for Coroutines and Concurrency
    relocate("kotlinx.coroutines", "com.anyonehub.abl.shadow.kotlinx.coroutines")
    relocate("org.jetbrains.concurrency", "com.anyonehub.abl.shadow.org.jetbrains.concurrency")
    
    // Relocate FlatBuffers
    relocate("com.google.flatbuffers", "com.anyonehub.abl.shadow.flatbuffers")
    
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-script-runtime.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-script.*"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-.*"))
        exclude(dependency("com.android.tools:.*"))
        exclude(dependency("com.android.tools.build:.*"))
        exclude(dependency("com.android.tools.r8:.*"))
    }
}

// 2. Configure the standard jar task to merge our clean source code WITH the relocated dependencies
tasks.named<Jar>("jar") {
    archiveClassifier.set("") // This becomes the master artifact
    
    // Depend on the shadow task and extract its safely-relocated contents into our jar
    dependsOn("shadowJar")
    from(zipTree(tasks.named<ShadowJar>("shadowJar").get().archiveFile)) {
        exclude("META-INF/MANIFEST.MF")
    }
}

// 3. Publish the standard merged jar, not the shadowJar
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named("jar"))
        }
    }
}
