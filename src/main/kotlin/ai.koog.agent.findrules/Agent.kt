package ai.koog.agent.findrules

import ai.koog.agent.findrules.api.PrivateYoutrackClient
import ai.koog.agent.findrules.tools.PrivateYoutrackTools
import ai.koog.agent.findrules.tools.YoutrackLinkTools
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agent.findrules.tools.UserTools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.xml.xml

fun createRulesExplanationAgent(
    promptExecutor: PromptExecutor,
    privateYoutrackClient: PrivateYoutrackClient,
    youtrackBaseUrl: String,
    youtrackMcpRegistry: ToolRegistry,
    onToolCallEvent: (String) -> Unit,
    showMessage: suspend (String) -> String,
): AIAgent<UserInput, UserInputToAgent> {
    val youtrackTools = youtrackMcpRegistry.tools
    val privateYoutrackTools = PrivateYoutrackTools(privateYoutrackClient)
    val youtrackLinkTools = YoutrackLinkTools(youtrackBaseUrl)
    val userTools = UserTools(
        showMessage
    )

    val toolRegistry = ToolRegistry {
        tools(userTools)
        tools(privateYoutrackTools)
        tools(youtrackLinkTools)
    } + youtrackMcpRegistry

    val findRulesStrategy = findRulesStrategy(
        youtrackTools = youtrackTools,
        privateYoutrackTools = privateYoutrackTools,
        youtrackLinkTools = youtrackLinkTools,
        userTools = userTools
    )

    val agentConfig = AIAgentConfig(
        prompt = prompt(
            "rules-explanation-agent-prompt",
            params = LLMParams(temperature = 0.2)
        ) {
            system(
                """
                You are an agent that finds a workflow rule that made some action in YouTrack by user’s description.               
                """.trimIndent()
            )
        },
        model = GoogleModels.Gemini2_5Pro,
        maxAgentIterations = 200
    )

    // Create the runner
    return AIAgent<UserInput, UserInputToAgent>(
        promptExecutor = promptExecutor,
        strategy = findRulesStrategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry,
    ) {
        handleEvents {
            onToolCall { ctx ->
                onToolCallEvent(
                    "Tool ${ctx.tool.name}, args ${
                        ctx.toolArgs.toString().replace('\n', ' ').take(100)
                    }..."
                )
            }
        }
    }
}


private fun findRulesStrategy(
    youtrackTools: List<Tool<*, *>>,
    privateYoutrackTools: PrivateYoutrackTools,
    youtrackLinkTools: YoutrackLinkTools,
    userTools: UserTools
) = strategy<UserInput, UserInputToAgent>("find-rules-strategy") {
    val userInputKey = createStorageKey<UserInputToAgent>("user_input")
    val prevSuggestedExplanationKey = createStorageKey<UserInputToAgent>("prev_suggested_explanation")

    // Nodes

    val setup by node<UserInput, String> { userInput -> userInput.message }

    val clarifyUserProblem by subgraphWithTask<String, UserInputToAgent>(
        tools = userTools.asTools()
    ) { initialMessage ->
        xml {
            tag("instructions") {
                +
                """
                Clarify the user problem if it was not provided by the user. The user can only describe problems in text format.
                """.trimIndent()
            }

            tag("initial_user_message") {
                +initialMessage
            }
        }
    }

    val suggestExplanation by subgraphWithTask<SuggestExplanationRequest, UserInputToAgent>(
        tools = youtrackTools + privateYoutrackTools.asTools() + youtrackLinkTools.asTools()
    ) { input ->
        xml {
            tag("instructions") {
                markdown {
                    h2("Requirements")
                    bulleted {
                        item("Explain the reason for the problem that was described by the user.")
                        item("Always provide the link to the workflow rule that may cause the user problem.")
                    }

                    h2("Tool usage guidelines")
                    +"""
                    ALWAYS use the getWorkflowRules tool to get available workflow rules from YouTrack 
                    and find the rule that may cause the user problem. Avoid making your own suggestions.                                                     
                    """.trimIndent()
                    br()

                    h2("Link building tool")
                    +"""
                    ALWAYS use  the buildWorkflowRuleLink tool to build the link to the workflow rule.
                    Args: projectNameOrKey (string), workflowId (string).
                    Example: buildWorkflowRuleLink("PROJECT_NAME", "123-56")
                    """.trimIndent()
                    br()

                    """
                    Use other tools from YouTrack to get more context/information.
                    """.trimIndent()
                }
            }

            when (input) {
                is SuggestExplanationRequest.InitialRequest -> {
                    tag("user_input") {
                        + input.userInput.toMarkdownString()
                    }
                }

                is SuggestExplanationRequest.CorrectionRequest -> {
                    tag("additional_instructions") {
                        +"User asked for clarifications/corrections to the previously suggested explanation. "
                        + "Provide updated explanation according to these clarifications/corrections and check that provided link is correct.\n."
                    }

                    tag("user_input") {
                        + input.userInput.toMarkdownString()
                    }

                    tag("previously_suggested_explanation") {
                        + input.prevSuggestedExplanation.toMarkdownString()
                    }

                    tag("user_feedback") {
                        + input.userFeedback
                    }
                }
            }
        }
    }

    val saveUserInput by node<UserInputToAgent, Unit> { input ->
        storage.set(userInputKey, input)

        llm.writeSession {
            replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.WholeHistory)
        }
    }

    val savePrevSuggestedExplanation by node<UserInputToAgent, UserInputToAgent> { explanation ->
        storage.set(prevSuggestedExplanationKey, explanation)

        llm.writeSession {
            replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.WholeHistory)
        }

        explanation
    }

    val createInitialExplanationRequest by node<Unit, SuggestExplanationRequest> {
        SuggestExplanationRequest.InitialRequest(
            userInput = storage.getValue(userInputKey),
        )
    }

    val createExplanationCorrectionRequest by node<String, SuggestExplanationRequest> { userFeedback ->
        SuggestExplanationRequest.CorrectionRequest(
            userFeedback = userFeedback,
            userInput = storage.getValue(userInputKey),
            prevSuggestedExplanation = storage.getValue(prevSuggestedExplanationKey)
        )
    }

    // Show explanation suggestion to the user and get a response
    val showExplanationSuggestion by node<String, String> { message ->
        userTools.showMessage(message)
    }

    val processUserFeedback by nodeLLMRequestStructured<ExplanationSuggestionFeedback>()

    // Edges

    nodeStart then setup then clarifyUserProblem then saveUserInput then createInitialExplanationRequest then suggestExplanation then savePrevSuggestedExplanation

    edge(
        savePrevSuggestedExplanation forwardTo showExplanationSuggestion
            transformed { it.toMarkdownString() }
    )

    edge(showExplanationSuggestion forwardTo processUserFeedback)

    edge(
        processUserFeedback forwardTo createExplanationCorrectionRequest
            transformed { it.getOrThrow().structure }
            onCondition { !it.isAccepted }
            transformed { it.message }
    )
    edge(
        processUserFeedback forwardTo nodeFinish
            transformed { it.getOrThrow().structure }
            onCondition { it.isAccepted }
            transformed { storage.getValue(prevSuggestedExplanationKey) }
    )

    edge(createExplanationCorrectionRequest forwardTo suggestExplanation)
}