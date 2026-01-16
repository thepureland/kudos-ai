package io.kudos.ai.ability.model.common.support.enums.ienum

/**
 * AI模型接口
 *
 * @author K
 * @since 1.0.0
 */
interface IAIModel {

    /**
     * 模型名称
     */
    val modelName: String

    /**
     * 参数数量，单位B
     */
    val parameters: Float

    /**
     * 上下文大小（token数）, 单位K
     */
    val contextSize: Float

    /**
     * 包尺寸(体积)，单位GB
     */
    val size: Float

    /**
     * 提供商
     */
    val provider: String

}