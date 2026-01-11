dependencies {
    api(project(":kudos-ai-ability:kudos-ai-ability-model:kudos-ai-ability-model-embedding"))
    api(libs.kudos.context)
    api(libs.spring.ai.vector.store)

    testImplementation(libs.kudos.test.common)
}