package io.kudos.ai.ability.model.image.support.enums.impl

import io.kudos.ai.ability.model.image.support.enums.ienums.IImageChatModelEnum

/**
 * 图片聊天模型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class ImageChatModelEnum(
    override val modelName: String,
    override val parameters: Float,
    override val contextSize: Float,
    override val size: Float,
    override val provider: String
) : IImageChatModelEnum {

    // llava
    LLAVA_7B("llava:7b", 7F, 32F, 4.7F, "University of Wisconsin–Madison、Microsoft Research、Columbia University"),

    // llama
    LLAMA_3_2_VISION_11B("llama3.2-vision:11b", 11F, 128F, 7.8F, "Meta"),

    // gemma
    GEMMA3_4B("gemma3:4b", 4F, 128F, 3.3F, "Google"),
    GEMMA3_12B("gemma3:12b", 12F, 128F, 8.1F, "Google"),

}