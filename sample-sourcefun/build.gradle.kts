import pl.mareklangiewicz.sourcefun.*
import pl.mareklangiewicz.ure.*
import pl.mareklangiewicz.utils.*

plugins {
  id("pl.mareklangiewicz.sourcefun") version "0.4.01"
}

val extensionsPath get() = rootProjectPath / "sample-lib/src/main/kotlin/extensions"
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


fun transformSpecialExtensionsContent(content: String): String {
  val regionByteName = "Byte Special Extensions"
  val regionGeneratedName = "Generated Special Extensions"
  val ureSpecialExtensionsKt = ure {
    val ureRegionByte = ureRegion(ureWhateva().withName("template"), ureRaw(regionByteName))
    val ureRegionGenerated = ureRegion(ureWhateva(), ir(regionGeneratedName))
    val ureBeforeGenerated = ure {
      1 of ureWhateva()
      1 of ureRegionByte
      1 of ureWhateva()
    }
    1 of ureBeforeGenerated.withName("before")
    1 of ureRegionGenerated // no need for name because we will replace it
  }
  val matchResult = ureSpecialExtensionsKt.compile().matchEntire(content)!!
  val before by matchResult
  val template by matchResult
  val generated = listOf("Short", "Int", "Long", "Float", "Double", "Boolean", "Char")
    .joinToString(
      separator = "",
      prefix = "// region $regionGeneratedName\n",
      postfix = "// endregion $regionGeneratedName",
    ) { template.replace("Byte", it) }
  return before + generated
}

