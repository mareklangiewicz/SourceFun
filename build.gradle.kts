import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "com.github.langara.sourcefun"
version = "0.0.1"

gradlePlugin {
    plugins {
        create("sourceFunPlugin") {
            id = "pl.mareklangiewicz.sourcefun"
            implementationClass = "pl.mareklangiewicz.SourceFunPlugin"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    testImplementation(gradleTestKit())
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}