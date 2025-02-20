plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mongodb:mongodb-driver-sync:4.10.2")



    // Corrected Spigot API reference
    compileOnly(files("libs/spigot-api-1.21.4-R0.1-SNAPSHOT.jar"))
}

tasks.test {
    useJUnitPlatform()
}

// Set Java version
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21)) // Ensure compatibility with Minecraft versions
}

// Automatically copy the built JAR to IntelliJ project directory: /server/plugins/
tasks.register<Copy>("copyToIntelliJServer") {
    dependsOn(tasks.build)
    from(layout.buildDirectory.file("libs/${project.name}-${project.version}.jar"))
    into(file("${rootProject.rootDir}/server/plugins/")) // Target IntelliJ's directory
}

// Ensure the JAR is copied after build
tasks.build {
    finalizedBy("copyToIntelliJServer")
}
