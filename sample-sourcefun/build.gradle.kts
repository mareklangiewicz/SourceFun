import pl.mareklangiewicz.sourcefun.*
import pl.mareklangiewicz.ure.*
import pl.mareklangiewicz.annotations.*
import pl.mareklangiewicz.defaults.*
import pl.mareklangiewicz.deps.*
import pl.mareklangiewicz.utils.*

plugins {
  plug(plugs.NexusPublish) // not really used in this sample, but needed for [Root Build Template] to compile
  plug(plugs.KotlinMulti) apply false
  id("pl.mareklangiewicz.sourcefun") version "0.4.01"
}

defaultBuildTemplateForRootProject(
  myLibDetails(
    name = "Sample-SourceFun",
    description = "Sample-SourceFun",
    githubUrl = "https://github.com/mareklangiewicz/SourceFun/tree/master/sample-sourcefun",
    version = Ver(0, 1, 1),
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
  val processExtensions1 by reg {
    // doNotTrackState("debugging")
    src = extensionsPath / "SpecialExtensions.kt"
    out = extensionsPath
    setTransformFun { transformSpecialExtensionsContent(it) }
  }
  def("processExtensions2deprecated", extensionsPath, extensionsPath) {
    if (name != "SpecialExtensions.kt") return@def null
    transformSpecialExtensionsContent(it)
  }
}

tasks.register<SourceFunTask>("reportStuff1") {
  group = "awesome"
  src = extensionsPath
  out = reportsPath
  setVisitPathFun { inPath, outPath -> println(inPath); println(outPath) }
}

tasks.register<SourceRegexTask>("reportStuff2") {
  group = "awesome"
  src = extensionsPath
  out = reportsPath
  match.set("Ar*ay")
  replace.set("XXX")
}


@OptIn(DelicateApi::class, NotPortableApi::class)
fun transformSpecialExtensionsContent(content: String): String {
  val regionByteLabel = "Byte Special Extensions"
  val regionGeneratedLabel = "Generated Special Extensions"
  val ureSpecialExtensionsKt = ure {
    val ureRegionByte = ureRegion(ureWhateva().withName("template"), regionByteLabel)
    val ureRegionGenerated = ureRegion(ureWhateva(), regionGeneratedLabel)
    val ureBeforeGenerated = ure {
      +ureWhateva()
      +ureRegionByte
      +ureWhateva()
    }
    +ureBeforeGenerated.withName("before")
    +ureRegionGenerated // no need for name because we will replace it
  }
  val matchResult = ureSpecialExtensionsKt.compile().matchEntire(content)!!
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


// region [Root Build Template]

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
 * * See DepsKt/template-mpp/template-mpp-lib/build.gradle.kts
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

// endregion [Root Build Template]
