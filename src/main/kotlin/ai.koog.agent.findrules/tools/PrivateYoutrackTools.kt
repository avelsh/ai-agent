package ai.koog.agent.findrules.tools

import ai.koog.agent.findrules.api.PrivateYoutrackClient
import ai.koog.agent.findrules.api.YoutrackWorkflow
import ai.koog.agent.findrules.api.YoutrackWorkflowRule
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.markdown.markdown

class PrivateYoutrackTools(private val privateYoutrackClient: PrivateYoutrackClient) : ToolSet {
    @Tool
    @LLMDescription("Fetch all workflows and their workflow rules accessible to the user that could potentially explain the observed behavior.")
    suspend fun getWorkflowRules(): String = workflowRules().toString()

    data class YoutrackResponseResult(
        val youtrackWorkflows: List<YoutrackWorkflow>?,
        val error: String? = null
    ) {
        override fun toString(): String {
            return when {
                error != null -> "There was an error calling the tool:\n$error"
                youtrackWorkflows == null || youtrackWorkflows.isEmpty() -> "There were no YouTrack workflows available"

                else -> markdown {
                    h1("Workflows and rules visible to the user's account:")
                    br()
                    
                    youtrackWorkflows.forEach { workflow ->
                        h2("Workflow ID: ${workflow.id}")
                        +"Workflow NAME: ${workflow.name}"
                        br()

                        val rules: List<YoutrackWorkflowRule> = workflow.rules ?: emptyList()
                        if (rules.isEmpty()) {
                            +"No rules in this workflow."
                            br()
                        } else {
                            +"Workflow rules:"
                            br()
                            rules.forEach { rule ->
                                h3("Rule TITLE: ${rule.title}")
                                +"Rule ID: ${rule.id}"
                                rule.type?.let { type ->
                                    +"Type: $type"
                                }
                                rule.script?.takeIf { it.isNotBlank() }?.let {
                                    br()
                                    +"Script:"
                                    +it
                                }
                                br()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun workflowRules(): YoutrackResponseResult {
        try {
            // Get the workflow rules
            val youtrackWorkflows: List<YoutrackWorkflow> = privateYoutrackClient.getWorkflowRules()

            return YoutrackResponseResult(
                youtrackWorkflows = youtrackWorkflows
            )
        } catch (e: Exception) {
            return YoutrackResponseResult(
                youtrackWorkflows = null,
                error = e.message
            )
        }
    }
}