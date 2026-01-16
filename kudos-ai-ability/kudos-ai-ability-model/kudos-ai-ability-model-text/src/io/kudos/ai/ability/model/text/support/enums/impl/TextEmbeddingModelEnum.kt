package io.kudos.ai.ability.model.text.support.enums.impl

import io.kudos.ai.ability.model.common.support.enums.ienum.IEmbeddingModel

/**
 * 文本embedding模型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class TextEmbeddingModelEnum(
    override val modelName: String,
    override val dimension: Int,
    override val parameters: Float,
    override val contextSize: Float,
    override val size: Float,
    override val provider: String
) : IEmbeddingModel {

    ALL_MINILM("all-minilm:33m", 384, 0.033F, 0.512F, 0.067F, "SBERT"), // 文本向量嵌入模型 小、快 適合測試/開發

    NOMIC_EMBED_TEXT("nomic-embed-text:v1.5", 768, 0.137F, 2F, 0.274F, "Nomic AI"), // 文本向量嵌入模型 高质量、通用 適合生产/RAG

}