package io.kudos.ai.test.container.containers.sentencetransformers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * sentence-transformers测试容器
 *
 * 提供多语言文本embedding能力的本地服务，基于sentence-transformers模型。
 * 使用HTTP接口接入，支持批量文本向量化。
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
object SentenceTransformersTestContainer {

    private const val IMAGE_NAME = "semitechnologies/transformers-inference:sentence-transformers-paraphrase-multilingual-MiniLM-L12-v2-1.13.2"

    const val PORT = 28002

    private const val CONTAINER_PORT = 8080

    const val LABEL = "SentenceTransformers"

    val container = GenericContainer(IMAGE_NAME).apply {
        // 创建宿主机目录用于持久化模型
        // transformers-inference 容器将模型存储在 HuggingFace 缓存目录
        // 通常为 ~/.cache/huggingface/hub 或 /root/.cache/huggingface/hub
        val hostModelDir: Path = Path.of(System.getProperty("user.home"), ".cache", "transformers-tc", "huggingface", "hub")
        Files.createDirectories(hostModelDir)

        // 使用绑定挂载（bind mount）以支持高效的数据持久化
        // transformers-inference 容器默认使用 /root/.cache/huggingface/hub
        withFileSystemBind(
            hostModelDir.toAbsolutePath().toString(),
            "/root/.cache/huggingface/hub",
            BindMode.READ_WRITE
        )

        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))

        // 环境变量配置
        // 禁用 CUDA（测试环境通常使用 CPU）
        withEnv("ENABLE_CUDA", "0")
        
        // 设置 HuggingFace 缓存目录（可选，但有助于明确指定）
        withEnv("HF_HOME", "/root/.cache/huggingface")

        // 日志配置
        withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger("SentenceTransformersContainer")).withPrefix("sentence-transformers"))

        // 等待端口就绪
        // transformers-inference 容器没有标准的健康检查端点，使用端口监听等待策略
        waitingFor(
            Wait.forListeningPort()
                .withStartupTimeout(Duration.ofMinutes(5))
        )

        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * 下载/加载模型（如果不存在）
     *
     * 注意：transformers-inference 容器使用预构建镜像，镜像名称中已包含模型。
     * 但如果需要使用其他模型（如 SigLIP2），可以通过 API 加载。
     * 
     * @param modelId 模型 ID（如 "google/siglip2-base-patch16-224"）
     * @param runningContainer 运行中的容器对象
     */
    private fun loadModelIfAbsent(modelId: String, runningContainer: Container) {
        val port = runningContainer.ports.first().publicPort
        val baseUrl = "http://127.0.0.1:$port"

        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        // 设置较长的超时时间，因为模型下载可能需要较长时间
        val requestTimeout = Duration.ofMinutes(10)

        try {
            // 1) 通过 API 检查模型是否已在容器中加载
            val listRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/models"))
                .GET()
                .timeout(requestTimeout)
                .build()

            val listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString())

            if (listResponse.statusCode() == 200) {
                val responseBody = listResponse.body()

                // 检查模型是否已在容器中加载
                if (responseBody.contains(modelId)) {
                    println("Model already loaded in container: $modelId")
                    return
                }
            }

            // 2) 模型未加载，需要下载/加载
            // 首先检查宿主机目录中是否已有模型文件
            val hostModelDir: Path = Path.of(System.getProperty("user.home"), ".cache", "transformers-tc", "huggingface", "hub")
            val modelDirName = modelId.replace("/", "--")
            val modelPath = hostModelDir.resolve("models--$modelDirName")

            val modelFilesExist = if (Files.exists(hostModelDir) && Files.exists(modelPath)) {
                val files = try {
                    Files.list(modelPath).use { it.toList() }
                } catch (_: Exception) {
                    emptyList()
                }
                files.isNotEmpty()
            } else {
                false
            }

            if (modelFilesExist) {
                println("DEBUG: Model found in host directory: $modelPath")
                println("Model files exist locally, but not loaded in container. Loading model: $modelId ...")
            } else {
                println("Start downloading model: $modelId ...")
            }

            val time = System.currentTimeMillis()

            // 构建下载/加载 URL
            // 注意：modelId 可能包含 '/'，这里需要 URL 编码成单个 path segment，
            // 否则路由可能会把它当成多段路径导致下载失败。
            val encodedModelId = URLEncoder.encode(modelId, StandardCharsets.UTF_8)
            val downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/models/$encodedModelId"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(requestTimeout)
                .build()

            val downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofString())

            if (downloadResponse.statusCode() in 200..299) {
                if (modelFilesExist) {
                    println("Finish loading model: $modelId in ${System.currentTimeMillis() - time}ms")
                } else {
                    println("Finish downloading model: $modelId in ${System.currentTimeMillis() - time}ms")
                }
            } else {
                // 如果返回错误，记录日志但不抛出异常
                // 因为某些 transformers-inference 版本可能不支持 /v1/models API
                println("Warning: Failed to download/load model $modelId, status code: ${downloadResponse.statusCode()}, response: ${downloadResponse.body()}")
                println("Model may be loaded on first use via /vectors endpoint")
            }
        } catch (e: Exception) {
            println("Warning: Could not verify/load model $modelId: ${e.message}")
            // 不抛出异常，因为容器可能已经预加载了模型，或者会在首次使用时自动加载
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
     * @param modelIds 模型id列表（可选，用于加载额外的模型）
     * @return 运行中的容器对象
     */
    fun startIfNeeded(
        registry: DynamicPropertyRegistry?,
        modelIds: List<String> = emptyList()
    ): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)

            // 加载所有指定的模型
            modelIds.forEach { modelId ->
                loadModelIfAbsent(modelId, runningContainer)
            }

            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    /**
     * 注册 Spring 动态属性
     */
    private fun registerProperties(
        registry: DynamicPropertyRegistry,
        runningContainer: Container
    ) {
        val port = runningContainer.ports.first().publicPort
        val baseUrl = "http://127.0.0.1:$port"

        // 注册 sentence-transformers API 的基础 URL
        // 注意：根据实际使用的客户端库，可能需要调整属性名称
        registry.add("spring.ai.sentence-transformers.base-url") { baseUrl }
        
        // 如果使用 OpenAI 兼容的 API，也可以注册为 OpenAI 配置
        // registry.add("spring.ai.openai.base-url") { baseUrl }
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
        val modelIds = if (args != null && args.isNotEmpty()) {
            args.toList()
        } else {
            emptyList()
        }
        startIfNeeded(null, modelIds)
        println("SentenceTransformers container started on localhost port: $PORT")
        println("API endpoint: http://localhost:$PORT")
        println("Vectors endpoint: http://localhost:$PORT/vectors")
        if (modelIds.isNotEmpty()) {
            println("Models to load: ${modelIds.joinToString(", ")}")
        }
        Thread.sleep(Long.MAX_VALUE)
    }
}