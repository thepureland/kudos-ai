package io.kudos.ai.ability.model.text.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.Configuration

/**
 * 文本模型自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class TextModelAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-model-text"

}
