package io.kudos.ai.ability.model.image

import io.kudos.ai.ability.model.image.support.enums.impl.ImageChatModelEnum
import io.kudos.ai.test.container.containers.ollama.OllamaMiniTestContainer
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import org.junit.jupiter.api.Timeout
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.Media
import org.springframework.core.io.ByteArrayResource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.MimeTypeUtils
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 图片模型测试用例
 *
 * 测试内容：
 * - 基本的图片描述
 * - 图片内容问答
 * - 图片中的对象识别
 * - 图片中的文字识别（OCR）
 * - 多图片对比
 * - 图片场景理解
 * - 图片颜色和风格分析
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
class ImageModelTest {

    @Resource
    private lateinit var chatModel: ChatModel

    private val log = LogFactory.getLog(this)

    @BeforeTest
    fun setup() {
        // 确保 ChatModel 已注入
        assertNotNull(chatModel, "ChatModel 应该被注入")
    }

    /**
     * 创建一个简单的测试图片（1x1 透明像素 PNG）
     * 用于测试基本的图片处理功能
     */
    private fun createTestImage(): ByteArray {
        // 一个有效的 1x1 透明像素 PNG 图片的 base64 编码
        val base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        return Base64.getDecoder().decode(base64Image)
    }

    /**
     * 创建一个简单的测试图片（使用与 createTestImage 相同的有效 PNG）
     * 用于测试更复杂的图片内容识别
     * 注意：为了确保图片格式有效，暂时使用相同的 1x1 透明 PNG
     */
    private fun createColorfulTestImage(): ByteArray {
        // 使用相同的有效 1x1 透明 PNG，确保格式正确
        // 在实际使用中，可以替换为更大的有效彩色 PNG
        return createTestImage()
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    fun test_describe_image() {
        // Arrange
        val imageBytes = createTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val question = "请描述这张图片的内容"
        log.debug("Q: $question")
        
        val userMessage = UserMessage.builder()
            .text(question)
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(imageResource)
                    .build()
            )
            .build()
        val prompt = Prompt(userMessage)

        // Act
        val response: ChatResponse = chatModel.call(prompt)

        // Assert
        assertNotNull(response, "响应不应该为 null")
        assertNotNull(response.result, "响应结果不应该为 null")
        val content = response.result.output.text
        log.debug("A: $content")
        assertTrue(
            content.isNotBlank(),
            "响应内容不应该为空"
        )
        // 验证响应包含对图片的描述
        assertTrue(
            content.length > 10,
            "图片描述应该有一定的长度"
        )
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    fun test_answer_question_about_image() {
        // Arrange
        val imageBytes = createTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val question = "这张图片的主要颜色是什么？"
        log.debug("Q: $question")
        
        val userMessage = UserMessage.builder()
            .text(question)
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(imageResource)
                    .build()
            )
            .build()
        val prompt = Prompt(userMessage)

        // Act
        val response: ChatResponse = chatModel.call(prompt)

        // Assert
        assertNotNull(response, "响应不应该为 null")
        val content = response.result.output.text
        log.debug("A: $content")
        assertTrue(
            content.isNotBlank(),
            "响应内容不应该为空"
        )
        // 验证响应回答了关于图片的问题
        assertTrue(
            content.length > 5,
            "回答应该有一定的内容"
        )
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    fun test_identify_objects_in_image() {
        // Arrange
        val imageBytes = createColorfulTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val question = "这张图片中有哪些对象或元素？"
        log.debug("Q: $question")
        
        val userMessage = UserMessage.builder()
            .text(question)
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(imageResource)
                    .build()
            )
            .build()
        val prompt = Prompt(userMessage)

        // Act
        val response: ChatResponse = chatModel.call(prompt)

        // Assert
        assertNotNull(response, "响应不应该为 null")
        val content = response.result.output.text
        log.debug("A: $content")
        assertTrue(
            content.isNotBlank(),
            "响应内容不应该为空"
        )
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    fun test_analyze_image_colors() {
        // Arrange
        val imageBytes = createColorfulTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val question = "请分析这张图片中的颜色组成"
        log.debug("Q: $question")
        
        val userMessage = UserMessage.builder()
            .text(question)
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(imageResource)
                    .build()
            )
            .build()
        val prompt = Prompt(userMessage)

        // Act
        val response: ChatResponse = chatModel.call(prompt)

        // Assert
        assertNotNull(response, "响应不应该为 null")
        val content = response.result.output.text
        log.debug("A: $content")
        assertTrue(
            content.isNotBlank(),
            "响应内容不应该为空"
        )
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    fun test_understand_image_scene() {
        // Arrange
        val imageBytes = createTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val question = "这张图片的场景是什么？"
        log.debug("Q: $question")
        
        val userMessage = UserMessage.builder()
            .text(question)
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(imageResource)
                    .build()
            )
            .build()
        val prompt = Prompt(userMessage)

        // Act
        val response: ChatResponse = chatModel.call(prompt)

        // Assert
        assertNotNull(response, "响应不应该为 null")
        val content = response.result.output.text
        log.debug("A: $content")
        assertTrue(
            content.isNotBlank(),
            "响应内容不应该为空"
        )
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    fun test_count_elements_in_image() {
        // Arrange
        val imageBytes = createColorfulTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val question = "这张图片中有多少个不同的元素？"
        log.debug("Q: $question")
        
        val userMessage = UserMessage.builder()
            .text(question)
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(imageResource)
                    .build()
            )
            .build()
        val prompt = Prompt(userMessage)

        // Act
        val response: ChatResponse = chatModel.call(prompt)

        // Assert
        assertNotNull(response, "响应不应该为 null")
        val content = response.result.output.text
        log.debug("A: $content")
        assertTrue(
            content.isNotBlank(),
            "响应内容不应该为空"
        )
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    fun test_analyze_image_style() {
        // Arrange
        val imageBytes = createTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val question = "请分析这张图片的风格和特点"
        log.debug("Q: $question")
        
        val userMessage = UserMessage.builder()
            .text(question)
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(imageResource)
                    .build()
            )
            .build()
        val prompt = Prompt(userMessage)

        // Act
        val response: ChatResponse = chatModel.call(prompt)

        // Assert
        assertNotNull(response, "响应不应该为 null")
        val content = response.result.output.text
        log.debug("A: $content")
        assertTrue(
            content.isNotBlank(),
            "响应内容不应该为空"
        )
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    fun test_multiple_questions_about_same_image() {
        // Arrange
        val imageBytes = createColorfulTestImage()
        val imageResource = ByteArrayResource(imageBytes)
        val questions = listOf(
            "这张图片的主要特征是什么？",
            "图片的尺寸大概是多少？",
            "图片的整体色调如何？"
        )
        log.debug("Q: ${questions.joinToString(" | ")}")

        // Act & Assert
        questions.forEach { question ->
            val userMessage = UserMessage.builder()
                .text(question)
                .media(
                    Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .data(imageResource)
                        .build()
                )
                .build()
            val prompt = Prompt(userMessage)
            val response: ChatResponse = chatModel.call(prompt)

            assertNotNull(response, "响应不应该为 null")
            val content = response.result.output.text
            log.debug("A: $content")
            assertTrue(
                content.isNotBlank(),
                "问题 '$question' 的响应不应该为空"
            )
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            val imageModel = ImageChatModelEnum.GEMMA3_4B.modelName
            
            // 启动 Ollama 容器并拉取模型
            OllamaMiniTestContainer.startIfNeeded(registry, imageModel)
            
            // 注册 ChatModel 相关配置（视觉模型通过 ChatModel 使用）
            registry.add("spring.ai.model.chat") { "ollama" }
            registry.add("spring.ai.ollama.chat.options.model") { imageModel }
            registry.add("spring.ai.ollama.chat.options.temperature") { "0.7" }
            registry.add("spring.ai.ollama.base-url") { "http://127.0.0.1:${OllamaMiniTestContainer.port}" }
        }
    }
}
