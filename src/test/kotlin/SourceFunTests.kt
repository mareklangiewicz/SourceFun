package pl.mareklangiewicz.sourcefun

import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// FIXME: why this is not running??
class SourceFunTests {

    @Test fun sourceFunTest1() {
        val project = ProjectBuilder.builder().build()!!
        project.pluginManager.apply("pl.mareklangiewicz.sourcefun")

        assertTrue(project.tasks.getByName("sourceFun") is Task)
    }
}