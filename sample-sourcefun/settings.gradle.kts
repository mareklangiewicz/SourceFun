rootProject.name = "sample-sourcefun"

// Note: Not using special region: 'My Settings Stuff', because pluginManagement has to differ: includeBuild("..")

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }

  includeBuild("..")
  // SourceFun plugin itself
  // Needed also as a workaround for TestKit issue with classloader
  // (see comments in SourceFun:SourceFunTests.kt)
}

plugins {
  id("pl.mareklangiewicz.deps.settings") version "0.3.11" // https://plugins.gradle.org/search?term=mareklangiewicz
}

include(":sample-lib")
