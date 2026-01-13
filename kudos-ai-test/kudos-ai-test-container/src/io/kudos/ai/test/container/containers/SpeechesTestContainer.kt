package io.kudos.ai.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

/**
 * speeches-ai测试容器
 *
 * 兼具STT(speech-to-text)和TTS(text-to-speech)的本地语音能力，
 * "OpenAI API 兼容"的服务端，HTTP 接口接入。
 *
 * 模型/引擎：
 * STT：faster-whisper（Whisper 的高性能实现，通常更快、更省内存，还支持 8-bit 量化）。
 * TTS：Piper 和 Kokoro。
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
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?, apiKey: String? = null): Container {
        synchronized(this) {
            val running = TestContainerKit.isContainerRunning(LABEL)
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            
            // 如果指定了 API Key 且容器是新启动的，可以通过环境变量设置
            // 注意：由于容器已经配置，这里只能记录日志
            if (!running && apiKey != null) {
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
        startIfNeeded(null, apiKey)
        println("Speeches container started on localhost port: $PORT")
        apiKey?.let {
            println("API Key: $it")
        }
        println("API endpoint: http://localhost:$PORT")
        println("Health check: http://localhost:$PORT/health")
        println("API docs: http://localhost:$PORT/docs")
        Thread.sleep(Long.MAX_VALUE)
    }
}
