# YouTrack Rules Explanation Agent

Kotlin-based AI agent using the Koog framework to help users understand YouTrack workflow rules and explain why certain actions are blocked or behave unexpectedly. The agent runs as an interactive console application.

## Features

- Analyzes YouTrack workflow rule issues
- Explains why state transitions and other actions are blocked
- Provides links to workflow rules in YouTrack
- Interactive conversation to clarify user problems
- Powered by Google Gemini AI (Gemini 2.5 Pro) (You can switch to OpenAI if you want to)
- Integrates with YouTrack via:
  - MCP (Model Context Protocol) for YouTrack tools
  - Direct API calls via PrivateYoutrackClient for workflow rules
  - Link generation tool (YoutrackLinkTools) to build correct rule URLs

## Schema

![YouTrack Rules Explanation Agent Schema](./assets/youtrack-ai-agent.svg)

## Setup

### Prerequisites

- Java 17
- Gradle 8+ (included via wrapper)
- Google AI API key
- YouTrack instance with API token
- Node.js (for MCP remote proxy via npx)

### Environment Variables

```bash
export GOOGLE_AI_API_KEY="your-google-ai-api-key"
export YOUTRACK_API_TOKEN="your-youtrack-auth-api-token"
export YOUTRACK_URL="https://your-company.youtrack.cloud"
```

## Running the Agent

### Start the Interactive Console

```bash
./gradlew run
```

You should see:
```
Hi, I'm a rules explanation agent of Youtrack. Describe your problem in details, and I'll help you explain and find the according rules.
Response > 
```

The agent will:
1. Ask you to describe your YouTrack problem
2. Clarify any unclear aspects of the problem
3. Fetch relevant workflow rules from YouTrack
4. Provide an explanation and links to the relevant workflow rules
5. Allow you to provide feedback and refine the explanation


### Clean Build Artifacts

```bash
./gradlew clean
```

## Project Structure

```
ai-agent/
├── src/main/kotlin/
│   └── ai.koog.agent.findrules/
│       ├── Main.kt                      # Console CLI application entry point
│       ├── Agent.kt                     # Koog agent implementation and strategy
│       ├── Structs.kt                   # Data structures and types
│       ├── api/
│       │   └── PrivateYoutrackClient.kt  # Direct YouTrack API client for workflows
│       └── tools/
│           ├── UserTools.kt              # User interaction tools (showMessage)
│           ├── PrivateYoutrackTools.kt   # YouTrack workflow rules fetching tool
│           └── YoutrackLinkTools.kt      # Builds UI links to workflow rules
├── build.gradle.kts                     # Gradle build configuration
├── gradle/
│   ├── libs.versions.toml                # Dependency version catalog
│   └── wrapper/                          # Gradle wrapper files
├── gradlew                                # Gradle wrapper script (Unix)
├── gradlew.bat                           # Gradle wrapper script (Windows)
└── README.md                             # This file
```


1. **Agent Initialization**: The agent is created with:
   - Google Gemini AI (Gemini 2.5 Pro via PromptExecutor)
   - YouTrack MCP tools (via ToolRegistry for general YouTrack operations)
   - PrivateYoutrackClient (direct API calls for workflow rules)
   - YoutrackLinkTools (builds correct UI links for workflow rules)
   - User interaction tools (for asking clarifying questions)

2. **Interactive Flow**:
   - User describes their YouTrack problem
   - Agent may ask for clarification if needed
   - Agent fetches relevant workflow rules from YouTrack
   - Agent analyzes the problem and provides explanation with workflow rule links
   - User can provide feedback to refine the explanation

3. **Workflow Rules & Link Building**: The agent uses:
   - `PrivateYoutrackClient` to directly call YouTrack API `/api/admin/workflows` endpoint
   - Fetches workflows with their rules (id, name, title, script, type)
   - Formats the results in a readable markdown format for the LLM
   - `YoutrackLinkTools.buildWorkflowRuleLink(projectNameOrKey, workflowId)` to construct links like: `https://<company>.youtrack.cloud/projects/<Project Name>?tab=workflow&selected=<Workflow ID>`

4. **MCP Integration**: The agent uses YouTrack MCP server via `npx mcp-remote` to access additional YouTrack tools and data

## Development

### Adding New Features

1. **Modify Agent Logic**: Edit `Agent.kt` to change agent behavior and strategy
2. **Add Tools**: 
   - Extend `UserTools.kt` for user interaction tools
   - Extend `PrivateYoutrackTools.kt` for YouTrack API tools
   - Add YouTrack MCP tools via MCP server
3. **Update Data Structures**: Modify `Structs.kt` for new data types
4. **Update API Client**: Modify `PrivateYoutrackClient.kt` to add new YouTrack API endpoints


## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `GOOGLE_AI_API_KEY` | Google AI API key for Gemini | Yes | - |
| `YOUTRACK_API_TOKEN` | YouTrack API token (value without "Bearer ") | Yes | - |
| `YOUTRACK_URL` | YouTrack instance URL (e.g. https://your-company.youtrack.cloud) | Yes | - |

### Dependencies

Key dependencies (see `gradle/libs.versions.toml`):
- Kotlin 2.1.20
- Ktor 3.2.2 (client and server)
- Koog Framework 0.4.2-feat-1-3
- Kotlinx Coroutines 1.10.2
- Kotlinx Serialization

## Troubleshooting

### Java lang conflicts

1. Install Java 17.
2. Update configs in the gradle.properties file 

org.gradle.java.home=/Users/max.musterman/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home

### MCP Connection Issues

If YouTrack MCP tools fail:
1. Verify `YOUTRACK_URL` and `YOUTRACK_API_TOKEN` are correct
2. Check that `npx mcp-remote` is available (requires Node.js)
3. Ensure the MCP endpoint is accessible at `$YOUTRACK_URL/mcp`
4. Check console output for MCP connection errors

### API Key Errors

Verify your environment variables are set correctly:

```bash
echo $GOOGLE_AI_API_KEY
echo $YOUTRACK_API_TOKEN
echo $YOUTRACK_URL
```

### Wrong or empty workflow results

- Ensure the provided `YOUTRACK_API_TOKEN` has permission to access `/api/admin/workflows`.
- Ensure `YOUTRACK_URL` points to your instance and is correct (no trailing slash required).

### LLM Token Limit Issues

If you see `MAX_TOKENS` errors:
1. The agent configuration includes `maxTokens = 8192` in `LLMParams`
2. If responses are still truncated, consider increasing the limit or simplifying prompts
3. Check agent logs for token usage information

### Build Errors

If you encounter dependency issues:

```bash
# Clean and rebuild with refreshed dependencies
./gradlew clean build --refresh-dependencies
```

## License

MIT
