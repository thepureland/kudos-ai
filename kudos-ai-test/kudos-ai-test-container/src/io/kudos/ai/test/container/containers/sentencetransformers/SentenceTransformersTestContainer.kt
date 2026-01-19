package io.kudos.ai.test.container.containers.sentencetransformers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
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
        withExposedPorts(CONTAINER_PORT)
        bindingPort(Pair(PORT, CONTAINER_PORT))

        // 环境变量配置
        // 禁用 CUDA（测试环境通常使用 CPU）
        withEnv("ENABLE_CUDA", "0")

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
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没安装时想忽略测试用例，可以用@EnabledIfDockerInstalled
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)

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
        startIfNeeded(null)
        println("SentenceTransformers container started on localhost port: $PORT")
        println("API endpoint: http://localhost:$PORT")
        println("Vectors endpoint: http://localhost:$PORT/vectors")
        println("Root endpoint: http://localhost:$PORT/")
        Thread.sleep(Long.MAX_VALUE)
    }
}