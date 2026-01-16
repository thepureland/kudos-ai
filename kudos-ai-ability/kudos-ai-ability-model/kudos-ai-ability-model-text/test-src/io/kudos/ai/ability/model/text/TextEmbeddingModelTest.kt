package io.kudos.ai.ability.model.text

import io.kudos.ai.ability.model.text.support.enums.impl.TextEmbeddingModelEnum
import io.kudos.ai.test.container.containers.ollama.OllamaMiniTestContainer
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 文本Embedding模型测试用例
 *
 * 测试内容：
 * - 生成单个文本的 embedding
 * - 生成多个文本的 embeddings（批量）
 * - 验证 embedding 的维度
 * - 验证 embedding 的格式（向量）
 * - 处理空文本
 * - 处理不同长度的文本
 * - 验证相同文本的 embedding 一致性
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
class TextEmbeddingModelTest {

    @Resource
    private lateinit var embeddingModel: EmbeddingModel

    private val log = LogFactory.getLog(this)

    @BeforeTest
    fun setup() {
        // 确保 EmbeddingModel 已注入
        assertNotNull(embeddingModel, "EmbeddingModel 应该被注入")
    }

    @Test
    fun test_generate_single_embedding() {
        // Arrange
        val text = "Spring AI 是一个强大的 AI 应用开发框架"
        log.debug("Text: $text")

        // Act
        val embeddingVector = embeddingModel.embed(text)

        // Assert
        assertNotNull(embeddingVector, "embedding 向量不应该为 null")
        assertTrue(embeddingVector.isNotEmpty(), "embedding 向量不应该为空")

        val dimensions = embeddingModel.dimensions()
        assertEquals(dimensions, embeddingVector.size, "embedding 维度应该与模型维度一致")

        log.debug("Embedding dimensions: ${embeddingVector.size}")
        log.debug("First 5 values: ${embeddingVector.take(5).joinToString()}")
    }

    @Test
    fun test_generate_multiple_embeddings() {
        // Arrange
        val texts = listOf(
            "Kotlin 是一种现代编程语言",
            "Spring Boot 简化了 Java 应用开发",
            "向量数据库用于存储和检索嵌入向量"
        )
        log.debug("Texts: ${texts.joinToString(" | ")}")

        // Act
        val embeddings = embeddingModel.embed(texts)

        // Assert
        assertNotNull(embeddings, "embeddings 不应该为 null")
        assertEquals(texts.size, embeddings.size, "应该返回与输入文本数量相同的 embeddings")

        val dimensions = embeddingModel.dimensions()
        embeddings.forEachIndexed { index, embeddingVector ->
            assertNotNull(embeddingVector, "embedding[$index] 不应该为 null")
            assertTrue(embeddingVector.isNotEmpty(), "embedding[$index] 向量不应该为空")
            assertEquals(dimensions, embeddingVector.size, "embedding[$index] 维度应该与模型维度一致")
        }

        log.debug("Generated ${embeddings.size} embeddings, each with $dimensions dimensions")
    }

    @Test
    fun test_verify_embedding_dimensions() {
        // Arrange
        val text = "测试 embedding 维度"
        val expectedDimensions = TextEmbeddingModelEnum.ALL_MINILM.dimension

        // Act
        val embeddingVector = embeddingModel.embed(text)
        val actualDimensions = embeddingModel.dimensions()

        // Assert
        assertEquals(expectedDimensions, actualDimensions, "模型维度应该与枚举定义一致")
        assertEquals(expectedDimensions, embeddingVector.size, "embedding 维度应该正确")

        log.debug("Expected dimensions: $expectedDimensions, Actual dimensions: $actualDimensions")
    }

    @Test
    fun test_embedding_vector_format() {
        // Arrange
        val text = "验证 embedding 向量格式"

        // Act
        val embeddingVector = embeddingModel.embed(text)

        // Assert
        assertTrue(embeddingVector.isNotEmpty(), "embedding 向量不应该为空")

        // 验证向量中的值都是数字（Float 类型）
        embeddingVector.forEachIndexed { index: Int, value: Float ->
            assertTrue(
                value.isFinite(),
                "embedding[$index] 应该是有效的浮点数，实际值: $value"
            )
        }

        // 验证向量不是全零
        val hasNonZero = embeddingVector.any { it != 0.0f }
        assertTrue(hasNonZero, "embedding 向量不应该全为零")

        log.debug("Embedding vector format verified: ${embeddingVector.size} dimensions, non-zero values present")
    }

    @Test
    fun test_handle_empty_text() {
        // Arrange
        val text = ""

        // Act
        val embeddingVector = embeddingModel.embed(text)

        // Assert
        assertNotNull(embeddingVector, "即使输入为空，也应该返回响应")
        val dimensions = embeddingModel.dimensions()
        assertEquals(dimensions, embeddingVector.size, "空文本的 embedding 也应该有正确的维度")

        log.debug("Empty text embedding generated with ${embeddingVector.size} dimensions")
    }

    @Test
    fun test_handle_different_text_lengths() {
        // Arrange
        val texts = listOf(
            "短",  // 单个字符
            "这是一个中等长度的文本，用于测试不同长度的输入",  // 中等长度
            "这是一个非常长的文本，用于测试 embedding 模型处理长文本的能力。" +
                    "它包含多个句子和段落，用于验证模型是否能够正确处理不同长度的输入。" +
                    "长文本通常包含更多的语义信息，embedding 应该能够捕获这些信息。" +
                    "我们期望模型能够为不同长度的文本生成有意义的向量表示。"  // 长文本
        )
        log.debug("Testing texts with lengths: ${texts.map { it.length }}")

        // Act
        val embeddings = embeddingModel.embed(texts)

        // Assert
        assertEquals(texts.size, embeddings.size, "应该返回与输入文本数量相同的 embeddings")

        val dimensions = embeddingModel.dimensions()
        embeddings.forEachIndexed { index, embeddingVector ->
            assertEquals(dimensions, embeddingVector.size, "不同长度的文本应该生成相同维度的 embedding")
            assertTrue(embeddingVector.isNotEmpty(), "embedding[$index] 不应该为空")
        }

        log.debug("All embeddings have consistent dimensions: $dimensions")
    }

    @Test
    fun test_embedding_consistency() {
        // Arrange
        val text = "测试 embedding 一致性"
        log.debug("Text: $text")

        // Act - 生成两次相同的 embedding
        val embedding1 = embeddingModel.embed(text)
        val embedding2 = embeddingModel.embed(text)

        // Assert
        assertEquals(embedding1.size, embedding2.size, "两次生成的 embedding 应该有相同的维度")

        // 验证向量值是否一致（允许小的浮点误差）
        var differences = 0
        embedding1.forEachIndexed { index: Int, value1: Float ->
            val value2 = embedding2[index]
            if (abs(value1 - value2) > 0.0001f) {
                differences++
            }
        }

        // 对于确定性模型，embedding 应该完全一致
        // 对于非确定性模型，可能会有小的差异，但大部分值应该相同
        assertTrue(
            differences < embedding1.size * 0.1,
            "相同文本的 embedding 应该基本一致，但发现 $differences 个不同的值（共 ${embedding1.size} 个）"
        )

        log.debug("Embedding consistency verified: $differences differences out of ${embedding1.size} values")
    }

    @Test
    fun test_embedding_similarity() {
        // Arrange
        val similarTexts = listOf(
            "Spring AI 是一个 AI 框架",
            "Spring AI 是用于 AI 应用开发的框架",
            "Spring AI 框架用于构建 AI 应用"
        )

        val differentText = "Python 是一种编程语言"

        // Act
        val similarEmbeddings = embeddingModel.embed(similarTexts)
        val differentEmbedding = embeddingModel.embed(differentText)

        // 计算相似文本之间的余弦相似度
        fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
            require(vec1.size == vec2.size) { "向量维度必须相同" }
            val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
            val norm1 = sqrt(vec1.sumOf { (it * it).toDouble() }).toFloat()
            val norm2 = sqrt(vec2.sumOf { (it * it).toDouble() }).toFloat()
            return if (norm1 == 0f || norm2 == 0f) 0f else dotProduct / (norm1 * norm2)
        }

        // 相似文本之间的相似度应该较高
        val similarityBetweenSimilar = cosineSimilarity(similarEmbeddings[0], similarEmbeddings[1])
        assertTrue(
            similarityBetweenSimilar > 0.7f,
            "相似文本的 embedding 应该有较高的相似度，实际值: $similarityBetweenSimilar"
        )

        // 不同文本之间的相似度应该较低
        val similarityWithDifferent = cosineSimilarity(similarEmbeddings[0], differentEmbedding)
        assertTrue(
            similarityWithDifferent < similarityBetweenSimilar,
            "不同文本的 embedding 相似度应该低于相似文本，实际值: $similarityWithDifferent vs $similarityBetweenSimilar"
        )

        log.debug("Similarity between similar texts: $similarityBetweenSimilar")
        log.debug("Similarity with different text: $similarityWithDifferent")
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            // 使用 ALL_MINILM 模型进行测试（小、快，适合测试/开发）
            val embeddingModel = TextEmbeddingModelEnum.ALL_MINILM.modelName

            // 启动 Ollama 容器并拉取模型
            OllamaMiniTestContainer.startIfNeeded(registry, embeddingModel)

            // 注册 EmbeddingModel 相关配置
            registry.add("spring.ai.model.embedding") { "ollama" }
            registry.add("spring.ai.ollama.embedding.options.model") { embeddingModel }
            registry.add("spring.ai.ollama.base-url") { "http://127.0.0.1:${OllamaMiniTestContainer.port}" }
        }
    }
}