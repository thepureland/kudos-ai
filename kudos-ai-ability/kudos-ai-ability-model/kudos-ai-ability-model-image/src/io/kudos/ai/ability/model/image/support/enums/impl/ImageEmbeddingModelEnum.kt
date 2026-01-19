package io.kudos.ai.ability.model.image.support.enums.impl

import io.kudos.ai.ability.model.image.support.enums.ienums.IImageEmbeddingModelEnum

/**
 * 圖片embedding模型枚舉
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
enum class ImageEmbeddingModelEnum(
    override val modelName: String,
    override val dimension: Int,
    override val parameters: Float,
    override val contextSize: Float,
    override val size: Float,
    override val provider: String
) : IImageEmbeddingModelEnum {

    /**
     * SigLIP2 Base Patch16 224 模型
     * Google 的多语言图像-文本共同嵌入模型
     * 支持零样本图像分类、图像-文本检索等任务
     * 输入图像尺寸：224x224
     */
    SIGLIP2_BASE_PATCH16_224("google/siglip2-base-patch16-224", 768, 0.086F, 0.224F, 0.33F, "Google"),

    /**
     * SigLIP2 Base Patch16 256 模型
     * Google 的多语言图像-文本共同嵌入模型
     * 支持零样本图像分类、图像-文本检索等任务
     * 输入图像尺寸：256x256，相比 224 版本有更好的细节保留
     */
    SIGLIP2_BASE_PATCH16_256("google/siglip2-base-patch16-256", 768, 0.086F, 0.256F, 0.33F, "Google"),

}