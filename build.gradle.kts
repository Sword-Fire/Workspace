import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

plugins {
    kotlin("jvm") version "1.7.22"
}

repositories {
    mavenCentral()
}

group = "net.geekmc"

val subExtensions = listOf(
    "turing-core",
    "athena"
)

// 配置所有项目共用的工件版本。
// ext格式： version.工件名
allprojects {

    val dependencyToVersionMap = mutableMapOf<String, String>()

    fun addArtifact(name: String, version: String) {
        ext["version.$name"] = version
        dependencyToVersionMap[name] = version
    }

    addArtifact("kotlinx-coroutines-core", "1.6.4")
    addArtifact("kotlinx-serialization-core", "1.4.1")
    addArtifact("kotlinx-serialization-json", "1.4.1")
    addArtifact("kotlin-scripting-common", "1.7.22")
    addArtifact("kotlin-scripting-jvm", "1.7.22")
    addArtifact("kotlin-scripting-dependencies", "1.7.22")
    addArtifact("kotlin-scripting-dependencies-maven", "1.7.22")
    addArtifact("kotlin-scripting-jvm-host", "1.7.22")
    addArtifact("snakeyaml", "1.33")
    addArtifact("adventure-text-minimessage", "4.12.0")
    addArtifact("KStom", "f02e4c21d4")
    addArtifact("kaml", "0.49.0")
    addArtifact("log4j-core", "2.19.0")
    addArtifact("log4j-slf4j-impl", "2.19.0")
    addArtifact("clikt", "3.5.0")
    addArtifact("flyway-core", "9.10.2")
    addArtifact("sqlite-jdbc", "3.40.0.0")
    addArtifact("ktorm-core", "3.5.0")
    addArtifact("kodein-di-jvm", "7.17.0")

    ext["version.dependencyToVersionMap"] = dependencyToVersionMap

}

tasks.create("buildAndCopyExtensions") {

    fun copyJarInDir(src: java.nio.file.Path, dest: java.nio.file.Path, overwrite: Boolean = true) {
        runCatching {
            // 完全遍历源目录。
            Files.walk(src).forEach { file ->
                if (file.fileName.toString().endsWith(".jar")) {
                    // 阻止自拷贝。
                    if (file == src) {
                        error("Src and dest are the same: $src")
                    }
                    // 当前遍历文件在源目录中的路径所对应的在目标目录中的路径。
                    val destPath = Paths.get(dest.toString() + file.toString().substring(src.toString().length))
                    if (overwrite) {
                        Files.copy(file, destPath, StandardCopyOption.REPLACE_EXISTING)
                    } else {
                        Files.copy(file, destPath)
                    }
                    println("Copy ${file.fileName} to $destPath")
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    val extensionProjects = subExtensions.map { childProjects[it]!! }

    // 运行前先编译所有拓展。
    dependsOn(":turing-core:build", ":athena:build")

    doLast {
        val runDir = project.rootDir.toPath().toAbsolutePath().normalize().resolve("run")
        val extensionsDir = runDir.resolve("extensions")
        Files.createDirectories(extensionsDir)
        extensionProjects.map {
            val libsDir = it.buildDir.toPath().resolve("libs")
            copyJarInDir(libsDir, extensionsDir)
        }
    }
}

// runServer任务会将所有拓展的jar文件拷贝到run目录下，然后运行服务端。
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