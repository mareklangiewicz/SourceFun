package pl.mareklangiewicz.sourcefun

import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
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
            }
            else println(it.status)
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

        "On create temp dir" o {
            lateinit var dir: File
            try {
                dir = File.createTempFile("uspekSourceFun", null).apply {
                    delete() || error("Can not delete temp file: $this")
                    mkdir() || error("Can not create dir: $this")
                }

                "On dir" o {

                    "On gradle settings kts file" o {
                        val settingsFile = File(dir, "settings.gradle.kts")
                        settingsFile.createNewFile() || error("Can not create file: $settingsFile")

                        "On gradle build kts file" o {
                            val buildFile = File(dir, "build.gradle.kts")
                            buildFile.createNewFile() || error("Can not create file: $buildFile")

                            TODO("tests with $buildFile and $settingsFile")
                        }
                    }
                }
            }
            finally {
                dir.deleteRecursively() || error("Can not delete recursively dir: $dir")
            }

        }
    }
}

