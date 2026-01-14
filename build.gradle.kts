import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val kudos_bom = libs.kudos.bom
val spring_boot_bom = libs.spring.boot.bom
val spring_cloud_bom = libs.spring.cloud.bom
val spring_ai_bom = libs.spring.ai.bom

subprojects {
    // 所有子模块都应用 Kotlin JVM 插件
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // Kotlin 源码目录
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        sourceSets {
            getByName("main").kotlin.srcDirs("src")
            getByName("test").kotlin.srcDirs("test-src")
        }
    }

    // 资源目录
    the<JavaPluginExtension>().sourceSets {
        getByName("main").resources.srcDir("resources")
        getByName("test").resources.srcDir("test-resources")
    }

    // 每个子模块的 build 目录挪到根项目下统一管理
    layout.buildDirectory = File(rootProject.projectDir, "build/${project.name}")

    dependencies {
        add("implementation", platform(kudos_bom))
        add("implementation", platform(spring_boot_bom))
        add("implementation", platform(spring_cloud_bom))
        add("implementation", platform(spring_ai_bom))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            // 显示标准输出和错误流（实时输出，不缓冲）
            showStandardStreams = true
            // 实时输出日志事件，包括标准输出和错误
            events(
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT,
                org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
            )
            // 显示异常信息
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStackTraces = true
            // 显示所有日志级别（包括 INFO 和 DEBUG）
            showCauses = true
            // 设置最小粒度，确保实时输出（0 = 输出所有事件）
            minGranularity = 0
        }
        // 确保测试输出不被缓冲
        outputs.upToDateWhen { false }
        // 禁用输出缓冲，实时显示日志
        systemProperty("java.util.logging.manager", "java.util.logging.LogManager")
    }
}

plugins {
    alias(libs.plugins.github.ben.manes)
}

// 判定是否为非稳定版
fun String.isNonStable(): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !stableKeyword && !matches(regex)
}

// 執行命令：./gradlew dependencyUpdates
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable() && !currentVersion.isNonStable()
    }
    checkForGradleUpdate = true
    outputFormatter = "json,plain"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}
