package io.kudos.ai.ability.model.embedding.support.enums.impl

import io.kudos.ai.ability.model.embedding.support.enums.ienums.IEmbeddingModelEnum


/**
 * embedding模型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class EmbeddingModelEnum(
    override val modelName: String,
    override val dimension: Int,
    override val parameters: Float,
    override val size: Float,
    override val provider: String
) : IEmbeddingModelEnum {

    ALL_MINILM("all-minilm:l6-v2", 384, 0.0227F, 0.09F, "SBERT"), // 文本向量嵌入模型 小、快 適合測試/開發

    NOMIC_EMBED_TEXT("nomic-embed-text", 768, 0.137F, 0.274F, "Nomic AI"), // 文本向量嵌入模型 高质量、通用 適合生产/RAG

}