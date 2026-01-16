package io.kudos.ai.ability.model.audio.support.enums.ienums

import io.kudos.ai.ability.model.audio.support.enums.impl.AudioModelTypeEnum
import io.kudos.ai.ability.model.common.support.enums.ienum.IAIModel

/**
 * STT (Speech-to-Text) 语音转文本模型枚举接口
 *
 * @author K
 * @author AI:cursor
 * @since 1.0.0
 */
interface ISTTModelEnum : IAIModel {

    /** 语音模型类型 */
    val audioModelType: AudioModelTypeEnum
        get() = AudioModelTypeEnum.STT

    /**
     * 静音音频最大允许误识别字符数
     * 
     * 不同模型对静音音频的处理能力不同：
     * - 小型模型（tiny, base, small）：通常产生较短的误识别（≤10字符）
     * - 中型模型（medium）：可能产生中等长度的误识别（≤30字符）
     * - 大型模型（large-v3）：可能产生较长的误识别（≤50字符）
     * 
     * 注意：大型模型由于模型复杂度更高，更容易对静音音频产生误识别，
     * 这是模型的局限性，不是 bug。
     */
    val maxSilenceMisrecognitionLength: Int

}
