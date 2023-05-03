package cc.unitmesh.processor.api.parser

import cc.unitmesh.core.model.ApiCollection
import cc.unitmesh.core.model.ApiItem
import cc.unitmesh.core.model.BodyMode
import cc.unitmesh.core.model.Parameter
import cc.unitmesh.core.model.Request
import cc.unitmesh.core.model.Response
import cc.unitmesh.processor.api.model.postman.*
import java.net.URI

sealed class ChildType {
    class NestedFolder(val folders: List<Folder>, val items: List<Item>) : ChildType()

    class Folder(val collection: ApiCollection) : ChildType()

    class Item(val items: List<ApiItem>) : ChildType()
}

class PostmanParser {
    private val `var`: PostmanVariables = PostmanVariables(PostmanEnvironment())
    fun parse(collection: PostmanCollection): List<ApiCollection>? {
        return collection.item?.map {
            parseFolder(it, it.name)
        }?.flatten()
    }

    private fun parseFolder(item: PostmanFolder, folderName: String?): List<ApiCollection> {
        val details: MutableList<ApiCollection> = mutableListOf()
        if (item.item != null) {
            val childTypes = item.item.map {
                parseChildItem(it, folderName, item.name)
            }.flatten()

            childTypes.filterIsInstance<ChildType.Folder>().forEach {
                details.add(it.collection)
            }

            childTypes.filterIsInstance<ChildType.NestedFolder>().forEach {
                val folder = it.folders.map { it.collection }
                details.addAll(folder)

                val items = it.items.map { it.items }.flatten()
                details.add(ApiCollection(folderName ?: "", "", items))
            }

            val items = childTypes.filterIsInstance<ChildType.Item>().map { it.items }.flatten()
            if (items.isNotEmpty()) {
                val descriptionName = if (folderName == item.name) {
                    ""
                } else {
                    item.name ?: ""
                }

                details.add(ApiCollection(folderName ?: "", descriptionName, items))
            }
        } else if (item.request != null) {
            val apiItems = processApiItem(item as PostmanItem, folderName, item.name)?.let {
                listOf(it)
            } ?: listOf()

            val descriptionName = if (folderName == item.name) {
                ""
            } else {
                item.name ?: ""
            }

            details.add(ApiCollection(folderName ?: "", descriptionName, apiItems))
        }

        return details
    }

    private fun parseChildItem(subItem: PostmanItem, folderName: String?, itemName: String?): List<ChildType> {
        return when {
            subItem.item != null -> {
                val childTypes = subItem.item!!.map {
                    parseChildItem(it, folderName, itemName)
                }.flatten()

                val folder = childTypes.filterIsInstance<ChildType.Folder>()
                val items = childTypes.filterIsInstance<ChildType.Item>()

                if (folder.isNotEmpty() && items.isNotEmpty()) {
                    return listOf(ChildType.NestedFolder(folder, items))
                } else if (items.size == subItem.item!!.size) {
                    val collection =
                        ApiCollection(folderName ?: "", subItem.name ?: "", items.map { it.items }.flatten())
                    return listOf(ChildType.Folder(collection))
                }

                return childTypes
            }

            subItem.request != null -> {
                val apiItems = processApiItem(subItem, folderName, itemName)?.let {
                    listOf(it)
                } ?: listOf()

                listOf(ChildType.Item(apiItems))
            }

            else -> {
                listOf()
            }
        }
    }

    private fun processApiItem(
        subItem: PostmanItem,
        folderName: String?,
        itemName: String?,
    ): ApiItem? {
        val request = subItem.request
        val url: PostmanUrl? = request?.url
        val method = request?.method
        val body = request?.body
        val description = request?.description
        val name = subItem.name

        var uri = request?.getUrl(`var`)
        uri = uri?.replace("http://UNDEFINED", "")
            ?.replace("https://UNDEFINED", "")
            ?.replace("UNDEFINED", "{}")

        try {
            val uriObj = URI(uri)
            uri = uriObj.path
        } catch (e: Exception) {
            // ignore
        }

        if (uri?.startsWith("/") == false) {
            uri = uri.substring(uri.indexOf("/"))
        }

        val responses = subItem.response?.map {
            Response(
                code = it.code ?: 0,
                parameters = listOf(),
                bodyMode = BodyMode.RAW_TEXT,
                bodyString = it.body ?: "",
            )
        }?.toList() ?: listOf()

        val req = Request(
            parameters = urlParameters(url),
            body = body?.formdata?.map { Parameter(it.key ?: "", it.value ?: "") } ?: listOf(),
        )

        if (uri?.isEmpty() == true) {
            return null
        }

        return ApiItem(
            method = method ?: "",
            path = uri ?: "",
            description = description.replaceLineBreak() ?: "",
            operationId = name ?: "",
            tags = listOf(folderName ?: "", itemName ?: ""),
            request = req,
            response = responses,
        )
    }

    private fun urlParameters(url: PostmanUrl?): List<Parameter> {
        val variable = url?.variable?.map {
            Parameter(it.key ?: "", formatValue(it.value))
        }

        val queries = url?.query?.map {
            Parameter(it.key ?: "", formatValue(it.value))
        }

        return (variable ?: listOf()) + (queries ?: listOf())
    }

    private fun formatValue(it: String?): String {
        val regex = Regex("^\\d+$")
        val boolRegex = Regex("^(true|false)$")

        return when {
            it?.matches(regex) == true -> {
                it
            }

            it?.matches(boolRegex) == true -> {
                it
            }

            (it?.length ?: 0) > 0 -> {
                "\"$it\""
            }

            else -> ""
        }
    }
}

private fun String?.replaceLineBreak(): String? {
    return this?.replace("\n", "")?.replace("\r", "")
}
