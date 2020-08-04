package pl.mareklangiewicz

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register
import java.io.File

data class Def(
    val taskName: String,
    val sourceDir: String,
    val outputDir: String,
    val action: Action<FileTreeElement>
)

open class SourceFunExtension {

    val defs = mutableListOf<Def>()

    operator fun Def.unaryPlus() = defs.add(this)
}

class SourceFunPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        val extension = target.extensions.create("sourceFun", SourceFunExtension::class.java)

        target.afterEvaluate {// FIXME: is afterEvaluate appropriate here??
            for (def in extension.defs) target.tasks.register<SourceFunTask>(def.taskName) {
                source(def.sourceDir)
                outputDir = target.file(def.outputDir)
                action = { def.action.execute(this) }
            }
        }
    }
}

open class SourceFunTask : SourceTask() {

    @get:OutputDirectory
    var outputDir: File? = null

    // action should only write file(s) inside outputDir
    @Internal
    var action: FileVisitDetails.() -> Unit = {}

    @TaskAction
    fun execute() {
        source.visit { action() } // FIXME: do not use "visit" we want user to write explicit loops
            // TODO: define own MINIMAL (micro) multiplatform abstractions for files and file trees
    }
}


// FIXME: remove - it's a temporary task class for experiments
open class SourceRegexTask : SourceTask() {

    @get:OutputDirectory
    var outputDir: File? = null

    @get:Input
    var match: String? = null

    @get:Input
    var replace: String? = null

    @TaskAction
    fun execute() {
        println("TODO: SourceRegexTask.execute() source:$source; outputDir: $outputDir; match: $match; replace: $replace")
    }
}
