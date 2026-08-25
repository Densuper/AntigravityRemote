package com.example.antigravityremote.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearMessageListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val text = String(messageEvent.data, Charsets.UTF_8)
        when (path) {
            "/antigravity/task_update" -> {
                WearRelayState.taskStatus.value = "⚡ Task Finished"
                WearRelayState.taskDetails.value = text
                WearRelayState.isActionRequired.value = false
            }
            "/antigravity/approval" -> {
                WearRelayState.taskStatus.value = "⚠️ Review & Approval"
                WearRelayState.taskDetails.value = text
                WearRelayState.isActionRequired.value = true
            }
            "/antigravity/status" -> {
                WearRelayState.taskStatus.value = text
            }
        }
    }
}
