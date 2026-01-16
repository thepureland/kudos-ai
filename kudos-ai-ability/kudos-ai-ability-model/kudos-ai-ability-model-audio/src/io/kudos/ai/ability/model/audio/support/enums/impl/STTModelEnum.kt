package io.kudos.ai.ability.model.audio.support.enums.impl

import io.kudos.ai.ability.model.audio.support.enums.ienums.ISTTModelEnum

/**
 * STT (Speech-to-Text) 语音转文本模型枚举
 *
 * @author K
 * @author AI:cursor
 * @since 1.0.0
 */
enum class STTModelEnum(
    override val modelName: String,
    override val parameters: Float,
    override val contextSize: Float,
    override val size: Float,
    override val maxSilenceMisrecognitionLength: Int,
    override val provider: String
) : ISTTModelEnum {

    /**
     * faster-whisper Tiny 模型
     * 快速推理，准确度有限，适合测试和开发
     * 处理 30 秒音频片段，约 300-500 token 输出
     */
    FASTER_WHISPER_TINY("Systran/faster-whisper-tiny", 0.039F, 0.5F, 0.075F, 10, "Systran"),
    
    /**
     * faster-whisper Base 模型
     * 速度和准确度的平衡，适合一般用途
     * 处理 30 秒音频片段，约 300-500 token 输出
     */
    FASTER_WHISPER_BASE("Systran/faster-whisper-base", 0.074F, 0.5F, 0.145F, 10, "Systran"),
    
    /**
     * faster-whisper Small 模型
     * 良好的准确度，中等速度，适合生产环境
     * 处理 30 秒音频片段，约 300-500 token 输出
     */
    FASTER_WHISPER_SMALL("Systran/faster-whisper-small", 0.244F, 0.5F, 0.486F, 10, "Systran"),
    
    /**
     * faster-whisper Medium 模型
     * 高准确度，适合对准确度要求较高的场景
     * 处理 30 秒音频片段，约 300-500 token 输出
     */
    FASTER_WHISPER_MEDIUM("Systran/faster-whisper-medium", 0.769F, 0.5F, 1.5F, 30, "Systran"),
    
    /**
     * faster-whisper Large v3 模型（默认）
     * 最佳准确度，适合对准确度要求极高的场景
     * 处理 30 秒音频片段，约 300-500 token 输出
     */
    FASTER_WHISPER_LARGE_V3("Systran/faster-whisper-large-v3", 1.55F, 0.5F, 3.0F, 50, "Systran"),

}
