package ai.koog.agent.findrules

import ai.koog.agent.findrules.api.PrivateYoutrackClient
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.delay

suspend fun main() {
    val googleAiKey = System.getenv("GOOGLE_AI_API_KEY")
    //val openAiKey = System.getenv("OPENAI_API_KEY")

    val youtrackToken = System.getenv("YOUTRACK_API_TOKEN")
    val youtrackUrl = System.getenv("YOUTRACK_URL")

    val youtrackMcp = createYoutrackMcp(youtrackUrl, youtrackToken)

    try {
        // Create agent
        val agent = createRulesExplanationAgent(
            promptExecutor = MultiLLMPromptExecutor(
                LLMProvider.Google to GoogleLLMClient(googleAiKey),
                //LLMProvider.OpenAI to OpenAILLMClient(openAiKey),
            ),
            privateYoutrackClient = PrivateYoutrackClient(youtrackUrl, youtrackToken),
            youtrackMcpRegistry = McpToolRegistryProvider.fromTransport(youtrackMcp),
            onToolCallEvent = { println("Tool called: $it") },
            showMessage = {
                println("Agent: $it")
                print("Response > ")
                readln()
            }
        )

        // Get initial request
        println("Hi, I'm a rules explanation agent of Youtrack. "
                + "Describe your problem in details, and I'll help you explain and find the according rules.")
        print("Response > ")
        val message = readln()

        val userInput = UserInput(message = message)

        // Print final result
        val result: UserInputToAgent = agent.run(userInput)
        println(result.toMarkdownString())
    } finally {
        youtrackMcp.close()
    }
}

private suspend fun createYoutrackMcp(youTrackUrl: String, youTrackToken: String): StdioClientTransport {
    val process = ProcessBuilder(
        "npx", "mcp-remote",
        "$youTrackUrl/mcp",
        "--header", "Authorization:Bearer $youTrackToken"
    )
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    // Wait briefly for the mcp-remote proxy to be ready
    delay(1000)

    // Connect stdio to Koog’s MCP transport
    return McpToolRegistryProvider.defaultStdioTransport(process)
}