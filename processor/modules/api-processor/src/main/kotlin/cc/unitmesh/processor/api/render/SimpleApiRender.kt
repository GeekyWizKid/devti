package cc.unitmesh.processor.api.render

import cc.unitmesh.processor.api.base.ApiDetailRender
import cc.unitmesh.processor.api.base.ApiItem
import cc.unitmesh.processor.api.base.ApiTagOutput

class SimpleApiRender : ApiDetailRender {
    override fun renderCollection(apiItems: List<ApiItem>): String {
        val apiDetailsByTag = renderByTag(apiItems)
        return apiDetailsByTag.joinToString("\n\n") { it.toString() }
    }

    override fun renderItem(tags: List<String>, apiItems: List<ApiItem>): ApiTagOutput {
        val tag = tags.joinToString(", ")
        val apiDetailsString = apiItems.joinToString("\n") {
            "${it.method} ${it.path} ${operationInformation(it)} "
        }

        return ApiTagOutput("$tag\n$apiDetailsString")
    }

    private fun operationInformation(it: ApiItem): String {
        if (it.operationId.isEmpty()) return ""

        return " ${it.operationId}${ioParameters(it)}"
    }

    private fun ioParameters(details: ApiItem): String {
        val inputs = details.request.toString()
        val outputs = details.response.toString()
        if (inputs.isEmpty() && outputs.isEmpty()) return "()"
        if (inputs.isEmpty()) return "(): $outputs"
        if (outputs.isEmpty()) return "($inputs)"

        return "(${inputs}) : $outputs"
    }
}