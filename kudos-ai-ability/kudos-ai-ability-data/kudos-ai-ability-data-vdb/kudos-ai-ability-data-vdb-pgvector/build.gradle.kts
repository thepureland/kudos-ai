dependencies {
    api(project(":kudos-ai-ability:kudos-ai-ability-data:kudos-ai-ability-data-vdb:kudos-ai-ability-data-vdb-common"))
    api(libs.spring.ai.starter.vector.store.pgvector)
    api(libs.postgresql)

    testImplementation(libs.spring.ai.starter.model.ollama)
    testImplementation(project(":kudos-ai-test:kudos-ai-test-container"))
}