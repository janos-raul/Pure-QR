package com.pureqr.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pureqr.app.model.HistoryItem
import com.pureqr.app.model.QrType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history_prefs")

class HistoryDataStore(private val context: Context) {

    private val HISTORY_KEY = stringPreferencesKey("qr_history")

    val historyFlow: Flow<List<HistoryItem>> = context.historyDataStore.data
        .map { preferences ->
            val historyJson = preferences[HISTORY_KEY] ?: "[]"
            parseHistoryJson(historyJson)
        }

    suspend fun addHistoryItem(item: HistoryItem) {
        context.historyDataStore.edit { preferences ->
            val currentHistoryJson = preferences[HISTORY_KEY] ?: "[]"
            val currentHistory = parseHistoryJson(currentHistoryJson).toMutableList()
            
            // Check for duplicates (same content and type)
            val existingIndex = currentHistory.indexOfFirst { it.content == item.content && it.type == item.type }
            if (existingIndex != -1) {
                currentHistory.removeAt(existingIndex)
            }
            
            currentHistory.add(0, item) // Add to the top
            
            // Limit history to 100 items
            val limitedHistory = currentHistory.take(100)
            preferences[HISTORY_KEY] = serializeHistory(limitedHistory)
        }
    }

    suspend fun deleteHistoryItem(id: String) {
        context.historyDataStore.edit { preferences ->
            val currentHistoryJson = preferences[HISTORY_KEY] ?: "[]"
            val currentHistory = parseHistoryJson(currentHistoryJson).toMutableList()
            currentHistory.removeAll { it.id == id }
            preferences[HISTORY_KEY] = serializeHistory(currentHistory)
        }
    }

    suspend fun clearHistory() {
        context.historyDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = "[]"
        }
    }

    private fun parseHistoryJson(json: String): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.getString("id"),
                        content = obj.getString("content"),
                        type = QrType.valueOf(obj.getString("type")),
                        timestamp = obj.getLong("timestamp"),
                        isGenerated = obj.optBoolean("isGenerated", true)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeHistory(history: List<HistoryItem>): String {
        val jsonArray = JSONArray()
        for (item in history) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("content", item.content)
            obj.put("type", item.type.name)
            obj.put("timestamp", item.timestamp)
            obj.put("isGenerated", item.isGenerated)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
