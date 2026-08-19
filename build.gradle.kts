plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "8.3.0"
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
}

tasks.named<Jar>("shadowJar") {
    archiveClassifier.set("all")
    // Note: minimize() is intentionally not called to preserve reflection targets and dynamic apksig classes.
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named("shadowJar"))
        }
    }
}
