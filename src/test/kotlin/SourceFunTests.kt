package pl.mareklangiewicz.sourcefun

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pl.mareklangiewicz.SourceFunTask
import java.io.File
import kotlin.test.assertEquals

class SourceFunTests {

    @Rule @JvmField
    val testProjectDir: TemporaryFolder = TemporaryFolder()

    private lateinit var settingsFile: File
    private lateinit var buildFile: File

    @Before
    fun setup() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts")
        buildFile = testProjectDir.newFile("build.gradle.kts")
    }

    @Test
    fun testHelloWorldTask() {

        settingsFile.writeText("""
            rootProject.name = "hello-world"
        """.trimIndent())

        buildFile.writeText("""
            tasks.register("helloWorld") {
                doLast {
                    println("Hello world!")
                }
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("helloWorld")
            .build()

        assertTrue(result.output.contains("Hello world!"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":helloWorld")?.outcome)
    }

    @Test
    fun sourceFunTest1() {
        val project = ProjectBuilder.builder().build()!!
        project.pluginManager.apply("pl.mareklangiewicz.sourcefun")

        assertTrue(project.tasks.getByName("sourceFun") is SourceFunTask)
    }
}