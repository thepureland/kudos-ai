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
     * 维度
     */
    val dimension: Int

}