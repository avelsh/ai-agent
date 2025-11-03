package ai.koog.agent.findrules.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class YoutrackWorkflow(
    val id: String,
    val name: String,
    val rules: List<YoutrackWorkflowRule>? = null
)

@Serializable
data class YoutrackWorkflowRule(
    val title: String? = null,
    val script: String? = null,
    val id: String,
    @SerialName("\$type") val type: String? = null
)

/**
 * Client for interacting with the Private Youtrack API
 */
class PrivateYoutrackClient() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    /**
     * Fetch workflows rules of the youtrack projects
     *
     */
    suspend fun getWorkflowRules(): List<YoutrackWorkflow> {
        val response: List<YoutrackWorkflow> = client.get("https://ai-agent.youtrack.cloud/api/admin/workflows") {
            header("Authorization", "Bearer perm-YWRtaW4=.NDEtMA==.iEWw3U3bH62WzOC8BBqkvZzz0PFGO0")
            header("Accept", "application/json")
            parameter("fields", "id,name,rules(id,title,script)")
            parameter("top", "-1")
        }.body()
        println("response")
        println(response)
        return response
    }
}
