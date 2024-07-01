
// region [Custom Basic Root Build Imports and Plugs]

import pl.mareklangiewicz.sourcefun.*
import pl.mareklangiewicz.ure.*
import pl.mareklangiewicz.annotations.*
import pl.mareklangiewicz.defaults.*
import pl.mareklangiewicz.ure.*
import pl.mareklangiewicz.ure.UReplacement.Companion.Literal
import pl.mareklangiewicz.deps.*
import pl.mareklangiewicz.utils.*

plugins {
  plug(plugs.KotlinMulti) apply false
  plug(plugs.NexusPublish)
  id("pl.mareklangiewicz.sourcefun") version "0.4.13" // I includeBuild("..") in settings so version doesn't matter
}

// endregion [Custom Basic Root Build Imports and Plugs]

buildscript {
  dependencies {
    // https://s01.oss.sonatype.org/content/repositories/releases/pl/mareklangiewicz/kommandline/
    // classpath("pl.mareklangiewicz:kommandline:0.0.67")
    // https://s01.oss.sonatype.org/content/repositories/releases/pl/mareklangiewicz/kgroundx-maintenance/
    // classpath("pl.mareklangiewicz:kgroundx-maintenance:0.0.61")
  }
}

defaultBuildTemplateForRootProject(
  myLibDetails(
    name = "Sample-SourceFun",
    description = "Sample-SourceFun",
    githubUrl = "https://github.com/mareklangiewicz/SourceFun/tree/master/sample-sourcefun",
    version = Ver(0, 1, 5),
    settings = LibSettings(
      withJs = false,
      withNativeLinux64 = false,
      compose = null,
      withSonatypeOssPublishing = false,
    ),
  ),
)

val extensionsPath get() = rootProjectPath / "sample-lib/src/jvmMain/kotlin/extensions"
val reportsPath get() = buildPath / "awesome-reports"

sourceFun {
  grp = "awesome"
  val processExtensions1ByReg by reg {
    // doNotTrackState("debugging")
    src = extensionsPath / "SpecialExtensions.kt"
    out = extensionsPath
    setTransformFun { transformSpecialExtensionsContent(it) }
  }
  def("processExtensions2WithDefDeprecated", extensionsPath, extensionsPath) {
    if (name != "SpecialExtensions.kt") return@def null
    transformSpecialExtensionsContent(it)
  }
}

tasks.register<SourceFunTask>("fakeReportStuff1JustPrintLn") {
  group = "awesome"
  src = extensionsPath
  out = reportsPath
  setForEachFileFun {
    println("FRS1 Faking report (will NOT create any files in $reportsPath)")
    println("FRS1 <- $first:1") // :1 is for IDE to make it clickable
    println("FRS1 -> $second:1") // no file will be there, unless other task created it
  }
}

tasks.register<SourceUreTask>("fakeReportStuff2UreArrayToXXX") {
  group = "awesome"
  src = extensionsPath
  out = reportsPath
  val ure = ure { +ch('A'); 0..MAX of ch('r'); +ureText("ay") }
  match put ure
  replace put Literal("XXX")
}


@OptIn(DelicateApi::class, NotPortableApi::class)
fun transformSpecialExtensionsContent(content: String): String {
  val regionByteLabel = "Byte Special Extensions"
  val regionGeneratedLabel = "Generated Special Extensions"
  val ureSpecialExtensionsKt = ure {
    val ureRegionByte = ureRegion(
      ureWhateva().withName("template"),
      regionLabel = ureText(regionByteLabel),
      regionLabelName = "rln1"
    )
    val ureRegionGenerated = ureRegion(
      ureWhateva(),
      regionLabel = ureText(regionGeneratedLabel),
      regionLabelName = "rln2" // has to be different than above
    )
    val ureBeforeGenerated = ure {
      +ureWhateva()
      +ureRegionByte
      +ureWhateva()
    }
    +ureBeforeGenerated.withName("before")
    +ureRegionGenerated // no need for name because we will replace it
  }
  val matchResult = ureSpecialExtensionsKt.matchEntireOrThrow(content)
  val before by matchResult
  val template by matchResult
  val generated = listOf("Short", "Int", "Long", "Float", "Double", "Boolean", "Char")
    .joinToString(
      separator = "",
      prefix = "// region $regionGeneratedLabel\n",
      postfix = "// endregion $regionGeneratedLabel",
    ) { template.replace("Byte", it) }
  return before + generated
}


// region [[Root Build Template]]

/** Publishing to Sonatype OSSRH has to be explicitly allowed here, by setting withSonatypeOssPublishing to true. */
fun Project.defaultBuildTemplateForRootProject(details: LibDetails? = null) {
  ext.addDefaultStuffFromSystemEnvs()
  details?.let {
    rootExtLibDetails = it
    defaultGroupAndVerAndDescription(it)
    if (it.settings.withSonatypeOssPublishing) defaultSonatypeOssNexusPublishing()
  }

  // kinda workaround for kinda issue with kotlin native
  // https://youtrack.jetbrains.com/issue/KT-48410/Sync-failed.-Could-not-determine-the-dependencies-of-task-commonizeNativeDistribution.#focus=Comments-27-5144160.0-0
  repositories { mavenCentral() }
}

/**
 * System.getenv() should contain six env variables with given prefix, like:
 * * MYKOTLIBS_signing_keyId
 * * MYKOTLIBS_signing_password
 * * MYKOTLIBS_signing_keyFile (or MYKOTLIBS_signing_key with whole signing key)
 * * MYKOTLIBS_ossrhUsername
 * * MYKOTLIBS_ossrhPassword
 * * MYKOTLIBS_sonatypeStagingProfileId
 * * First three of these used in fun pl.mareklangiewicz.defaults.defaultSigning
 * * See KGround/template-full/template-full-lib/build.gradle.kts
 */
fun ExtraPropertiesExtension.addDefaultStuffFromSystemEnvs(envKeyMatchPrefix: String = "MYKOTLIBS_") =
  addAllFromSystemEnvs(envKeyMatchPrefix)

fun Project.defaultSonatypeOssNexusPublishing(
  sonatypeStagingProfileId: String = rootExtString["sonatypeStagingProfileId"],
  ossrhUsername: String = rootExtString["ossrhUsername"],
  ossrhPassword: String = rootExtString["ossrhPassword"],
) {
  nexusPublishing {
    this.repositories {
      sonatype {  // only for users registered in Sonatype after 24 Feb 2021
        stagingProfileId put sonatypeStagingProfileId
        username put ossrhUsername
        password put ossrhPassword
        nexusUrl put repos.sonatypeOssNexus
        snapshotRepositoryUrl put repos.sonatypeOssSnapshots
      }
    }
  }
}

// endregion [[Root Build Template]]
