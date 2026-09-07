package com.pureqr.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pureqr.app.data.HistoryDataStore
import com.pureqr.app.model.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val historyDataStore = HistoryDataStore(application)
    
    val historyItems: StateFlow<List<HistoryItem>> = historyDataStore.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addToHistory(item: HistoryItem) {
        viewModelScope.launch {
            historyDataStore.addHistoryItem(item)
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            historyDataStore.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyDataStore.clearHistory()
        }
    }

    fun exportHistoryToCsv(history: List<HistoryItem>): String {
        val builder = StringBuilder("Content,Type,Timestamp,Source\n")
        for (item in history) {
            val source = if (item.isGenerated) "Generated" else "Scanned"
            val content = item.content.replace("\"", "\"\"")
            builder.append("\"$content\",${item.type.name},${item.timestamp},$source\n")
        }
        return builder.toString()
    }

    fun exportHistoryToJson(history: List<HistoryItem>): String {
        val orgArray = org.json.JSONArray()
        for (item in history) {
            val obj = org.json.JSONObject()
            obj.put("content", item.content)
            obj.put("type", item.type.name)
            obj.put("timestamp", item.timestamp)
            obj.put("source", if (item.isGenerated) "Generated" else "Scanned")
            orgArray.put(obj)
        }
        return orgArray.toString(4)
    }
}
