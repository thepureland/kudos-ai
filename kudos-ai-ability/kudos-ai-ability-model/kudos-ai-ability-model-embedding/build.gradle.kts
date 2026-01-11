dependencies {
    api(project(":kudos-ai-ability:kudos-ai-ability-model:kudos-ai-ability-model-common"))
    api(libs.spring.ai.model)

    testImplementation(project(":kudos-ai-test:kudos-ai-test-container"))
    testImplementation(libs.spring.ai.starter.model.ollama)
}
