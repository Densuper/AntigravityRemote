package com.example.antigravityremote.wear

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object WearRelayState {
    val taskStatus = MutableStateFlow("Ready & Connected")
    val taskDetails = MutableStateFlow("Antigravity 2.0 paired with phone. Standing by for workspace briefings.")
    val isActionRequired = MutableStateFlow(false)
}

class WearMainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Native Watch TTS Engine (Speaks from Watch Speaker / Earbuds)
        tts = TextToSpeech(this, this)

        setContent {
            WearApp(
                onApprove = { sendResponseToPhone("/antigravity/action", "APPROVE") },
                onCancel = { sendResponseToPhone("/antigravity/action", "CANCEL") }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.UK
            ttsReady = true
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path
        val text = String(event.data, Charsets.UTF_8)

        when (path) {
            "/antigravity/task_update" -> {
                WearRelayState.taskStatus.value = "⚡ Task Finished"
                WearRelayState.taskDetails.value = text
                WearRelayState.isActionRequired.value = false
                vibrateWatch(100)
                speakOnWatch(text)
            }
            "/antigravity/approval" -> {
                WearRelayState.taskStatus.value = "⚠️ Review & Approval"
                WearRelayState.taskDetails.value = text
                WearRelayState.isActionRequired.value = true
                vibrateWatch(200)
                speakOnWatch("Approval required on your wrist: $text")
            }
            "/antigravity/status" -> {
                WearRelayState.taskStatus.value = text
            }
        }
    }

    private fun speakOnWatch(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "watch_tts_id")
        }
    }

    private fun vibrateWatch(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun sendResponseToPhone(path: String, msg: String) {
        val client = Wearable.getNodeClient(this)
        client.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Wearable.getMessageClient(this).sendMessage(node.id, path, msg.toByteArray(Charsets.UTF_8))
            }
        }
    }
}

@Composable
fun WearApp(
    onApprove: () -> Unit,
    onCancel: () -> Unit
) {
    val status by WearRelayState.taskStatus.asStateFlow().collectAsState()
    val details by WearRelayState.taskDetails.asStateFlow().collectAsState()
    val actionRequired by WearRelayState.isActionRequired.asStateFlow().collectAsState()

    val scalingLazyListState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = scalingLazyListState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = scalingLazyListState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 28.dp)
        ) {
            item {
                Text(
                    text = "ANTIGRAVITY 2.0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF58A6FF),
                    textAlign = TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = {},
                    label = {
                        Text(
                            text = status,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3FB950)
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(
                        backgroundColor = Color(0xFF161B22)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = details,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = Color.White
                    )
                }
            }

            if (actionRequired) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF238636)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("👍 Approve", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.secondaryButtonColors(backgroundColor = Color(0xFFDA3633)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("❌ Deny", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
