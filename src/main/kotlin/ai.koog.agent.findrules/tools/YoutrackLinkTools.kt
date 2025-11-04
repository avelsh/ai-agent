package ai.koog.agent.findrules.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

class YoutrackLinkTools(private val youtrackBaseUrl: String) : ToolSet {
    @Tool
    @LLMDescription(
        "Build a user-facing link to a workflow rule in YouTrack UI. " +
            "Args: projectNameOrKey (string), workflowId (string)."
    )
    fun buildWorkflowRuleLink(projectNameOrKey: String, workflowId: String): String {
        val base: String = youtrackBaseUrl.removeSuffix("/")
        val projectPath = projectNameOrKey.trim()
        val selected = workflowId.trim()
        return "$base/projects/$projectPath?tab=workflow&selected=$selected"
    }
}


