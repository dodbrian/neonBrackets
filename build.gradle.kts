plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "tech.zimin.neonBrackets"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Default configuration uses IntelliJ IDEA Community
dependencies {
    intellijPlatform {
        // Default to IntelliJ IDEA Community Edition
        val ideType = project.findProperty("ideType") ?: "IC"
        when (ideType) {
            "RD" -> rider("2024.1", useInstaller = false)
            // RustRover is relatively new, so we'll use the create method with the type
            "RR" -> create("RR", "2024.1")
            "PC" -> pycharmCommunity("2024.1")
            else -> intellijIdeaCommunity("2024.1")
        }
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    pluginConfiguration {
        name = "Neon Brackets"
        version = "${project.version}"
    }

    // Add plugin verification configuration
    pluginVerification {
        ides {
            // Verify against the same version as the plugin targets
            ide("IC", "2024.1")
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // Fix for StartupActivity warning
    register("fixStartupActivity") {
        doLast {
            println("Fixing StartupActivity warning by updating plugin.xml")
            val pluginXml = file("src/main/resources/META-INF/plugin.xml")
            val content = pluginXml.readText()
            val updated = content.replace(
                "<postStartupActivity implementation=\"tech.zimin.neonbrackets.neonbrackets.NeonBracketsStartupActivity\"/>",
                "<projectService serviceImplementation=\"tech.zimin.neonbrackets.neonbrackets.NeonBracketsStartupActivity\"/>"
            )
            pluginXml.writeText(updated)
        }
    }
}
