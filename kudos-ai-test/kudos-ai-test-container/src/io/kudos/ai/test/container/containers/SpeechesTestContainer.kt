package io.kudos.ai.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

/**
 * speeches-ai测试容器
 *
 * 兼具STT(speech-to-text)和TTS(text-to-speech)的本地语音能力，
 * "OpenAI API 兼容"的服务端，HTTP 接口接入。
 *
 * 模型/引擎：
 * STT：faster-whisper（Whisper 的高性能实现，通常更快、更省内存，还支持 8-bit 量化）。
 *   可用模型示例：
 *   - Systran/faster-whisper-tiny (39M参数)
 *   - Systran/faster-whisper-base (74M参数)
 *   - Systran/faster-whisper-small (244M参数)
 *   - Systran/faster-whisper-medium (769M参数)
 *   - Systran/faster-whisper-large-v3 (1.55B参数，默认)
 * TTS：Piper 和 Kokoro。
 *   可用模型示例：
 *   - speaches-ai/Kokoro-82M-v1.0-ONNX (默认)
 *   - rhasspy/piper-voices (Piper 模型)
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
object SpeechesTestContainer {

    private const val IMAGE_NAME = "ghcr.io/speaches-ai/speaches:0.9.0-rc.3-cpu"

    const val PORT = 28001

    private const val CONTAINER_PORT = 8000

    const val LABEL = "Speeches"

    val container = GenericContainer(IMAGE_NAME).apply {
        // 创建宿主机目录用于持久化模型
        // speaches-ai 将模型存储在 /home/ubuntu/.cache/huggingface/hub 目录
        val hostModelDir: Path = Path.of(System.getProperty("user.home"), ".cache", "speaches-tc", "huggingface", "hub")
        Files.createDirectories(hostModelDir)
        
        // 使用绑定挂载（bind mount）以支持高效的数据持久化
        // 注意：speaches-ai 容器默认使用 /home/ubuntu/.cache/huggingface/hub
        withFileSystemBind(
            hostModelDir.toAbsolutePath().toString(),
            "/home/ubuntu/.cache/huggingface/hub",
            BindMode.READ_WRITE
        )
        
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))
        
        // 环境变量配置
        withEnv("host", "0.0.0.0")
        withEnv("port", CONTAINER_PORT.toString())
        
        // 可选配置：模型 TTL（Time To Live）
        // -1 表示永不卸载，0 表示使用后立即卸载
        // 测试环境可以设置为 -1，避免频繁加载模型
        withEnv("stt_model_ttl", "-1")
        withEnv("tts_model_ttl", "-1")
        withEnv("vad_model_ttl", "-1")
        
        // 日志级别
        withEnv("log_level", "info")
        
        // 禁用 UI（测试环境通常不需要）
        withEnv("enable_ui", "False")
        
        // 等待 HTTP API 就绪
        // 使用健康检查端点
        // 注意：forPort 需要使用容器内部端口，而不是主机端口
        waitingFor(
            Wait.forHttp("/health")
                .forPort(CONTAINER_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(5))
        )
        
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * 检查模型是否在 speaches 注册表中支持
     *
     * @param modelId 模型 ID（如 "Systran/faster-whisper-base"）
     * @param baseUrl speaches API 的基础 URL
     * @param httpClient HTTP 客户端
     * @return 如果模型在注册表中支持则返回 true，否则返回 false
     */
    private fun isModelSupportedInRegistry(modelId: String, baseUrl: String, httpClient: HttpClient): Boolean {
        try {
            // 查询 speaches 的模型注册表
            val registryRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/registry"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build()
            
            val registryResponse = httpClient.send(registryRequest, HttpResponse.BodyHandlers.ofString())
            
            if (registryResponse.statusCode() == 200) {
                val responseBody = registryResponse.body()
                
                // 解析注册表响应（可能是 JSON 格式）
                // 简单检查：响应中是否包含模型 ID
                // 注意：这里使用简单的字符串匹配，如果注册表返回的是结构化 JSON，
                // 可以考虑使用 JSON 解析库进行更精确的匹配
                return responseBody.contains(modelId)
            } else {
                println("WARN: Failed to query registry, status code: ${registryResponse.statusCode()}")
                // 如果查询注册表失败，返回 false 以进行更严格的检查
                return false
            }
        } catch (e: Exception) {
            println("WARN: Error checking model registry: ${e.message}")
            // 如果查询注册表出错，返回 false 以进行更严格的检查
            return false
        }
    }

    /**
     * 下载模型（如果不存在）
     *
     * @param modelId 模型 ID（如 "Systran/faster-whisper-base"）
     * @param runningContainer 运行中的容器对象
     */
    private fun downloadModelIfAbsent(modelId: String, runningContainer: Container) {
        // 首先检查宿主机目录中是否已有模型文件
        val hostModelDir: Path = Path.of(System.getProperty("user.home"), ".cache", "speaches-tc", "huggingface", "hub")
        if (hostModelDir.exists()) {
            // Hugging Face 模型通常存储在 models--{org}--{name} 格式的目录中
            // 例如：Systran/faster-whisper-base -> models--Systran--faster-whisper-base
            val modelDirName = modelId.replace("/", "--")
            val modelPath = hostModelDir.resolve("models--$modelDirName")
            
            if (modelPath.exists()) {
                // 检查目录中是否有文件（模型文件通常包含多个文件）
                val files = try {
                    modelPath.listDirectoryEntries()
                } catch (e: Exception) {
                    emptyList()
                }
                
                if (files.isNotEmpty()) {
                    println("DEBUG: Model found in host directory: $modelPath (${files.size} files)")
                    println("Model already exists in persistent storage: $modelId, skip downloading.")
                    return
                }
            }
        }
        
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort
        val baseUrl = "http://$host:$port"
        
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        
        // 设置较长的超时时间，因为模型下载可能需要较长时间
        val requestTimeout = Duration.ofMinutes(10)
        
        try {
            // 0) 首先检查模型是否在 speaches 注册表中支持
            if (!isModelSupportedInRegistry(modelId, baseUrl, httpClient)) {
                throw IllegalArgumentException(
                    "Model '$modelId' is not supported in speaches registry. " +
                    "Please check available models at $baseUrl/v1/registry"
                )
            }
            
            // 1) 通过 API 检查模型是否已存在
            val listRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/models"))
                .GET()
                .timeout(requestTimeout)
                .build()
            
            val listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString())
            
            if (listResponse.statusCode() == 200) {
                val responseBody = listResponse.body()
                
                // 简单检查：响应中是否包含模型 ID
                // speaches API 可能返回 JSON 格式或简单列表，直接字符串匹配即可
                if (responseBody.contains(modelId)) {
                    println("Model already exists: $modelId, skip downloading.")
                    return
                }
            }
            
            // 2) 不存在则下载
            println("Start downloading model: $modelId ...")
            val time = System.currentTimeMillis()
            
            // 构建下载 URL
            // 注意：modelId 可能包含 '/'，这里需要 URL 编码成单个 path segment，
            // 否则路由可能会把它当成多段路径导致下载失败。
            val encodedModelId = URLEncoder.encode(modelId, StandardCharsets.UTF_8)
            val downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/models/$encodedModelId"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(requestTimeout)
                .build()
            
            val downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofString())

            check(downloadResponse.statusCode() in 200..299) {
                "Failed to download model $modelId, status code: ${downloadResponse.statusCode()}, response: ${downloadResponse.body()}"
            }
            println("Finish downloading model: $modelId in ${System.currentTimeMillis() - time}ms")
        } catch (e: Exception) {
            throw RuntimeException("Error downloading model $modelId: ${e.message}", e)
        }
    }

    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没安装时想忽略测试用例，可以用@EnabledIfDockerInstalled
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @param apiKey 可选，API 密钥（如果设置了，所有 API 请求都需要此密钥）
     * @param sttModel 可选的 STT 模型名称（如 "Systran/faster-whisper-base"），如果指定则使用该模型
     * @param ttsModel 可选的 TTS 模型名称（如 "speaches-ai/Kokoro-82M-v1.0-ONNX"），如果指定则使用该模型
     * @return 运行中的容器对象
     */
    fun startIfNeeded(
        registry: DynamicPropertyRegistry?,
        apiKey: String? = null,
        sttModel: String? = null,
        ttsModel: String? = null
    ): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            
            // 如果指定了 STT 模型，下载它
            sttModel?.let {
                downloadModelIfAbsent(it, runningContainer)
            }
            
            // 如果指定了 TTS 模型，下载它
            ttsModel?.let {
                downloadModelIfAbsent(it, runningContainer)
            }
            
            // 如果指定了 API Key，记录日志
            apiKey?.let {
                println("Note: API Key specified, but container environment is already set. " +
                        "Consider setting API_KEY environment variable before container creation.")
            }
            
            if (registry != null) {
                registerProperties(registry, runningContainer, apiKey)
            }
            return runningContainer
        }
    }

    /**
     * 注册 Spring 动态属性
     */
    private fun registerProperties(
        registry: DynamicPropertyRegistry,
        runningContainer: Container,
        apiKey: String?
    ) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort
        val baseUrl = "http://$host:$port"
        
        // 注册 speaches API 的基础 URL
        registry.add("spring.ai.speaches.base-url") { baseUrl }
        
        // 如果指定了 API Key，注册 API Key 配置
        apiKey?.let {
            registry.add("spring.ai.speaches.api-key") { it }
        }
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    /**
     * 主方法，用于手动启动容器
     */
    @JvmStatic
    fun main(args: Array<String>?) {
        val apiKey = args?.getOrNull(0)
        val sttModel = args?.getOrNull(1)
        val ttsModel = args?.getOrNull(2)
        
        startIfNeeded(null, apiKey, sttModel, ttsModel)
        
        println("Speeches container started on localhost port: $PORT")
        apiKey?.let {
            println("API Key: $it")
        }
        sttModel?.let {
            println("STT Model: $it")
        }
        ttsModel?.let {
            println("TTS Model: $it")
        }
        println("API endpoint: http://localhost:$PORT")
        println("Health check: http://localhost:$PORT/health")
        println("API docs: http://localhost:$PORT/docs")
        Thread.sleep(Long.MAX_VALUE)
    }
}
