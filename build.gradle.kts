plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // MongoDB driver needed at runtime.
    implementation("org.mongodb:mongodb-driver-sync:4.10.2")

    // Spigot API (provided as compileOnly).
    compileOnly(files("libs/spigot-api-1.21.4-R0.1-SNAPSHOT.jar"))

    // Explicitly add ASM dependencies that support Java 21/22.
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-commons:9.4")
}

// Force any dependency on ASM to use version 9.4.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm") {
            useVersion("9.4")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    // Set the toolchain to Java 23 (or a version you have installed).
    toolchain.languageVersion.set(JavaLanguageVersion.of(23))
}

tasks.shadowJar {
    archiveClassifier.set("") // Remove classifier to override the default JAR name.

    // Relocate MongoDB driver packages to avoid conflicts with other plugins.
    relocate("org.bson", "org.example.shaded.org.bson")
    relocate("com.mongodb", "org.example.shaded.com.mongodb")
}

// Automatically copy the built shadow JAR to the server plugins directory.
tasks.register<Copy>("copyToIntelliJServer") {
    dependsOn(tasks.shadowJar)
    from(layout.buildDirectory.file("libs/${project.name}-${project.version}.jar"))
    into(file("${rootProject.rootDir}/server/plugins/"))
}

// Ensure the JAR is copied after build.
tasks.build {
    finalizedBy("copyToIntelliJServer")
}
