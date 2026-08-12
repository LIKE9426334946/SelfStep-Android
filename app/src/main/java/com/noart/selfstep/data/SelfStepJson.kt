package com.noart.selfstep.data

import com.noart.selfstep.model.DailyRecord
import com.noart.selfstep.model.DailyTaskStatus
import com.noart.selfstep.model.DisciplineTask
import com.noart.selfstep.model.SelfStepData
import com.noart.selfstep.model.TaskType
import org.json.JSONArray
import org.json.JSONObject

object SelfStepJson {
    fun encode(data: SelfStepData): String {
        val root = JSONObject()
            .put("version", data.version)
            .put("tasks", JSONArray().apply {
                data.tasks.forEach { task ->
                    put(
                        JSONObject()
                            .put("id", task.id)
                            .put("title", task.title)
                            .put("type", task.type.name)
                            .put("createdAt", task.createdAt)
                    )
                }
            })
            .put("records", JSONArray().apply {
                data.records.values.sortedBy { it.date }.forEach { record ->
                    put(
                        JSONObject()
                            .put("date", record.date)
                            .put("tasks", JSONArray().apply {
                                record.tasks.forEach { status ->
                                    put(
                                        JSONObject()
                                            .put("taskId", status.taskId)
                                            .put("title", status.title)
                                            .put("type", status.type.name)
                                            .put("completed", status.completed)
                                    )
                                }
                            })
                    )
                }
            })

        return root.toString(2)
    }

    fun decode(json: String): SelfStepData {
        val root = JSONObject(json)
        val tasks = root.optJSONArray("tasks").toObjectList { item ->
            DisciplineTask(
                id = item.getString("id"),
                title = item.getString("title"),
                type = item.optTaskType(),
                createdAt = item.optLong("createdAt", 0L)
            )
        }
        val records = root.optJSONArray("records").toObjectList { item ->
            val date = item.getString("date")
            DailyRecord(
                date = date,
                tasks = item.optJSONArray("tasks").toObjectList { status ->
                    DailyTaskStatus(
                        taskId = status.getString("taskId"),
                        title = status.getString("title"),
                        type = status.optTaskType(),
                        completed = status.optBoolean("completed", false)
                    )
                }
            )
        }.associateBy { it.date }

        return SelfStepData(
            version = root.optInt("version", 1),
            tasks = tasks,
            records = records
        )
    }

    private fun JSONObject.optTaskType(): TaskType =
        runCatching { TaskType.valueOf(optString("type", TaskType.MUST_DO.name)) }
            .getOrDefault(TaskType.MUST_DO)

    private inline fun <T> JSONArray?.toObjectList(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optJSONObject(index)?.let { add(transform(it)) }
            }
        }
    }
}
