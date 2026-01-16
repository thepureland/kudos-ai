// The settings
// is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins, dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

dependencyResolutionManagement {
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "kudos-ai"


// test
include("kudos-ai-test:kudos-ai-test-container")

// tools
include("kudos-ai-tools")

// data
include("kudos-ai-ability:kudos-ai-ability-data:kudos-ai-ability-data-vdb:kudos-ai-ability-data-vdb-common")
include("kudos-ai-ability:kudos-ai-ability-data:kudos-ai-ability-data-vdb:kudos-ai-ability-data-vdb-pgvector")
include("kudos-ai-ability:kudos-ai-ability-data:kudos-ai-ability-data-vdb:kudos-ai-ability-data-vdb-milvus")


include("kudos-ai-ability:kudos-ai-ability-agent:kudos-ai-ability-agent-embabel")

// model
include("kudos-ai-ability:kudos-ai-ability-model:kudos-ai-ability-model-common")
include("kudos-ai-ability:kudos-ai-ability-model:kudos-ai-ability-model-text")
include("kudos-ai-ability:kudos-ai-ability-model:kudos-ai-ability-model-image")
include("kudos-ai-ability:kudos-ai-ability-model:kudos-ai-ability-model-audio")


