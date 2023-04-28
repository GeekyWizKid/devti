package cc.unitmesh.processor.api.base

import kotlinx.serialization.Serializable


@Serializable
data class ApiTagOutput(val string: String) {
    override fun toString() = string
}

@Serializable
data class Parameter(
    val name: String,
    val type: String,
) {
    override fun toString() = "$name: $type"
}

@Serializable
data class Request(
    val parameters: List<Parameter> = listOf(),
    val body: List<Parameter> = listOf(),
) {
    override fun toString(): String {
        val params = parameters.joinToString(", ") { it.toString() }
        val body = body.joinToString(", ") { it.toString() }
        if (params.isEmpty() && body.isEmpty()) return ""
        if (params.isEmpty()) return body
        if (body.isEmpty()) return params

        return "$params, ($body)"
    }
}

@Serializable
data class Response(
    val parameters: List<Parameter> = listOf()
) {
    override fun toString() = parameters.joinToString(", ") { it.toString() }
}

@Serializable
data class ApiDetail(
    val path: String,
    val method: String,
    val summary: String,
    val operationId: String,
    val tags: List<String>,
    val request: Request? = null,
    val response: Response? = null,
)