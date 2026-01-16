package io.kudos.ai.ability.model.common.support.enums.ienum

/**
 * embedding模型接口
 *
 * @author K
 * @since 1.0.0
 */
interface IEmbeddingModel : IAIModel {

    /**
     * 维度
     */
    val dimension: Int

}