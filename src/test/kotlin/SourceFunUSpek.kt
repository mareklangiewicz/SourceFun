package pl.mareklangiewicz.sourcefun

import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.Test
import pl.mareklangiewicz.SourceFunPlugin
import pl.mareklangiewicz.uspek.eq
import pl.mareklangiewicz.uspek.failed
import pl.mareklangiewicz.uspek.o
import pl.mareklangiewicz.uspek.status
import pl.mareklangiewicz.uspek.uspek
import pl.mareklangiewicz.uspek.uspekLog
import java.io.File

class SourceFunUSpek {

    // FIXME: use junit5 and fun uspekTestFactory when gradle starts supporting junit5 for plugin development
    init {
        uspekLog = {
            if (it.failed) {
                System.err.println(it.status)
                throw it.end!!
            } else println(it.status)
        }
    }

    @Test
    fun sourceFunUSpek() = uspek {

        "Example test with ProjectBuilder" o {
            val project = ProjectBuilder.builder().build()!!
            project.pluginManager.apply(SourceFunPlugin::class)
            // TODO_maybe: how to configure my plugin in such test? (so I can assert it creates appropriate tasks)
            project.plugins.any { it is SourceFunPlugin } eq true
        }

        "On create temp project dir" o {
            withTempBuildEnvironment { tempDir, settingsFile, buildFile ->
                onSingleHelloWorld(tempDir, settingsFile, buildFile)
            }
        }
    }
}

private fun withTempBuildEnvironment(code: (tempDir: File, settingsFile: File, buildFile: File) -> Unit) {
    withTempDir { tempDir ->
        val settingsFile = File(tempDir, "settings.gradle.kts")
        settingsFile.createNewFile() || error("Can not create file: $settingsFile")
        val buildFile = File(tempDir, "build.gradle.kts")
        buildFile.createNewFile() || error("Can not create file: $buildFile")
        code(tempDir, settingsFile, buildFile)
    }
}

private fun withTempDir(tempDirPrefix: String = "uspek", code: (tempDir: File) -> Unit) {
    lateinit var tempDir: File
    try {
        tempDir = File.createTempFile(tempDirPrefix, null).apply {
            delete() || error("Can not delete temp file: $this")
            mkdir() || error("Can not create dir: $this")
        }
        code(tempDir)
    } finally {
        tempDir.deleteRecursively() || error("Can not delete recursively dir: $tempDir")
    }
}

private fun onSingleHelloWorld(tempDir: File, settingsFile: File, buildFile: File) {
    "On single hello world project" o {
        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        "On build file with helloWorld task" o {
            buildFile.writeText("""
                tasks.register("helloWorld") {
                    doLast {
                        println("Hello world!")
                    }
                }
            """.trimIndent())

            "On gradle runner with temp dir" o {
                val runner = GradleRunner.create().withProjectDir(tempDir)

                "On task helloWorld" o {
                    runner.withArguments("helloWorld")

                    "On gradle build" o {
                        val result = runner.build()

                        "task helloWorld ends successfully" o { result.task(":helloWorld")?.outcome eq SUCCESS }
                        "output contains hello world message" o { result.output.contains("Hello world!") eq true }
                    }
                }

                "On non existing task" o {
                    runner.withArguments("blabla")

                    "gradle fails" o { runner.buildAndFail() }
                }
            }
        }
    }
}

