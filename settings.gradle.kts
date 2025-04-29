pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-plugin-repository") }
    }
    plugins {
        id("org.jetbrains.intellij.platform") version "2.2.1"
    }
}

rootProject.name = "neon-brackets"