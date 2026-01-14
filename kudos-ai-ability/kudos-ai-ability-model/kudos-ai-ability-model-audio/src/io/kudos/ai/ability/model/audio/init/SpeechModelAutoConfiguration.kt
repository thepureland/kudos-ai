package io.kudos.ai.ability.model.audio.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.Configuration

/**
 * 语音模型自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class SpeechModelAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-model-audio"

}