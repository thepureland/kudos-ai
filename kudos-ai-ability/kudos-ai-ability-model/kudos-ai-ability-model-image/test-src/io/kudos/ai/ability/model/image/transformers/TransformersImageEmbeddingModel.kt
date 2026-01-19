package io.kudos.ai.ability.model.image.transformers

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.*
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * 适配 Transformers Inference 的图像 embedding。
 *
 * 约定：
 * - EmbeddingRequest.instructions(List<String>) 的每个字符串 = 图像文件路径（本地路径 or classpath 相对路径都可）
 * - options 必须是 TransformersImageEmbeddingOptions，里面带 model（例如 "google/siglip2-base-patch16-224"）
 *
 * Transformers Inference API：POST /vectors
 * 请求格式：{"inputs": [base64_encoded_image], "model": "model_name"}
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
class TransformersImageEmbeddingModel(
    private val restClient: RestClient, // baseUrl 指向 Transformers Inference，例如 http://localhost:28002
    private val defaultModel: String? = null // 默认模型名称（可选）
) : EmbeddingModel {

    override fun call(request: EmbeddingRequest): EmbeddingResponse {
        val opts = request.options as? TransformersImageEmbeddingOptions
            ?: error("TransformersImageEmbeddingModel 需要 TransformersImageEmbeddingOptions，但实际是：${request.options?.javaClass}")

        val modelName = opts.getModel()
        val inputs = request.instructions ?: emptyList()
        require(inputs.isNotEmpty()) { "EmbeddingRequest.instructions 为空" }

        val embeddings = inputs.mapIndexed { index, imagePathString ->
            createEmbedding(imagePathString, modelName, index)
        }

        return EmbeddingResponse(embeddings)
    }

    override fun embed(document: Document): FloatArray {
        val content = document.formattedContent
        val imagePath = when {
            !content.isNullOrBlank() -> content
            else -> document.metadata["imagePath"] as? String
                ?: error("Document 必须包含图像文件路径（content 或 metadata['imagePath']）")
        }

        val modelName = defaultModel
            ?: (document.metadata["model"] as? String)
            ?: error("必须指定模型名称（通过 defaultModel 或 metadata['model']）")

        val embedding = createEmbedding(imagePath, modelName, 0)
        return embedding.output
    }

    /**
     * 创建单个图像的 embedding
     */
    private fun createEmbedding(imagePathString: String, modelName: String, index: Int): Embedding {
        val imageBytes = readImageBytes(imagePathString)
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)

        // Transformers Inference API 格式
        // 对于图像模型，需要正确编码模型名称中的 '/' 为 '%2F'
        // 注意：不要使用 URLEncoder.encode，因为它会编码所有特殊字符，导致双重编码
        val encodedModelName = modelName.replace("/", "%2F")
        
        // 尝试使用 feature-extraction 端点（用于视觉模型）
        // 格式：{"inputs": [base64_encoded_image]}
        val requestBody = mapOf(
            "inputs" to listOf(base64Image)
        )

        val resp = try {
            // 首先尝试使用 feature-extraction 端点（视觉模型的常用端点）
            // 使用 UriComponentsBuilder 正确构建 URI，只编码路径段，不编码整个路径
            val uri = UriComponentsBuilder.fromPath("/v1/models/{model}/feature-extraction")
                .buildAndExpand(encodedModelName)
                .toUriString()
            restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Any::class.java)
        } catch (_: Exception) {
            // 如果 feature-extraction 失败，尝试使用 /v1/feature-extraction 端点（不带模型路径）
            try {
                val uri = UriComponentsBuilder.fromPath("/v1/feature-extraction/{model}")
                    .buildAndExpand(encodedModelName)
                    .toUriString()
                restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map::class.java)
            } catch (_: Exception) {
                // 尝试使用 /v1/models/{model}/embed 端点
                try {
                    val uri = UriComponentsBuilder.fromPath("/v1/models/{model}/embed")
                        .buildAndExpand(encodedModelName)
                        .toUriString()
                    restClient.post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(Map::class.java)
                } catch (_: Exception) {
                    // 如果都失败，尝试使用 /vectors 端点，但使用 "text" 字段（某些版本可能支持）
                    // 注意：这通常用于文本，但某些实现可能支持 base64 图像
                    try {
                        restClient.post()
                            .uri("/vectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(mapOf(
                                "text" to base64Image,
                                "model" to modelName
                            ))
                            .retrieve()
                            .body(Any::class.java)
                    } catch (_: Exception) {
                        // 最后尝试使用 multipart form data（类似音频模型）
                        val bodyBuilder = MultipartBodyBuilder().apply {
                            part("model", modelName)
                            part("inputs", base64Image)
                        }
                        restClient.post()
                            .uri("/vectors")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(bodyBuilder.build())
                            .retrieve()
                            .body(Any::class.java)
                    }
                }
            }
        } ?: error("Transformers Inference 返回空响应")

        val vector = extractEmbeddingVector(resp)
            ?: error("无法从 Transformers Inference 响应解析 embedding，响应：$resp (类型: ${resp.javaClass})")

        return Embedding(vector.toFloatArray(), index)
    }

    private fun readImageBytes(pathOrResource: String): ByteArray {
        // 1) 先当作本地路径
        val p: Path = Paths.get(pathOrResource)
        if (Files.exists(p) && Files.isRegularFile(p)) {
            return Files.readAllBytes(p)
        }

        // 2) 再当作 classpath resource（例如 "image/test-image.jpg"）
        val cl = Thread.currentThread().contextClassLoader
        val url = cl.getResource(pathOrResource)
            ?: error("找不到图像文件：'$pathOrResource'（既不是本地文件，也不是 classpath resource）")
        return url.openStream().use { it.readBytes() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractEmbeddingVector(resp: Any): List<Float>? {
        // 兼容多种响应格式：
        
        // 1) 响应是直接数组：[[0.1, 0.2, ...]]
        if (resp is List<*>) {
            if (resp.isEmpty()) return null
            val firstItem = resp[0]
            if (firstItem is List<*>) {
                // 嵌套数组，取第一个
                return firstItem.mapNotNull { (it as? Number)?.toFloat() }
            } else if (firstItem is Number) {
                // 直接是数字数组
                return resp.mapNotNull { (it as? Number)?.toFloat() }
            }
        }

        // 2) 响应是 Map 对象
        if (resp !is Map<*, *>) return null
        
        // A) {"vectors": [[0.1, 0.2, ...]]}
        val vectors = resp["vectors"]
        if (vectors is List<*> && vectors.isNotEmpty()) {
            val firstVector = vectors[0]
            if (firstVector is List<*>) {
                return firstVector.mapNotNull { (it as? Number)?.toFloat() }
            }
        }

        // B) {"embedding": [0.1, 0.2, ...]}
        val direct = resp["embedding"]
        if (direct is List<*>) {
            return direct.mapNotNull { (it as? Number)?.toFloat() }
        }

        // C) {"data": [{"embedding": [...]}]}
        val data = resp["data"]
        if (data is List<*> && data.isNotEmpty()) {
            val first = data[0] as? Map<*, *> ?: return null
            val emb = first["embedding"]
            if (emb is List<*>) {
                return emb.mapNotNull { (it as? Number)?.toFloat() }
            }
        }

        // D) 响应可能包含其他字段，尝试查找任何 List<Number> 类型的值
        resp.values.forEach { value ->
            if (value is List<*>) {
                val numbers = value.mapNotNull { (it as? Number)?.toFloat() }
                if (numbers.isNotEmpty() && numbers.size > 10) { // 假设 embedding 维度 > 10
                    return numbers
                }
            }
        }

        return null
    }
}


/**
 * Transformers Image Embedding 选项
 */
class TransformersImageEmbeddingOptions(
    private val model: String,
    private val dimensions: Int = -1
) : EmbeddingOptions {
    override fun getModel(): String = model
    override fun getDimensions(): Int = dimensions
}
