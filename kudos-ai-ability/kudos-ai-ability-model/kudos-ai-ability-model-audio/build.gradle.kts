dependencies {
    api(project(":kudos-ai-ability:kudos-ai-ability-model:kudos-ai-ability-model-common"))

    testImplementation(project(":kudos-ai-test:kudos-ai-test-container"))
    testImplementation(libs.spring.ai.starter.openai)
}
