package com.murcafes.notcatg.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupData(val topics: List<Topic>, val resources: List<Resource>)
data class RestoreResult(val topicsAdded: Int, val resourcesAdded: Int)

object BackupCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    fun encode(data: BackupData): String = JSONObject().apply {
        put("format", "com.murcafes.notcatg.backup")
        put("version", 1)
        put("topics", JSONArray().apply { data.topics.forEach { topic -> put(JSONObject().apply {
            put("id", topic.id); put("name", topic.name); put("description", topic.description); put("createdAt", topic.createdAt)
        }) } })
        put("resources", JSONArray().apply { data.resources.forEach { resource -> put(JSONObject().apply {
            put("id", resource.id); put("topicId", resource.topicId); put("type", resource.type)
            put("reference", resource.reference); put("text", resource.text); put("source", resource.source)
            put("personalNote", resource.personalNote); put("favorite", resource.favorite)
            put("createdAt", resource.createdAt); put("updatedAt", resource.updatedAt)
        }) } })
    }.toString(2)

    fun decode(text: String): BackupData {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "El respaldo supera 5 MB." }
        val root = JSONObject(text)
        require(root.getString("format") == "com.murcafes.notcatg.backup" && root.getInt("version") == 1) {
            "El archivo no es un respaldo compatible de Notcatg."
        }
        val topicsJson = root.getJSONArray("topics")
        val resourcesJson = root.getJSONArray("resources")
        require(topicsJson.length() + resourcesJson.length() <= 20000) { "El respaldo contiene demasiados registros." }
        val topics = (0 until topicsJson.length()).map { index ->
            val item = topicsJson.getJSONObject(index)
            Topic(item.getLong("id"), item.getString("name"), item.getString("description"), item.getLong("createdAt"))
        }
        val resources = (0 until resourcesJson.length()).map { index ->
            val item = resourcesJson.getJSONObject(index)
            require(item.get("favorite") is Boolean)
            Resource(item.getLong("id"), item.getLong("topicId"), item.getString("type"), item.getString("reference"),
                item.getString("text"), item.getString("source"), item.getString("personalNote"), item.getBoolean("favorite"),
                item.getLong("createdAt"), item.getLong("updatedAt"))
        }
        require(topics.all { it.id > 0 && it.name.isNotBlank() && it.createdAt >= 0 })
        require(topics.map { it.id }.distinct().size == topics.size)
        val topicIds = topics.map { it.id }.toSet()
        require(resources.all { it.id > 0 && it.topicId in topicIds && hasResourceContent(it.reference, it.text, it.personalNote) && it.createdAt >= 0 && it.updatedAt >= 0 })
        require(resources.map { it.id }.distinct().size == resources.size)
        return BackupData(topics, resources)
    }
}
