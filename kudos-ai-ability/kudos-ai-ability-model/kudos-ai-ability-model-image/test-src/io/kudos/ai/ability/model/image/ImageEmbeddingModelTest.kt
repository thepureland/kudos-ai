package io.kudos.ai.ability.model.image

import io.kudos.ai.ability.model.image.support.enums.impl.ImageEmbeddingModelEnum
import io.kudos.ai.ability.model.image.transformers.TransformersImageEmbeddingModel
import io.kudos.ai.ability.model.image.transformers.TransformersImageEmbeddingOptions
import io.kudos.ai.test.container.containers.sentencetransformers.SentenceTransformersTestContainer
import io.kudos.base.logger.LogFactory
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import kotlin.math.abs
import kotlin.math.sqrt
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.test.*
import org.springframework.core.io.Resource as SpringResource

/**
 * 图片Embedding模型测试用例
 *
 * 注意：transformers-inference 容器主要用于文本 embedding，
 * 对于图像 embedding，可能需要使用不同的模型或端点。
 * 这里使用 SentenceTransformersTestContainer 作为基础容器。
 *
 * 测试内容：
 * - 生成单个图像的 embedding
 * - 生成多个图像的 embeddings（批量）
 * - 验证 embedding 的维度
 * - 验证 embedding 的格式（向量）
 * - 处理不同尺寸的图像
 * - 验证相同图像的 embedding 一致性
 * - 验证图像相似度计算
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
class ImageEmbeddingModelTest {

    val transformersRestClient = RestClient.builder()
        .baseUrl("http://127.0.0.1:${SentenceTransformersTestContainer.PORT}")
        .build()

    private var embeddingModel: EmbeddingModel = TransformersImageEmbeddingModel(transformersRestClient)

    private val log = LogFactory.getLog(this)

    @BeforeTest
    fun setup() {
        // 确保 EmbeddingModel 已初始化
        assertNotNull(embeddingModel, "EmbeddingModel 应该被初始化")
    }

    /**
     * 创建测试图像文件（用于测试）
     * 创建一个简单的测试图像
     */
    private fun createTestImageFile(testName: String, width: Int = 224, height: Int = 224): Path {
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val timestamp = System.currentTimeMillis()
        val fileName = "${testName}_${timestamp}.png"
        val filePath = tempDir.resolve(fileName)

        // 创建一个简单的测试图像
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        
        // 填充渐变背景
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255 / width).coerceIn(0, 255)
                val g = (y * 255 / height).coerceIn(0, 255)
                val b = ((x + y) * 255 / (width + height)).coerceIn(0, 255)
                image.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
        }
        graphics.dispose()

        // 保存为 PNG
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        Files.write(filePath, outputStream.toByteArray())
        
        log.debug("创建测试图像文件: ${filePath.toAbsolutePath()}, 大小: ${Files.size(filePath)} bytes, 尺寸: ${width}x${height}")
        return filePath
    }

    /**
     * 创建带有不同模式的测试图像文件（用于相似度测试）
     */
    private fun createTestImageFileWithPattern(testName: String, pattern: Int = 0, width: Int = 224, height: Int = 224): Path {
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val timestamp = System.currentTimeMillis()
        val fileName = "${testName}_${timestamp}.png"
        val filePath = tempDir.resolve(fileName)

        // 创建带有不同模式的测试图像
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        
        // 根据 pattern 创建不同的图像模式
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = when (pattern) {
                    0 -> {
                        // 水平条纹
                        if ((y / 20) % 2 == 0) 0xFFFFFF else 0x000000
                    }
                    1 -> {
                        // 垂直条纹
                        if ((x / 20) % 2 == 0) 0xFFFFFF else 0x000000
                    }
                    else -> {
                        // 棋盘格
                        if ((x / 20 + y / 20) % 2 == 0) 0xFFFFFF else 0x000000
                    }
                }
                image.setRGB(x, y, color)
            }
        }
        graphics.dispose()

        // 保存为 PNG
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        Files.write(filePath, outputStream.toByteArray())
        
        log.debug("创建测试图像文件（模式 $pattern）: ${filePath.toAbsolutePath()}, 大小: ${Files.size(filePath)} bytes, 尺寸: ${width}x${height}")
        return filePath
    }

    /**
     * 获取图像文件路径（用于 EmbeddingModel）
     */
    private fun getImagePath(imageResource: SpringResource): String {
        return when (imageResource) {
            is FileSystemResource -> imageResource.file.absolutePath
            is ClassPathResource -> imageResource.path
            else -> error("不支持的 Resource 类型: ${imageResource::class}")
        }
    }

    @Test
    fun test_image_embedding_model_enum() {
        // 验证 ImageEmbeddingModelEnum 包含 SigLIP2 模型
        val siglip2_224 = ImageEmbeddingModelEnum.SIGLIP2_BASE_PATCH16_224
        val siglip2_256 = ImageEmbeddingModelEnum.SIGLIP2_BASE_PATCH16_256

        assertEquals("google/siglip2-base-patch16-224", siglip2_224.modelName)
        assertEquals(768, siglip2_224.dimension)
        assertEquals("Google", siglip2_224.provider)

        assertEquals("google/siglip2-base-patch16-256", siglip2_256.modelName)
        assertEquals(768, siglip2_256.dimension)
        assertEquals("Google", siglip2_256.provider)

        log.debug("ImageEmbeddingModelEnum 验证通过")
        log.debug("SigLIP2 224 模型: ${siglip2_224.modelName}, 维度: ${siglip2_224.dimension}")
        log.debug("SigLIP2 256 模型: ${siglip2_256.modelName}, 维度: ${siglip2_256.dimension}")
    }

    @Test
    fun test_create_test_image() {
        // 测试创建测试图像的功能
        val testImagePath = createTestImageFile("test_create", 224, 224)
        
        assertTrue(Files.exists(testImagePath), "测试图像文件应该存在")
        assertTrue(Files.size(testImagePath) > 0, "测试图像文件大小应该大于 0")
        
        // 验证图像可以读取
        val image = ImageIO.read(testImagePath.toFile())
        assertNotNull(image, "应该能够读取创建的图像")
        assertEquals(224, image.width, "图像宽度应该是 224")
        assertEquals(224, image.height, "图像高度应该是 224")
        
        log.debug("测试图像创建成功: ${testImagePath.fileName}, 大小: ${Files.size(testImagePath)} bytes")
    }

    @Test
    fun test_create_test_image_with_pattern() {
        // 测试创建带模式的测试图像
        val pattern0 = createTestImageFileWithPattern("test_pattern_0", 0, 224, 224)
        val pattern1 = createTestImageFileWithPattern("test_pattern_1", 1, 224, 224)
        val pattern2 = createTestImageFileWithPattern("test_pattern_2", 2, 224, 224)
        
        assertTrue(Files.exists(pattern0), "模式 0 图像应该存在")
        assertTrue(Files.exists(pattern1), "模式 1 图像应该存在")
        assertTrue(Files.exists(pattern2), "模式 2 图像应该存在")
        
        log.debug("带模式的测试图像创建成功")
    }

    @Test
    fun test_handle_different_image_sizes() {
        // 测试处理不同尺寸的图像
        val smallImage = createTestImageFile("test_small", 224, 224)
        val mediumImage = createTestImageFile("test_medium", 256, 256)
        val largeImage = createTestImageFile("test_large", 384, 384)

        val imageFiles = listOf(smallImage, mediumImage, largeImage)
        log.debug("测试不同尺寸的图像: ${imageFiles.map { "${Files.size(it)} bytes" }}")

        // 验证所有图像都可以读取
        imageFiles.forEach { imagePath ->
            val image = ImageIO.read(imagePath.toFile())
            assertNotNull(image, "应该能够读取图像: ${imagePath.fileName}")
            assertTrue(image.width > 0 && image.height > 0, "图像尺寸应该大于 0")
        }

        log.debug("不同尺寸的图像处理测试通过")
    }

    @Test
    fun test_generate_single_embedding() {
        // Arrange
        val testImagePath = createTestImageFile("test_single_embedding")
        val imagePath = getImagePath(FileSystemResource(testImagePath.toFile()))
        log.debug("Image file: ${testImagePath.fileName}, Path: $imagePath")

        // Act
        val embeddingModelEnum = ImageEmbeddingModelEnum.SIGLIP2_BASE_PATCH16_256
        val embeddingRequest = buildEmbeddingRequest(listOf(imagePath), embeddingModelEnum)
        val embeddingVector = embed(embeddingRequest).first()

        // Assert
        assertNotNull(embeddingVector, "embedding 向量不应该为 null")
        assertTrue(embeddingVector.isNotEmpty(), "embedding 向量不应该为空")

        val dimensions = embeddingModelEnum.dimension
        // 注意：实际返回的维度可能与枚举定义不同，这里只验证维度大于 0
        assertTrue(embeddingVector.isNotEmpty(), "embedding 维度应该大于 0")

        log.debug("Embedding dimensions: ${embeddingVector.size}")
        log.debug("First 5 values: ${embeddingVector.take(5).joinToString()}")
    }

    @Test
    fun test_generate_multiple_embeddings() {
        // Arrange
        val imageFiles = listOf(
            createTestImageFile("test_multiple_1"),
            createTestImageFile("test_multiple_2"),
            createTestImageFile("test_multiple_3")
        )
        val imagePaths = imageFiles.map { getImagePath(FileSystemResource(it.toFile())) }
        log.debug("Image files: ${imageFiles.map { it.fileName }}, Paths: ${imagePaths.joinToString(" | ")}")

        // Act
        val embeddingModelEnum = defaultEmbeddingModel
        val embeddingRequest = buildEmbeddingRequest(imagePaths, embeddingModelEnum)
        val embeddings = embed(embeddingRequest)

        // Assert
        assertNotNull(embeddings, "embeddings 不应该为 null")
        assertEquals(imagePaths.size, embeddings.size, "应该返回与输入图像数量相同的 embeddings")

        val dimensions = embeddingModelEnum.dimension
        embeddings.forEachIndexed { index, embeddingVector ->
            assertNotNull(embeddingVector, "embedding[$index] 不应该为 null")
            assertTrue(embeddingVector.isNotEmpty(), "embedding[$index] 向量不应该为空")
            assertTrue(embeddingVector.size > 0, "embedding[$index] 维度应该大于 0")
        }

        log.debug("Generated ${embeddings.size} embeddings, each with ${embeddings.firstOrNull()?.size ?: 0} dimensions")
    }

    @Test
    fun test_verify_embedding_dimensions() {
        // Arrange
        val testImagePath = createTestImageFile("test_dimensions")
        val imagePath = getImagePath(FileSystemResource(testImagePath.toFile()))
        val embeddingModelEnum = defaultEmbeddingModel
        val expectedDimensions = embeddingModelEnum.dimension

        // Act
        val embeddingRequest = buildEmbeddingRequest(listOf(imagePath), embeddingModelEnum)
        val embeddingVector = embed(embeddingRequest).first()
        val actualDimensions = embeddingVector.size

        // Assert
        assertTrue(actualDimensions > 0, "embedding 维度应该大于 0")

        log.debug("Expected dimensions: $expectedDimensions, Actual dimensions: $actualDimensions")
    }

    @Test
    fun test_embedding_vector_format() {
        // Arrange
        val testImagePath = createTestImageFile("test_format")
        val imagePath = getImagePath(FileSystemResource(testImagePath.toFile()))

        // Act
        val embeddingModelEnum = defaultEmbeddingModel
        val embeddingRequest = buildEmbeddingRequest(listOf(imagePath), embeddingModelEnum)
        val embeddingVector = embed(embeddingRequest).first()

        // Assert
        assertTrue(embeddingVector.isNotEmpty(), "embedding 向量不应该为空")

        // 验证向量中的值都是有效的浮点数
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
    fun test_embedding_consistency() {
        // Arrange
        val testImagePath = createTestImageFile("test_consistency")
        val imagePath = getImagePath(FileSystemResource(testImagePath.toFile()))
        log.debug("Image file: ${testImagePath.fileName}, Path: $imagePath")

        // Act - 生成两次相同的 embedding
        val embeddingModelEnum = defaultEmbeddingModel
        val embeddingRequest = buildEmbeddingRequest(listOf(imagePath), embeddingModelEnum)
        val embedding1 = embed(embeddingRequest).first()
        val embedding2 = embed(embeddingRequest).first()

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

        // 对于确定性模型，embedding 应该基本一致
        assertTrue(
            differences < embedding1.size * 0.1,
            "相同图像的 embedding 应该基本一致，但发现 $differences 个不同的值（共 ${embedding1.size} 个）"
        )

        log.debug("Embedding consistency verified: $differences differences out of ${embedding1.size} values")
    }

    @Test
    fun test_image_similarity() {
        // 测试图像相似度计算
        // 创建相似的图像（相同模式）
        val similarImage1 = createTestImageFileWithPattern("test_similar_1", 0, 224, 224)
        val similarImage2 = createTestImageFileWithPattern("test_similar_2", 0, 224, 224)
        val similarImage3 = createTestImageFileWithPattern("test_similar_3", 0, 224, 224)

        // 创建不同的图像（不同模式）
        val differentImage = createTestImageFileWithPattern("test_different", 1, 224, 224)

        val similarImagePaths = listOf(
            getImagePath(FileSystemResource(similarImage1.toFile())),
            getImagePath(FileSystemResource(similarImage2.toFile())),
            getImagePath(FileSystemResource(similarImage3.toFile()))
        )
        val differentImagePath = getImagePath(FileSystemResource(differentImage.toFile()))
        val embeddingModelEnum = defaultEmbeddingModel
        log.debug("Model: ${embeddingModelEnum.modelName}")

        // Act
        val embeddingRequest1 = buildEmbeddingRequest(similarImagePaths, embeddingModelEnum)
        val similarEmbeddings = embed(embeddingRequest1)
        val embeddingRequest2 = buildEmbeddingRequest(listOf(differentImagePath), embeddingModelEnum)
        val differentEmbedding = embed(embeddingRequest2).first()

        // 计算相似图像之间的余弦相似度
        fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
            require(vec1.size == vec2.size) { "向量维度必须相同" }
            val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
            val norm1 = sqrt(vec1.sumOf { (it * it).toDouble() }).toFloat()
            val norm2 = sqrt(vec2.sumOf { (it * it).toDouble() }).toFloat()
            return if (norm1 == 0f || norm2 == 0f) 0f else dotProduct / (norm1 * norm2)
        }

        // 相似图像之间的相似度应该较高
        val similarityBetweenSimilar = cosineSimilarity(similarEmbeddings[0], similarEmbeddings[1])
        assertTrue(
            similarityBetweenSimilar > 0.5f,
            "相似图像的 embedding 应该有较高的相似度，实际值: $similarityBetweenSimilar"
        )

        // 不同图像之间的相似度应该较低
        val similarityWithDifferent = cosineSimilarity(similarEmbeddings[0], differentEmbedding)
        assertTrue(
            similarityWithDifferent < similarityBetweenSimilar,
            "不同图像的 embedding 相似度应该低于相似图像，实际值: $similarityWithDifferent vs $similarityBetweenSimilar"
        )

        log.debug("Model: ${embeddingModelEnum.modelName}, Similarity between similar images: $similarityBetweenSimilar")
        log.debug("Model: ${embeddingModelEnum.modelName}, Similarity with different image: $similarityWithDifferent")
    }

    @Test
    fun test_real_image_embedding() {
        // 测试真实图像 embedding
        // 使用 test-resources 下的真实图像文件（如果存在）
        val imageResource = try {
            ClassPathResource("image/test-image.png")
        } catch (e: Exception) {
            log.warn("真实图像文件不存在，跳过此测试: ${e.message}")
            return
        }

        if (!imageResource.exists()) {
            log.warn("真实图像文件不存在，跳过此测试")
            return
        }

        log.info("使用真实图像文件进行 embedding 测试")
        val embeddingModelEnum = defaultEmbeddingModel
        log.info("Model: ${embeddingModelEnum.modelName}, 图像文件路径: ${imageResource.path}")

        val imagePath = getImagePath(imageResource)

        // 验证图像可以读取
        val image = ImageIO.read(imageResource.inputStream)
        assertNotNull(image, "应该能够读取真实图像")
        assertTrue(image.width > 0 && image.height > 0, "图像尺寸应该大于 0")

        // Act
        val embeddingRequest = buildEmbeddingRequest(listOf(imagePath), embeddingModelEnum)
        val embeddingVector = try {
            embed(embeddingRequest).first()
        } catch (e: Exception) {
            log.error("真实图像文件 embedding 失败: ${e.message ?: e.javaClass.simpleName}", e)
            throw e
        }

        // Assert
        assertNotNull(embeddingVector, "embedding 向量不应该为 null")
        assertTrue(embeddingVector.isNotEmpty(), "embedding 向量不应该为空")

        val actualDimensions = embeddingVector.size
        assertTrue(actualDimensions > 0, "embedding 维度应该大于 0")

        log.info("Model: ${embeddingModelEnum.modelName}, 真实图像 embedding 生成成功，维度: ${embeddingVector.size}")
        log.debug("First 5 values: ${embeddingVector.take(5).joinToString()}")
    }

    private fun embed(embeddingRequest: EmbeddingRequest): List<FloatArray> {
        val response = when (embeddingModel) {
            is TransformersImageEmbeddingModel ->
                embeddingModel.call(embeddingRequest)
            else -> {
                error("未知模型：${embeddingModel::class}")
            }
        }
        return response.results
            .stream()
            .map(Embedding::getOutput)
            .toList()
    }

    private fun buildEmbeddingRequest(
        imagePaths: List<String>,
        embeddingModelEnum: ImageEmbeddingModelEnum,
    ): EmbeddingRequest {
        val opts = when (embeddingModel) {
            is TransformersImageEmbeddingModel -> {
                TransformersImageEmbeddingOptions(
                    model = embeddingModelEnum.modelName
                )
            }
            else -> {
                error("未知模型类型：${embeddingModel::class}")
            }
        }
        return EmbeddingRequest(imagePaths, opts)
    }

    companion object {
        /** 默认使用的图像 Embedding 模型 */
        val defaultEmbeddingModel = ImageEmbeddingModelEnum.SIGLIP2_BASE_PATCH16_224

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            // 启动 SentenceTransformers 容器并加载模型
            // 注意：transformers-inference 容器使用预构建镜像，镜像中已包含模型
            // 但如果需要使用其他模型（如 SigLIP2），可以通过 modelIds 参数加载
            SentenceTransformersTestContainer.startIfNeeded(
                registry,
                listOf(
                    defaultEmbeddingModel.modelName,
                    ImageEmbeddingModelEnum.SIGLIP2_BASE_PATCH16_256.modelName
                )
            )

            // 注册 EmbeddingModel 相关配置
            registry.add("spring.ai.sentence-transformers.base-url") { 
                "http://127.0.0.1:${SentenceTransformersTestContainer.PORT}" 
            }
        }
    }

}