package io.kudos.ai.ability.model.audio.support.enums.impl

import io.kudos.ai.ability.model.audio.support.enums.ienums.ITTSModelEnum

/**
 * TTS (Text-to-Speech) 文本转语音模型枚举
 *
 * @author K
 * @author AI:cursor
 * @since 1.0.0
 */
enum class TTSModelEnum(
    override val modelName: String,
    override val parameters: Float,
    override val contextSize: Float,
    override val size: Float,
    override val provider: String
) : ITTSModelEnum {

    /**
     * Kokoro TTS 模型
     * 高质量语音合成，82M 参数，支持多种语言
     * 可处理较长文本输入（约 2K token）
     */
    KOKORO_82M("speaches-ai/Kokoro-82M-v1.0-ONNX", 0.082F, 2.0F, 0.163F, "speaches-ai"),

    // Speeches-ai不支持：
//    /**
//     * Kokoro TTS 中文优化模型
//     * 专门针对中文优化的版本，82M 参数
//     * 包含 100 种来自 LongMaoData 的中文声音，以及 3 种新的英文声音
//     * 可处理较长文本输入（约 2K token）
//     *
//     * 注意：此模型可能不在 speaches 的模型注册表中，使用时需要确认 speaches 是否支持
//     * 如果 speaches 不支持，可能需要使用其他 TTS 服务或直接使用 Hugging Face 模型
//     */
//    KOKORO_82M_V1_1_ZH("hexgrad/Kokoro-82M-v1.1-zh", 0.082F, 2.0F, 0.163F, "hexgrad"),
//
//    /**
//     * Kokoro TTS 中文优化模型（ONNX 版本）
//     * 专门针对中文优化的版本，82M 参数，ONNX 格式
//     * 包含 100 种来自 LongMaoData 的中文声音，以及 3 种新的英文声音
//     * 可处理较长文本输入（约 2K token）
//     * ONNX 格式通常具有更好的推理性能和跨平台兼容性
//     */
//    KOKORO_82M_V1_1_ZH_ONNX("onnx-community/Kokoro-82M-v1.1-zh-ONNX", 0.082F, 2.0F, 0.163F, "onnx-community"),
//
//    /**
//     * Piper TTS 模型
//     * 轻量级语音合成，适合快速部署
//     * 可处理中等长度文本输入（约 1K token）
//     */
//    PIPER_VOICES_50M("rhasspy/piper-voices", 0.05F, 1.0F, 0.072F, "Rhasspy"),

}
