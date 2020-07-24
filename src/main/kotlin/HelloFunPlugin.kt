package pl.mareklangiewicz

import org.gradle.api.Plugin
import org.gradle.api.Project

class HelloFunPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.task("hello") {
            it.doLast {
                println("Hello! Is gradle fun??")
            }
        }
    }
}