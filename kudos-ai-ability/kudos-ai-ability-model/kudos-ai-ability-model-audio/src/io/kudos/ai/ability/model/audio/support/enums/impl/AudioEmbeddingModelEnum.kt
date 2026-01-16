package io.kudos.ai.ability.model.audio.support.enums.impl

import io.kudos.ai.ability.model.common.support.enums.ienum.IEmbeddingModel

/**
 * 語音embedding模型枚舉
 *
 * @author K
 * @author AI: cursor
 * @since 1.0.0
 */
enum class AudioEmbeddingModelEnum(
    override val modelName: String,
    override val dimension: Int,
    override val parameters: Float,
    override val contextSize: Float,
    override val size: Float,
    override val provider: String
) : IEmbeddingModel {

    /**
     * WeSpeaker VoxCeleb ResNet34 LM 模型
     * 基于 ResNet34 架构的说话人嵌入模型，使用 Large-Margin 技术微调
     * 用于说话人识别、验证和相似度计算
     */
    WESPEAKER_VOXCELEB_RESNET34_LM("Wespeaker/wespeaker-voxceleb-resnet34-LM", 512, 0.1F, 1.0F, 0.2F, "Wespeaker"),

    /**
     * fedirz segmentation community 模型
     * 社区提供的语音分割和嵌入模型
     */
    FEDIRZ_SEGMENTATION_COMMUNITY_1("fedirz/segmentation_community_1", 256, 0.05F, 1.0F, 0.1F, "fedirz"),

}