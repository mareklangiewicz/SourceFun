rootProject.name = "sample-sourcefun"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
  }
//    includeBuild("..") // DepsKt
}

plugins {
  id("pl.mareklangiewicz.deps.settings") version "0.3.09" // https://plugins.gradle.org/search?term=mareklangiewicz
}

include(":sample-lib")
