package io.kudos.ai.ability.model.audio.support.enums.ienums

import io.kudos.ai.ability.model.audio.support.enums.impl.AudioModelTypeEnum
import io.kudos.ai.ability.model.common.support.enums.ienum.IAIModel

/**
 * TTS (Text-to-Speech) 文本转语音模型枚举接口
 *
 * @author K
 * @author AI:cursor
 * @since 1.0.0
 */
interface ITTSModelEnum : IAIModel {

    /** 语音模型类型 */
    val audioModelType: AudioModelTypeEnum
        get() = AudioModelTypeEnum.TTS

}
