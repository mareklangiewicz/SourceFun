package pl.mareklangiewicz

import org.gradle.api.Plugin
import org.gradle.api.Project


open class SourceFunPluginExtension {
    // FIXME: list of definitions: (input, output, transformation??) (transformation will be THE FUN)
    var input = "TODO input file/dir/list??"
    var output = "TODO output file/dir/list??"
}

class SourceFunPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        val extension = target.extensions.create<SourceFunPluginExtension>("sourceFun", SourceFunPluginExtension::class.java)

        // FIXME: remove this temporary task definition
        target.task("sourceFun") {
            it.doLast {
                println("TODO: tasks for sourceFun; input: ${extension.input}; output: ${extension.output}")
            }
        }
    }
}