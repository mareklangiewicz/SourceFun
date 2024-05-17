package pl.mareklangiewicz.sourcefun

import okio.*
import okio.FileSystem.Companion.SYSTEM
import okio.Path.Companion.toPath
import org.gradle.testfixtures.*
import org.gradle.testkit.runner.*
import org.gradle.testkit.runner.TaskOutcome.*
import org.junit.jupiter.api.*
import pl.mareklangiewicz.bad.*
import pl.mareklangiewicz.io.*
import pl.mareklangiewicz.uspek.*

class SourceFunTests {

  @TestFactory
  fun sourceFunTests() = uspekTestFactory {
    onExampleWithProjectBuilder()
    onSingleHelloWorldProject()
    onSampleSourceFunProject()
  }
}

private fun onExampleWithProjectBuilder() {
  "On example with ProjectBuilder" o {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(SourceFunPlugin::class.java)
    // TODO_maybe: how to configure my plugin in such test? (so I can assert it creates appropriate tasks)
    project.plugins.any { it is SourceFunPlugin } chkEq true
  }
}

@Suppress("SameParameterValue")
private fun withTempProject(settingsKtsContent: String, buildKtsContent: String, code: (tempDir: Path) -> Unit) =
  SYSTEM.withTempDir(tempDirPrefix = "sourceFunTest") { tempDir ->
    writeUtf8(tempDir / "settings.gradle.kts", settingsKtsContent)
    writeUtf8(tempDir / "build.gradle.kts", buildKtsContent)
    code(tempDir)
  }

private fun onSingleHelloWorldProject() {
  "On single hello world project" o {
    val settingsKtsContent = """rootProject.name = "hello-world""""
    val buildKtsContent =
      """
      tasks.register("helloWorld") {
        doLast {
          println("Hello world!")
        }
      }

      tasks.register("helloFail") {
        doLast {
          println("The exception is coming!")
          throw RuntimeException("helloFail exception")
        }
      }
      """.trimIndent()


    withTempProject(settingsKtsContent, buildKtsContent) { tempDir ->
      "On gradle runner within temp environment" o {

        val runner = GradleRunner.create().withProjectPath(tempDir)
        //.withPluginClasspath() // see comment under other commented out invocation below

        "On task helloWorld" o {
          runner.withArguments("helloWorld")

          "On gradle build" o {
            val result = runner.build()

            // not really needed because build() would throw if didn't end successfully; leaving as example of .outcome
            "task helloWorld ends successfully" o { result.task(":helloWorld")?.outcome chkEq SUCCESS }

            "output contains hello world message" o { result.output.contains("Hello world!") chkEq true }
          }
        }

        "On nonexistent task" o {
          runner.withArguments("someNonExistentTask")

          "gradle fails" o { runner.buildAndFail() }
        }

        "On task helloFail" o {
          runner.withArguments("helloFail")
          "On gradle failing build" o {
            val result = runner.buildAndFail()

            "task helloFail ends with failure" o { result.task(":helloFail")?.outcome chkEq FAILED }
            "output contains hello fail message" o { result.output.contains("The exception is coming!") chkEq true }
          }
        }
      }
    }
  }
}

private val theSourceFunProjectPath = System.getenv("GITHUB_WORKSPACE")?.toPath()
  ?: (System.getenv("HOME").chkNN { "No HOME env var available."}.toPath() / "code/kotlin/SourceFun")
private val sampleSourceFunProjectPath = theSourceFunProjectPath / "sample-sourcefun"

private fun onSampleSourceFunProject() {
  "On sample-sourcefun project" o {

    // Note: the sample-sourcefun project settings.gradle.kts -> pluginManagement -> includeBuild("..")
    // So it's composite-build that include THIS (SourceFun) project back! (sort of circular "dependency"?)
    // But I guess GradleRunner/GradleTestKit separates managed builds enough, so it's working fine.
    val runner = GradleRunner.create().withProjectPath(sampleSourceFunProjectPath)
    //.withPluginClasspath()

    /*
    It would be nicer to use .withPluginClasspath() it so runner inject current SourceFun plugin to tested project,
    but then I get weird error:
    * What went wrong:

    Execution failed for task ':tasks'.
    > Could not create task ':processExtensions1ByReg'.
    > loader constraint violation: when resolving method 'void pl.mareklangiewicz.sourcefun.TasksKt.setSrc(pl.mareklangiewicz.sourcefun.SourceFunTask, okio.Path)' the class loader org.gradle.internal.classloader.VisitableURLClassLoader @7dce4f5e of the current class, Build_gradle$1$processExtensions1ByReg$2, and the class loader org.gradle.internal.classloader.VisitableURLClassLoader$InstrumentingVisitableURLClassLoader @4078a234 for the method's defining class, pl/mareklangiewicz/sourcefun/TasksKt, have different Class objects for the type okio/Path used in the signature (Build_gradle$1$processExtensions1ByReg$2 is in unnamed module of loader org.gradle.internal.classloader.VisitableURLClassLoader @7dce4f5e, parent loader org.gradle.internal.classloader.CachingClassLoader @783b0e4b; pl.mareklangiewicz.sourcefun.TasksKt is in unnamed module of loader org.gradle.internal.classloader.VisitableURLClassLoader$InstrumentingVisitableURLClassLoader @4078a234, parent loader org.gradle.internal.classloader.CachingClassLoader @5c7ab031)

    So my workaround is not do it and use composite-build inside tested sample-sourcefun project, to include SourceFun code from there
    (This workaround is pretty nice anyway when opening sample-sourcefun in IDE, because it allows me to test/work on both sides manually)
    */

    "On gradle tasks command" o {
      runner.withArguments("tasks")
      val result = runner.build()

      "All awesome tasks printed" o {
        val lines = result.output.lines()
        val idx = lines.indexOf("Awesome tasks")
        chk(idx > 0)
        lines[idx + 2] chkEq "fakeReportStuff1JustPrintLn"
        lines[idx + 3] chkEq "fakeReportStuff2UreArrayToXXX"
        lines[idx + 4] chkEq "processExtensions1ByReg"
        lines[idx + 5] chkEq "processExtensions2WithDefDeprecated"
      }
    }

    "On clean gradle cache and build dir programmatically" o {
      SYSTEM.deleteTreeWithDoubleChk(sampleSourceFunProjectPath / ".gradle", mustExist = false) { "sourcefun" in it }
      SYSTEM.deleteTreeWithDoubleChk(sampleSourceFunProjectPath / "build", mustExist = false) { "sourcefun" in it }

      "On task processExtensions1ByReg" o {
        runner.withArguments("processExtensions1ByReg")
        val result = runner.build()

        // TODO: mess with generated source code and check if the task fixes it.
      }

      "On task fakeReportStuff1JustPrintLn" o {
        runner.withArguments("fakeReportStuff1JustPrintLn")
        val result = runner.build()

        "visited files are printed" o {
          chk("GenericExtensions.kt" in result.output)
          chk("SpecialExtensions.kt" in result.output)
        }
      }

      "On task fakeReportStuff2UreArrayToXXX" o {
        runner.withArguments("fakeReportStuff2UreArrayToXXX")
        val result = runner.build()

        "On generated reports" o {
          val reportsPaths = SYSTEM.list(sampleSourceFunProjectPath / "build/awesome-reports")
          val reportsNames = reportsPaths.map { it.name }

          "generated two files" o { reportsNames chkEq listOf("GenericExtensions.kt", "SpecialExtensions.kt") }

          for (reportPath in reportsPaths) "On report file ${reportPath.name}" o {
            val content = SYSTEM.readUtf8(reportPath)

            "no Array word in it" o { Regex("Array").containsMatchIn(content) chkEq false }
            "some XXX words instead" o { check(Regex("XXX").findAll(content).count() > 2) }
          }
        }
      }
    }
  }
}

private fun GradleRunner.withProjectPath(path: Path) = withProjectDir(path.toFile())

// FIXME: use impl from kground-io after update kground
// Note: it should pop up as better alternative to okio deleteRecursively in autocompletion.
// I think it's good idea to always double-check some specific part of rootPath
// to make sure we're not accidentally deleting totally wrong dir tree.
fun FileSystem.deleteTreeWithDoubleChk(
  rootPath: Path,
  mustExist: Boolean = true,
  mustBeDir: Boolean = true,
  doubleChk: (rootPathString: String) -> Boolean, // mandatory on purpose
) {
  val rootPathString = rootPath.toString()
  val md = metadataOrNull(rootPath) ?: run {
    mustExist.chkFalse { "Tree rootPath: $rootPathString does NOT exist."}
    return // So it doesn't exist and it's fine; nothing to delete.
  }
  chk(!mustBeDir || md.isDirectory) { "Tree rootPath: $rootPathString is NOT directory." }
  doubleChk(rootPathString).chkTrue { "Can NOT remove $rootPathString tree because doubleChk failed." }
  deleteRecursively(rootPath, mustExist)
}
