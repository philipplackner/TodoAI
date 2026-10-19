import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// LiteRT-LM ships its iOS runtime as a prebuilt dynamic framework with a C API. Kotlin/Native
// binds to it through cinterop, and the iOS app embeds the same framework.
val liteRtLmVersion = libs.versions.litertlm.get()
val liteRtLmFrameworksDir = rootProject.layout.projectDirectory.dir("iosApp/Frameworks")
val liteRtLmXcframework = liteRtLmFrameworksDir.dir("CLiteRTLM.xcframework")

val downloadLiteRtLmFramework by tasks.registering {
    val version = liteRtLmVersion
    val url = "https://github.com/google-ai-edge/LiteRT-LM/releases/download/v$version/CLiteRTLM.xcframework.zip"
    val frameworksDir = liteRtLmFrameworksDir.asFile
    val versionFile = liteRtLmFrameworksDir.file("CLiteRTLM.version").asFile
    inputs.property("version", version)
    outputs.dir(liteRtLmXcframework)
    outputs.file(versionFile)
    onlyIf { !versionFile.exists() || versionFile.readText() != version }
    doLast {
        frameworksDir.mkdirs()
        val zipFile = frameworksDir.resolve("CLiteRTLM.xcframework.zip")
        URI(url).toURL().openStream().use { input ->
            zipFile.outputStream().use { output -> input.copyTo(output) }
        }
        frameworksDir.resolve("CLiteRTLM.xcframework").deleteRecursively()
        val exitCode = ProcessBuilder("unzip", "-q", "-o", zipFile.absolutePath, "-d", frameworksDir.absolutePath)
            .inheritIO()
            .start()
            .waitFor()
        check(exitCode == 0) { "Unzipping $zipFile failed" }
        zipFile.delete()
        versionFile.writeText(version)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64() to "ios-arm64",
        iosSimulatorArm64() to "ios-arm64-simulator"
    ).forEach { (iosTarget, sliceName) ->
        val frameworkSearchPath = liteRtLmXcframework.dir(sliceName).asFile.absolutePath
        iosTarget.compilations.getByName("main") {
            cinterops.create("litertlm") {
                definitionFile = project.file("src/nativeInterop/cinterop/litertlm.def")
                compilerOpts("-F$frameworkSearchPath")
            }
        }
        iosTarget.binaries.all {
            linkerOpts("-F$frameworkSearchPath", "-framework", "CLiteRTLM", "-rpath", frameworkSearchPath)
        }
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.plcoding.todoai.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.litertlm.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelSavedstate)
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.matching { it.name.startsWith("cinteropLitertlm") }.configureEach {
    dependsOn(downloadLiteRtLmFramework)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}