package io.kudos.ai.test.container.containers.ollama

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * ollama测试容器抽象類
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractOllamaTestContainer(
    val imageName: String,
    val port: Int,
    val label: String
) {

    private val containerPort = 11434

    protected val container = GenericContainer(imageName).apply {
        // 絕對路徑，並確保目錄存在
        val hostDir: Path = Path.of(System.getProperty("user.home"), ".cache", "ollama-tc")
        Files.createDirectories(hostDir)

        // 使用绑定挂载（bind mount）以支持高效的数据持久化
        withFileSystemBind(
            hostDir.toAbsolutePath().toString(),
            "/root/.ollama",
            BindMode.READ_WRITE
        )

        withExposedPorts(containerPort)
        bindingPort(Pair(port, containerPort))
        // 让 Ollama 监听 0.0.0.0（容器内可被映射端口访问）
        withEnv("OLLAMA_HOST", "0.0.0.0:$containerPort")

        waitingFor(
            Wait.forHttp("/api/tags")
                .forPort(port)
                .forStatusCode(200)
        )
        withStartupTimeout(Duration.ofMinutes(3))

        withLabel(TestContainerKit.LABEL_KEY, label)
    }

    /**
     * 拉取模型
     */
    private fun pullModelIfAbsent(model: String?, container: Container) {
        if (model.isNullOrBlank()) { return }

        // 1) 先检查是否已存在
        val list = TestContainerKit.execInContainer(container, "ollama", "list")
        check(list.exitCode == 0) { "ollama list failed: ${list.stderr}\n${list.stdout}" }
        
        // 由于ollama list返回的model名字可能没有tag（如 'llama2' 而不是 'llama2:latest'），应同时支持tag和无tag的情况
        // 且ollama pull会在存在时很快返回。因此这里改进为：仅当fullname或无tag名都不存在时才pull
        val modelBase = model.substringBefore(':')

        // 解析已存在的模型列表，同时保存完整名和基础名
        val existingModels: Set<String> = list.stdout
            .lineSequence()
            .drop(1) // 跳过表头：NAME ID SIZE MODIFIED
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // NAME 通常是第一列，可能是 all-minilm:l6-v2 这种
                line.split(Regex("\\s+"), limit = 2).first()
            }
            .toSet()
        
        // 同时提取所有已存在模型的基础名（无tag）
        val existingModelBases: Set<String> = existingModels.map { it.substringBefore(':') }.toSet()

        // 调试信息：打印解析结果
        println("DEBUG: Requested model: $model, modelBase: $modelBase")
        println("DEBUG: Existing models: $existingModels")
        println("DEBUG: Existing model bases: $existingModelBases")

        // 检查：完整模型名存在，或基础名存在（无论tag是否匹配）
        val modelExists = model in existingModels || 
                         modelBase in existingModels || 
                         modelBase in existingModelBases
        
        println("DEBUG: modelExists = $modelExists (model in existingModels: ${model in existingModels}, modelBase in existingModels: ${modelBase in existingModels}, modelBase in existingModelBases: ${modelBase in existingModelBases})")
        
        if (modelExists) {
            val existingName = when {
                model in existingModels -> model
                modelBase in existingModels -> modelBase
                else -> existingModels.find { it.substringBefore(':') == modelBase } ?: modelBase
            }
            println("Model already exists: $existingName, skip pulling.")
            return
        }

        // 2) 不存在才 pull
        println("Start pulling model: $model ...")
        val time = System.currentTimeMillis()
        val r = TestContainerKit.execInContainer(container, "ollama", "pull", model)
        check(r.exitCode == 0) { "ollama pull $model failed: ${r.stderr}\n${r.stdout}" }
        println("Finish pulling model: $model in ${System.currentTimeMillis() - time}ms")
    }

    /**
     * 启动容器(若需要)
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没启动时想忽略测试用例，可以用@EnabledIfDockerAvailable
     * 来代替@Testcontainers(disabledWithoutDocker = true)
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @param model ollama支持的模型的名称
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?, model: String?): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(label, container)
            pullModelIfAbsent(model, runningContainer)
            if (registry != null) {
                registerProperties(registry, runningContainer, model)
            }
            return runningContainer
        }
    }

    protected fun registerProperties(
        registry: DynamicPropertyRegistry,
        runningContainer: Container,
        model: String?
    ) {
        registry.add("spring.ai.model.embedding") { "ollama" }
        model?.let { registry.add("spring.ai.ollama.embedding.options.model") { it } }
        registry.add("spring.ai.ollama.base-url") { "http://127.0.0.1:$port" }
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(label)

}