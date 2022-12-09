import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

plugins {
    kotlin("jvm") version "1.7.22"
}

group = "net.geekmc"

repositories {
    mavenCentral()
}

tasks.create("buildAndCopyExtensions") {
    fun copyShadowJarInDir(src: java.nio.file.Path, dest: java.nio.file.Path, overwrite: Boolean = true) {
        runCatching {
            Files.walk(src).forEach { a ->
                if (a.fileName.toString().endsWith("-all.jar")) {
                    val b = Paths.get(dest.toString() + a.toString().substring(src.toString().length))
                    if (a != src) {
                        if (overwrite) {
                            Files.copy(a, b, StandardCopyOption.REPLACE_EXISTING)
                        } else {
                            Files.copy(a, b)
                        }
                    }
                    println("Copy from $a to $b")
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    val extensionProjects = listOf(
        childProjects["turing-core"]!!, childProjects["athena"]!!
    )

    dependsOn(":turing-core:shadowJar", ":athena:shadowJar")

    doLast {
        val runDir = project.rootDir.toPath().toAbsolutePath().normalize().resolve("run")
        val extensionsDir = runDir.resolve("extensions")
        Files.createDirectories(extensionsDir)
        extensionProjects.map {
            val libsDir = it.buildDir.toPath().resolve("libs")
            copyShadowJarInDir(libsDir, extensionsDir)
        }
    }
}

tasks.create("runServer") {
    dependsOn(tasks.findByName("buildAndCopyExtensions"))

    doLast {
        val runDir = project.rootDir.toPath().toAbsolutePath().normalize().resolve("run")

        (childProjects["turing"]!!.tasks.getByName("run") as JavaExec).apply {
            workingDir = runDir.toFile()
            exec()
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}