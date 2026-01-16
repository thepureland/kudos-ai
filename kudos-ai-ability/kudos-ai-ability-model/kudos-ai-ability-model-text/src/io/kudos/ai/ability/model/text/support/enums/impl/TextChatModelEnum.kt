package io.kudos.ai.ability.model.text.support.enums.impl

import io.kudos.ai.ability.model.text.support.enums.ienums.ITextModelEnum

/**
 * 文本模型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class TextChatModelEnum(
    override val modelName: String,
    override val parameters: Float,
    override val contextSize: Float,
    override val size: Float,
    override val provider: String
) : ITextModelEnum {

    // Gemma
    GEMMA3_1B("gemma3:1b", 1F, 32F, 0.8F, "Google"),
    GEMMA3_12B("gemma3:12b", 12F, 128F, 8.1F, "Google"),

    // LLama
    LLAMA_3_2_1B("llama3.2:1b", 1F, 128F, 1.3F, "Meta"),
    LLAMA_3_2_3B("llama3.2", 3F, 128F, 2F, "Meta"),
    LLAMA_3_1_8B("llama3.1", 8F, 128F, 4.7F, "Meta"),

    // DeepSeek
    DEEPSEEK_R1_7B("deepseek-r1", 7F, 128F, 4.7F, "DeepSeek"),

}