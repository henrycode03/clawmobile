package com.user.ui

object FailureSummary {

    fun summarizeTaskFailure(error: String?, result: String?): String? {
        val combined = listOfNotNull(error, result)
            .joinToString("\n")
            .trim()
        if (combined.isBlank()) return null

        val text = combined.lowercase()
        return when {
            "planning result is not" in text || "parse planning result" in text ->
                "Planning output format mismatch. The model returned a plan shape Orchestrator could not use directly."
            "context window exceeded" in text || ("context" in text && "exceeded" in text) ->
                "Prompt exceeded the model context window. The task may need a smaller prompt or fewer files in context."
            "timed out" in text || "already running too long" in text || "timeout" in text ->
                "Execution timed out before the task could finish. Breaking the task into smaller steps is the safest next move."
            "checkpoint" in text && "resume" in text ->
                "Recovery flow hit a checkpoint or resume problem. Check the latest usable checkpoint before retrying."
            "workspace isolation" in text || "path escapes task workspace" in text ->
                "The task tried to read or write outside its allowed workspace."
            "permission denied" in text ->
                "A command failed because the workspace or tool permissions were not sufficient."
            "no attribute" in text || "nonetype" in text ->
                "The run hit an internal orchestration bug rather than a normal task error."
            else ->
                "The task failed, but the exact cause still needs log review."
        }
    }

    fun summarizeSessionFailure(logs: List<String>): String? {
        if (logs.isEmpty()) return null
        val combined = logs.joinToString("\n").trim()
        if (combined.isBlank()) return null

        val text = combined.lowercase()
        return when {
            "failed to parse planning result" in text ->
                "Planning failed because the returned plan format did not match what Orchestrator expected."
            "timed out" in text || "timeout" in text ->
                "The session appears to have stopped on a timeout."
            "checkpoint" in text && "error" in text ->
                "The session hit an error and saved a checkpoint for recovery."
            "revising" in text || "plan revised" in text ->
                "The run needed to revise its plan after hitting an execution problem."
            "failed" in text || "error" in text ->
                "The session hit an execution error. Review the Errors filter for the exact failing step."
            else ->
                null
        }
    }
}