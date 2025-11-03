package ai.koog.agent.findrules

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.markdown.markdown
import kotlinx.serialization.Serializable

data class UserInput(
    val message: String
)

sealed interface SuggestExplanationRequest {
    data class InitialRequest(
        val userInput: UserInputToAgent,
    ) : SuggestExplanationRequest

    data class CorrectionRequest(
        val userInput: UserInputToAgent,
        val userFeedback: String,
        val prevSuggestedExplanation: UserInputToAgent,
    ) : SuggestExplanationRequest
}

@Serializable
@LLMDescription("User feedback for the explanation and the link to the workflow rule.")
data class ExplanationSuggestionFeedback(
    @property:LLMDescription("Whether the explanation and the link to the corresponding workflow rule are accepted.")
    val isAccepted: Boolean,
    @property:LLMDescription("The original message from the user.")
    val message: String,
)


@Serializable
@LLMDescription(
    "Finish tool to compile final explanation and a link to the workflow rule for the user's request. \n" +
        "Call to provide the final explanation and the workflow rule suggestion result."
)
data class UserInputToAgent(
    @property:LLMDescription("The problems in the user experience with youtrack rules.")
    val problems: List<Problem>,
) {
    @Serializable
    @LLMDescription("The problem in the user experience with youtrack rules.")
    data class Problem(
        @property:LLMDescription("The problem description")
        val description: String
    )

    fun toMarkdownString(): String = markdown {
        h1("Problem:")
        br()

        problems.forEach { problem ->
            h2(problem.description)
            br()
        }
    }
}
