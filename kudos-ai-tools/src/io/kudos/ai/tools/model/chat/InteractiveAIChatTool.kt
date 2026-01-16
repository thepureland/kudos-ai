package io.kudos.ai.tools.model.chat

import io.kudos.ai.ability.model.text.support.enums.impl.TextChatModelEnum
import io.kudos.ai.test.container.containers.ollama.OllamaMiniTestContainer
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaChatOptions
import java.util.Scanner

/**
 * 基于ollama的test-container的交互式AI聊天工具
 *
 * 从控制台提交prompt，交给由test-container运行的ollama处理后，将回答显示于控制台。
 * 可多轮交互，直至手动中止进程。
 *
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
object InteractiveAIChatTool {

    private val DEFAULT_MODEL = TextChatModelEnum.LLAMA_3_1_8B

    private val OLLAMA_URL = "http://127.0.0.1:${OllamaMiniTestContainer.port}"

    @JvmStatic
    fun main(args: Array<String>) {
        // 解析命令行参数：模型名称
        val modelName = args.getOrNull(0)?.let { modelArg ->
            TextChatModelEnum.entries.find { it.modelName == modelArg }?.modelName
                ?: modelArg // 如果找不到枚举，直接使用输入的字符串
        } ?: DEFAULT_MODEL.modelName

        println("=".repeat(60))
        println("交互式聊天工具 - 基于 Ollama Test Container")
        println("=".repeat(60))
        println("使用模型: $modelName")
        println("提示: 输入 'quit' 或 'exit' 退出，输入 'clear' 清屏")
        println("=".repeat(60))
        println()

        // 启动 Ollama 容器
        println("正在启动 Ollama 容器...")
        OllamaMiniTestContainer.startIfNeeded(null, modelName)
        println("Ollama 容器已启动: $OLLAMA_URL")
        println()

        // 创建 ChatModel
        val ollamaApi = OllamaApi.builder()
            .baseUrl(OLLAMA_URL)
            .build()
        
        val chatOptions = OllamaChatOptions.builder()
            .model(modelName)
            .temperature(0.7)
            .build()
        
        val chatModel: ChatModel = OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(chatOptions)
            .build()

        // 交互式循环
        val scanner = Scanner(System.`in`)
        // 维护对话历史消息列表，用于保留上下文
        val conversationHistory = mutableListOf<Message>()

        while (true) {
            print("你: ")
            val userInput = scanner.nextLine().trim()

            // 处理退出命令
            if (userInput.isEmpty()) {
                continue
            }
            if (userInput.lowercase() in listOf("quit", "exit", "q")) {
                println("\n再见！")
                break
            }
            if (userInput.lowercase() == "clear") {
                conversationHistory.clear()
                println("对话历史已清空\n")
                continue
            }

            try {
                // 构建包含历史消息的 Prompt
                val messages = conversationHistory.toMutableList()
                messages.add(UserMessage(userInput))
                val prompt = Prompt(messages)

                // 调用 ChatModel
                print("AI: ")
                val response: ChatResponse = chatModel.call(prompt)
                val content = response.result.output.text!!
                println(content)
                println()

                // 将用户消息和 AI 回复都添加到历史中，保留上下文
                conversationHistory.add(UserMessage(userInput))
                conversationHistory.add(AssistantMessage(content))
            } catch (e: Exception) {
                println("错误: ${e.message}")
                e.printStackTrace()
                println()
            }
        }

        scanner.close()
    }
}
