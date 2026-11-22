package com.example.monitor

import com.example.domain.ParsedOcrItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LiveMonitorState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val autoScanEnabled: Boolean = true,
    val autoSaveToVault: Boolean = true,
    val scanIntervalMs: Long = 1500L,
    val hasOverlayBubble: Boolean = false,
    val framesAnalyzed: Int = 0,
    val itemsLoggedCount: Int = 0,
    val statusMessage: String = "Monitor idle. Tap Start to monitor Evil Hunter Tycoon on-demand.",
    val lastDetectedItem: ParsedOcrItem? = null,
    val lastDetectedVerdict: String? = null,
    val recentDetections: List<ParsedOcrItem> = emptyList(),
    val lastHunterUpdateSummary: String? = null,
    val requestedAppTab: Int? = null
)

object LiveMonitorBus {
    private val _state = MutableStateFlow(LiveMonitorState())
    val state: StateFlow<LiveMonitorState> = _state.asStateFlow()

    fun update(transform: (LiveMonitorState) -> LiveMonitorState) {
        _state.update(transform)
    }

    fun clearDetections() {
        _state.update {
            it.copy(
                recentDetections = emptyList(),
                lastDetectedItem = null,
                lastDetectedVerdict = null,
                itemsLoggedCount = 0
            )
        }
    }

    fun consumeRequestedTab() {
        _state.update { it.copy(requestedAppTab = null) }
    }
}
