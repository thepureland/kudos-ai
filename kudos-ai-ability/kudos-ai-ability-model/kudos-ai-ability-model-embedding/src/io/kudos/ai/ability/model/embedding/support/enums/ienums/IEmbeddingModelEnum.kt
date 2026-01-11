package io.kudos.ai.ability.model.embedding.support.enums.ienums

import io.kudos.ai.ability.model.common.IAIModel

/**
 * embedding模型枚举接口
 *
 * @author K
 * @since 1.0.0
 */
interface IEmbeddingModelEnum : IAIModel {

    /**
     * 参数数量，单位B
     */
    val parameters: Float

    /**
     * 维度
     */
    val dimension: Int

    /**
     * 包尺寸(体积)，单位GB
     */
    val size: Float

    /**
     * 提供商
     */
    val provider: String

}