@file:Suppress("UnstableApiUsage", "unused")

import pl.mareklangiewicz.defaults.*
import pl.mareklangiewicz.deps.*
import pl.mareklangiewicz.utils.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import com.vanniktech.maven.publish.*

plugins {
  plugAll(plugs.KotlinJvm, plugs.GradlePublish, plugs.VannikPublish)

  // Note: I could probably easily include all deps in fat jar by just adding: plug(plugs.GradleShadow),
  // but let's not do it yet; someday maybe (so I can publish all my new kground stuff fast from maven local)
  // https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#shadow_dependencies
}

repositories {
  mavenLocal()
  google()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  api(Com.SquareUp.Okio.okio)
  api(Langiewicz.kground)
  api(Langiewicz.kgroundx)
  api(Langiewicz.kground_io)
  api(Langiewicz.kgroundx_io)
  api(Langiewicz.kgroundx_maintenance)
  api(Langiewicz.kommand_line)
  api(Langiewicz.kommand_samples)
  testImplementation(Langiewicz.uspekx_junit5)
  testImplementation(Org.JUnit.Jupiter.junit_jupiter)
  testImplementation(Org.JUnit.Jupiter.junit_jupiter_engine)
  testRuntimeOnly(Org.JUnit.Platform.junit_platform_launcher)
  // Explicit platform launcher required in new gradle:
  // https://docs.gradle.org/8.13/userguide/upgrading_version_8.html#test_framework_implementation_dependencies

  // TODO: check separation between api and engine - so I can do similar in ULog (with separate bridges to CLog etc.)
}

val kgVer = "0.1.22" // https://central.sonatype.com/artifact/pl.mareklangiewicz/kground/versions

setMyWeirdSubstitutions(
  "kground" to kgVer,
  "kgroundx" to kgVer,
  "kground-io" to kgVer,
  "kgroundx-io" to kgVer,
  "kgroundx-maintenance" to kgVer,
  "kommand-line" to kgVer,
  "kommand-samples" to kgVer,
)

tasks.defaultKotlinCompileOptions()

tasks.defaultTestsOptions()

val details = myLibDetails(
  name = "SourceFun",
  group = "pl.mareklangiewicz.deps", // important non default ...deps group (as accepted on gradle portal)
  // see before any decision to change the group: https://plugins.gradle.org/docs/publish-plugin#approval
  description = "Maintain typical java/kotlin/android projects sources with fun.",
  githubUrl = "https://github.com/mareklangiewicz/SourceFun",
  version = Ver(0, 4, 39), // https://plugins.gradle.org/search?term=pl.mareklangiewicz
  settings = LibSettings(
    withJs = false,
    compose = null,
  ),
)

defaultBuildTemplateForRootProject(details)

kotlin {
  jvmToolchain(23)
}

defaultPublishing(details)

gradlePlugin {
  website.set("https://github.com/mareklangiewicz/SourceFun")
  vcsUrl.set("https://github.com/mareklangiewicz/SourceFun")
  plugins {
    create("sourceFunPlugin") {
      id = "pl.mareklangiewicz.sourcefun"
      implementationClass = "pl.mareklangiewicz.sourcefun.SourceFunPlugin"
      displayName = "SourceFun plugin"
      description = "Maintain typical java/kotlin/android projects sources with fun."
      tags.set(listOf("SourceTask", "DSL"))
    }
  }
}


// region [[Root Build Template]]

fun Project.defaultBuildTemplateForRootProject(details: LibDetails? = null) {
  details?.let {
    rootExtLibDetails = it
    defaultGroupAndVerAndDescription(it)
  }
}

// endregion [[Root Build Template]]

// region [[Kotlin Module Build Template]]

// Kind of experimental/temporary.. not sure how it will evolve yet,
// but currently I need these kind of substitutions/locals often enough
// especially when updating kground <-> kommandline (trans deps issues)
fun Project.setMyWeirdSubstitutions(
  vararg rules: Pair<String, String>,
  myProjectsGroup: String = "pl.mareklangiewicz",
  tryToUseLocalProjects: Boolean = true,
) {
  val foundLocalProjects: Map<String, Project?> =
    if (tryToUseLocalProjects) rules.associate { it.first to findProject(":${it.first}") }
    else emptyMap()
  configurations.all {
    resolutionStrategy.dependencySubstitution {
      for ((projName, projVer) in rules)
        substitute(module("$myProjectsGroup:$projName"))
          .using(
            // Note: there are different fun in gradle: Project.project; DependencySubstitution.project
            if (foundLocalProjects[projName] != null) project(":$projName")
            else module("$myProjectsGroup:$projName:$projVer")
          )
    }
  }
}

fun RepositoryHandler.addRepos(settings: LibReposSettings) = with(settings) {
  @Suppress("DEPRECATION")
  if (withMavenLocal) mavenLocal()
  if (withMavenCentral) mavenCentral()
  if (withGradle) gradlePluginPortal()
  if (withGoogle) google()
  if (withKotlinx) maven(repos.kotlinx)
  if (withKotlinxHtml) maven(repos.kotlinxHtml)
  if (withComposeJbDev) maven(repos.composeJbDev)
  if (withKtorEap) maven(repos.ktorEap)
  if (withJitpack) maven(repos.jitpack)
}

// TODO_maybe: doc says it could be now also applied globally instead for each task (and it works for andro too)
//   But it's only for jvm+andro, so probably this is better:
//   https://kotlinlang.org/docs/gradle-compiler-options.html#for-all-kotlin-compilation-tasks
fun TaskCollection<Task>.defaultKotlinCompileOptions(
  apiVer: KotlinVersion = KotlinVersion.KOTLIN_2_1,
  jvmTargetVer: String? = null, // it's better to use jvmToolchain (normally done in fun allDefault)
  renderInternalDiagnosticNames: Boolean = false,
) = withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    apiVersion.set(apiVer)
    jvmTargetVer?.let { jvmTarget = JvmTarget.fromTarget(it) }
    if (renderInternalDiagnosticNames) freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
    // useful, for example, to suppress some errors when accessing internal code from some library, like:
    // @file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "EXPOSED_PARAMETER_TYPE", "EXPOSED_PROPERTY_TYPE", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
  }
}

fun TaskCollection<Task>.defaultTestsOptions(
  printStandardStreams: Boolean = true,
  printStackTraces: Boolean = true,
  onJvmUseJUnitPlatform: Boolean = true,
) = withType<AbstractTestTask>().configureEach {
  testLogging {
    showStandardStreams = printStandardStreams
    showStackTraces = printStackTraces
  }
  if (onJvmUseJUnitPlatform) (this as? Test)?.useJUnitPlatform()
}

// Provide artifacts information requited by Maven Central
fun MavenPom.defaultPOM(lib: LibDetails) {
  name put lib.name
  description put lib.description
  url put lib.githubUrl

  licenses {
    license {
      name put lib.licenceName
      url put lib.licenceUrl
    }
  }
  developers {
    developer {
      id put lib.authorId
      name put lib.authorName
      email put lib.authorEmail
    }
  }
  scm { url put lib.githubUrl }
}

fun Project.defaultPublishing(lib: LibDetails) = extensions.configure<MavenPublishBaseExtension> {
  propertiesTryOverride("signingInMemoryKey", "signingInMemoryKeyPassword", "mavenCentralPassword")
  if (lib.settings.withCentralPublish) publishToMavenCentral(automaticRelease = false)
  signAllPublications()
  signAllPublicationsFixSignatoryIfFound()
  // Note: artifactId is not lib.name but current project.name (module name)
  coordinates(groupId = lib.group, artifactId = name, version = lib.version.str)
  pom { defaultPOM(lib) }
}

// endregion [[Kotlin Module Build Template]]
