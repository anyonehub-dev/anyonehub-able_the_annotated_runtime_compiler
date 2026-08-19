plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.anyonehub"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

gradlePlugin {
    plugins {
        create("ablCompilerPlugin") {
            id = "abl.runtime.compiler"
            implementationClass = "com.anyonehub.abl.plugin.AblCompilerPlugin"
        }
    }
}
