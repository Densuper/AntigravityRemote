package com.example.antigravityremote

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.antigravityremote.theme.AntigravityRemoteTheme
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : FragmentActivity() {
    private val CHANNEL_PROJECT = "antigravity_project_alerts"
    private var activityTts: android.speech.tts.TextToSpeech? = null

    private val speechReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("text") ?: return
            speakText(text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannels()
        initTts()

        val filter = android.content.IntentFilter("com.example.antigravityremote.SPEAK")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(speechReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(speechReceiver, filter)
        }

        // Google Android 13+ Runtime Notification Permission Request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            AntigravityRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RemoteWebViewApp(
                        activity = this,
                        onShowProjectAlert = { title, msg -> showProjectActionNotification(title, msg) }
                    )
                }
            }
        }
    }

    private fun initTts() {
        activityTts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                activityTts?.language = Locale.UK
                activityTts?.setSpeechRate(1.0f)
                activityTts?.setPitch(0.95f)
            }
        }
    }

    fun speakText(text: String) {
        activityTts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "ag_speech_${System.currentTimeMillis()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(speechReceiver)
        } catch (_: Exception) {}
        activityTts?.stop()
        activityTts?.shutdown()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Chrome-Style Heads-Up Project Alert & Approval Notification Channel
            val projectChannel = NotificationChannel(
                CHANNEL_PROJECT,
                "Project Alerts & Approvals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chrome-style Heads-Up popup notifications for approvals and task alerts"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 80, 150)
            }

            notificationManager.createNotificationChannel(projectChannel)
        }
    }

    fun showProjectActionNotification(title: String, message: String) {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 200, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val agentPerson = androidx.core.app.Person.Builder()
            .setName("Antigravity AI")
            .setBot(true)
            .setImportant(true)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(agentPerson)
            .setConversationTitle(title)
            .addMessage(message, System.currentTimeMillis(), agentPerson)

        // Public version displayed on Lockscreen showing sender and full message
        val publicNotification = NotificationCompat.Builder(this, CHANNEL_PROJECT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Antigravity AI: $title")
            .setContentText(message)
            .setStyle(messagingStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // Full Messaging Notification for Notification Shade & Lockscreen
        val builder = NotificationCompat.Builder(this, CHANNEL_PROJECT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Antigravity AI: $title")
            .setContentText(message)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicNotification)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, false)
            .setAutoCancel(true)
            .setShowWhen(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, builder.build())

        // Relay live briefing data to paired Samsung Galaxy Watch Ultra over Bluetooth / Wi-Fi
        try {
            val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(this)
            val msgClient = com.google.android.gms.wearable.Wearable.getMessageClient(this)
            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                val path = if (title.contains("Approval", ignoreCase = true) || message.contains("approve", ignoreCase = true)) {
                    "/antigravity/approval"
                } else {
                    "/antigravity/task_update"
                }
                val payload = "$title: $message".toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    msgClient.sendMessage(node.id, path, payload)
                }
            }
        } catch (_: Exception) {}
    }
}

fun triggerHaptic(context: Context, durationMs: Long = 20) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    } catch (_: Exception) {}
}

fun isLocalOrTrustedUrl(rawUrl: String): Boolean {
    return try {
        val cleanUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "http://$rawUrl"
        } else {
            rawUrl
        }
        val uri = Uri.parse(cleanUrl)
        val host = uri.host ?: return false
        host == "localhost" ||
        host == "127.0.0.1" ||
        host.startsWith("192.168.") ||
        host.startsWith("10.") ||
        host.startsWith("172.") ||
        host.endsWith(".local") ||
        host.contains(":") ||
        host.matches(Regex("^[0-9.]+$"))
    } catch (_: Exception) {
        true
    }
}

class AndroidNativeBridge(
    private val context: Context,
    private val onNotify: (String, String) -> Unit,
    private val onSpeak: (String) -> Unit,
    private val onQuota: (Float, String, Float, String) -> Unit = { _, _, _, _ -> }
) {
    @JavascriptInterface
    fun showNotification(title: String, message: String) {
        onNotify(title, message)
    }

    @JavascriptInterface
    fun playTts(text: String) {
        onSpeak(text)
    }

    @JavascriptInterface
    fun updateQuota(fiveHourPct: Float, fiveHourReset: String, weeklyPct: Float, weeklyReset: String) {
        onQuota(fiveHourPct, fiveHourReset, weeklyPct, weeklyReset)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteWebViewApp(
    activity: FragmentActivity,
    onShowProjectAlert: (String, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ag_remote_prefs", Context.MODE_PRIVATE) }
    
    var remoteUrl by remember {
        mutableStateOf(prefs.getString("saved_remote_url", null))
    }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isTopDrawerOpen by remember { mutableStateOf(false) }
    var isAppLocked by remember { mutableStateOf(prefs.getBoolean("is_app_locked", false)) }
    var scanError by remember { mutableStateOf<String?>(null) }

    // Live Dynamic Quota & Rate Limit State (100% in sync with desktop companion)
    var fiveHourProgress by remember { mutableFloatStateOf(prefs.getFloat("five_hour_quota_pct", 0.88f)) }
    var fiveHourResetText by remember { mutableStateOf(prefs.getString("five_hour_reset_text", "3h 15m") ?: "3h 15m") }
    var weeklyProgress by remember { mutableFloatStateOf(prefs.getFloat("weekly_quota_pct", 0.94f)) }
    var weeklyResetText by remember { mutableStateOf(prefs.getString("weekly_reset_text", "5d 18h") ?: "5d 18h") }

    fun triggerBiometricUnlock() {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = androidx.biometric.BiometricPrompt(
            activity,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    triggerHaptic(context, 30)
                    isAppLocked = false
                    prefs.edit().putBoolean("is_app_locked", false).apply()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    triggerHaptic(context, 50)
                }
            }
        )

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Antigravity Remote")
            .setSubtitle("Authenticate with Biometrics or Device PIN")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(isAppLocked) {
        if (isAppLocked && remoteUrl != null) {
            triggerBiometricUnlock()
        }
    }

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        // Read directly from disk on launch
        val saved = prefs.getString("saved_remote_url", null)
        if (!saved.isNullOrBlank()) {
            remoteUrl = saved
        }
    }

    if (remoteUrl != null) {
        if (isAppLocked) {
            // Lock Screen UI with Biometric Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161B22))
                            .border(1.dp, Color(0xFF58A6FF).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔒", fontSize = 32.sp)
                    }

                    Text(
                        text = "Antigravity Workspace Locked",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Biometric authentication is required to access the active session.",
                        fontSize = 13.sp,
                        color = Color(0xFF8B949E),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { triggerBiometricUnlock() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.7f).height(46.dp)
                    ) {
                        Text("🔓 Unlock with Biometrics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        } else {
            BackHandler {
                if (isTopDrawerOpen) {
                    isTopDrawerOpen = false
                } else if (webViewInstance?.canGoBack() == true) {
                    webViewInstance?.goBack()
                } else {
                    activity.moveTaskToBack(true)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
            // Dedicated Top HUD Bar (Native status row above the web app, 0 overlap)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clickable {
                        triggerHaptic(context, 15)
                        isTopDrawerOpen = !isTopDrawerOpen
                    },
                color = Color(0xFF0D1117),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF30363D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3FB950))
                        )
                        Text(
                            text = "⚡ AGENT HUD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF58A6FF)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isTopDrawerOpen) "▲ Close Console" else "▼ Actions",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF58A6FF)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                FullscreenRemoteWebView(
                    url = remoteUrl!!,
                    onShowNotification = { title, msg -> onShowProjectAlert(title, msg) },
                    onUpdateQuota = { fPct, fReset, wPct, wReset ->
                        fiveHourProgress = fPct
                        fiveHourResetText = fReset
                        weeklyProgress = wPct
                        weeklyResetText = wReset
                        prefs.edit()
                            .putFloat("five_hour_quota_pct", fPct)
                            .putString("five_hour_reset_text", fReset)
                            .putFloat("weekly_quota_pct", wPct)
                            .putString("weekly_reset_text", wReset)
                            .apply()
                    },
                    onWebViewCreated = { webViewInstance = it }
                )

                // Tap outside scrim to close Action Drawer
                if (isTopDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    triggerHaptic(context, 15)
                                    isTopDrawerOpen = false
                                }
                            }
                    )
                }

                // Action Drawer with Cyber Frosted Glass & Visual Circular Gauges
                androidx.compose.animation.AnimatedVisibility(
                    visible = isTopDrawerOpen,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -15) { // Swipe UP gesture closes drawer!
                                    triggerHaptic(context, 15)
                                    isTopDrawerOpen = false
                                }
                            }
                        }
                ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0D1117).copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF58A6FF).copy(alpha = 0.35f)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Tap or Drag Pill to Close
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF8B949E).copy(alpha = 0.5f))
                                .clickable {
                                    triggerHaptic(context, 15)
                                    isTopDrawerOpen = false
                                }
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Header Status Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161B22))
                                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3FB950))
                                )
                                Column {
                                    Text("Antigravity Active Session", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("Neural Stream & On-Device TTS Online", fontSize = 11.sp, color = Color(0xFF3FB950))
                                }
                            }
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3FB950))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Control Action Buttons with Biometric Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    triggerHaptic(context, 20)
                                    webViewInstance?.reload()
                                    isTopDrawerOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔄 Resync", fontSize = 12.sp, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    triggerHaptic(context, 40)
                                    isAppLocked = true
                                    prefs.edit().putBoolean("is_app_locked", true).apply()
                                    isTopDrawerOpen = false
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF58A6FF).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🔒 Lock", fontSize = 12.sp, color = Color(0xFF58A6FF))
                                }
                            }

                            Button(
                                onClick = {
                                    triggerHaptic(context, 45)
                                    prefs.edit().remove("saved_remote_url").apply()
                                    prefs.edit().remove("is_app_locked").apply()
                                    isTopDrawerOpen = false
                                    remoteUrl = null
                                    isAppLocked = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDA3633)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("❌ Unpair", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            } // Close AnimatedVisibility
        } // Close Box
    } // Close Column
} // Close else (isAppLocked)
} else {
    ScannerScreen(
            scanError = scanError,
            onUrlScanned = { rawUrl ->
                val finalUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "http://$rawUrl"
                scanError = null
                triggerHaptic(context, 40)
                prefs.edit().putString("saved_remote_url", finalUrl).apply()
                remoteUrl = finalUrl
            }
        )
    }
}

@Composable
fun ScannerScreen(
    scanError: String?,
    onUrlScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Antigravity 2.0 Remote",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan desktop QR code to launch remote controller",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (scanError != null) {
            Text(
                text = scanError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CameraXBarcodeScanner(onUrlScanned = onUrlScanned)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission is required to scan the desktop QR code.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraXBarcodeScanner(onUrlScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    var scanned by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !scanned) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val raw = barcode.rawValue
                                    if (!raw.isNullOrBlank()) {
                                        scanned = true
                                        onUrlScanned(raw)
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullscreenRemoteWebView(
    url: String,
    onShowNotification: (String, String) -> Unit,
    onUpdateQuota: (Float, String, Float, String) -> Unit = { _, _, _, _ -> },
    onWebViewCreated: (WebView) -> Unit
) {
    var filePathCallback by remember { mutableStateOf<android.webkit.ValueCallback<Array<android.net.Uri>>?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    val context = LocalContext.current
    var ttsEngine by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ttsEngine?.language = Locale.UK
            }
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = true
                    allowContentAccess = true
                    mediaPlaybackRequiresUserGesture = false
                    setGeolocationEnabled(true)
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    safeBrowsingEnabled = false
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    blockNetworkLoads = false
                    userAgentString = "Mozilla/5.0 (Linux; Android 15; Pixel 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val swController = android.webkit.ServiceWorkerController.getInstance()
                    swController.setServiceWorkerClient(object : android.webkit.ServiceWorkerClient() {
                        override fun shouldInterceptRequest(request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                            return super.shouldInterceptRequest(request)
                        }
                    })
                }
                
                addJavascriptInterface(
                    AndroidNativeBridge(
                        context = ctx,
                        onNotify = onShowNotification,
                        onSpeak = { text ->
                            ttsEngine?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "ag_tts_${System.currentTimeMillis()}")
                        },
                        onQuota = onUpdateQuota
                    ),
                    "AndroidBridge"
                )

                android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                
                val chromeNotificationBridge = """
                    (function() {
                        function triggerNativeAlert(title, message) {
                            if (window.AndroidBridge && window.AndroidBridge.showNotification) {
                                window.AndroidBridge.showNotification(title || 'Antigravity Notification', message || '');
                            }
                        }

                        // 1. Force Google Chrome window.Notification Object BEFORE any scripts load
                        window.Notification = function(title, options) {
                            options = options || {};
                            var body = options.body || options.message || '';
                            triggerNativeAlert(title, body);
                            
                            this.title = title;
                            this.body = body;
                            this.close = function() {};
                            this.addEventListener = function() {};
                            this.removeEventListener = function() {};
                            
                            if (typeof options.onclick === 'function') {
                                this.onclick = options.onclick;
                            }
                        };
                        window.Notification.permission = 'granted';
                        window.Notification.requestPermission = function(callback) {
                            if (typeof callback === 'function') callback('granted');
                            return Promise.resolve('granted');
                        };

                        // 2. ServiceWorker Registration Hook
                        if (window.ServiceWorkerRegistration) {
                            window.ServiceWorkerRegistration.prototype.showNotification = function(title, options) {
                                options = options || {};
                                triggerNativeAlert(title, options.body || options.message || '');
                                return Promise.resolve();
                            };
                        }

                        // 3. Hook navigator.serviceWorker
                        if (navigator.serviceWorker) {
                            try {
                                navigator.serviceWorker.ready.then(function(reg) {
                                    if (reg) {
                                        reg.showNotification = function(title, options) {
                                            options = options || {};
                                            triggerNativeAlert(title, options.body || options.message || '');
                                            return Promise.resolve();
                                        };
                                    }
                                }).catch(function(){});
                            } catch(e){}
                        }

                        // 4. Hook EventSource & WebSocket streams
                        if (!window._agStreamHooked) {
                            window._agStreamHooked = true;
                            if (window.EventSource) {
                                var OrigEventSource = window.EventSource;
                                window.EventSource = function(url, config) {
                                    var es = new OrigEventSource(url, config);
                                    es.addEventListener('message', function(e) {
                                        try {
                                            var parsed = JSON.parse(e.data);
                                            if (parsed.type === 'notification' || parsed.type === 'turn_complete' || parsed.type === 'attention') {
                                                triggerNativeAlert(parsed.title || 'Antigravity Update', parsed.message || parsed.body || '');
                                            }
                                        } catch(err){}
                                    });
                                    es.addEventListener('notification', function(e) {
                                        try {
                                            var parsed = JSON.parse(e.data);
                                            triggerNativeAlert(parsed.title || 'Antigravity Notification', parsed.message || parsed.body || '');
                                        } catch(err){}
                                    });
                                    return es;
                                };
                                window.EventSource.prototype = OrigEventSource.prototype;
                            }

                            var OrigWebSocket = window.WebSocket;
                            window.WebSocket = function(url, protocols) {
                                var wsInstance = new OrigWebSocket(url, protocols);
                                wsInstance.addEventListener('message', function(e) {
                                    try {
                                        var parsed = JSON.parse(e.data);
                                        if (parsed.action === 'project_alert' || parsed.action === 'task_update' || parsed.action === 'notification' || parsed.type === 'notification') {
                                            triggerNativeAlert(parsed.title || 'Antigravity Alert', parsed.message || parsed.body || '');
                                        }
                                        // Live Dynamic Quota Broadcast Hook
                                        if (parsed.action === 'quota_update' && window.AndroidBridge && window.AndroidBridge.updateQuota) {
                                            var fPct = typeof parsed.five_hour_percent === 'number' ? parsed.five_hour_percent : 0.88;
                                            var fReset = parsed.five_hour_reset_text || '3h 15m';
                                            var wPct = typeof parsed.weekly_percent === 'number' ? parsed.weekly_percent : 0.94;
                                            var wReset = parsed.weekly_reset_text || '5d 18h';
                                            window.AndroidBridge.updateQuota(fPct, fReset, wPct, wReset);
                                        }
                                    } catch(err){}
                                });
                                return wsInstance;
                            };
                            window.WebSocket.prototype = OrigWebSocket.prototype;
                        }

                        // 5. DOM Observers for Turn Completion, Blue Dot & Approvals
                        if (!window._agAttentionListenerAttached) {
                            window._agAttentionListenerAttached = true;
                            var lastPrompt = '';
                            var lastTurnText = '';

                            setInterval(function() {
                                try {
                                    // A. Blue Dot indicator
                                    var blueDots = document.querySelectorAll('.unread-dot, .blue-dot, .notification-dot, [data-unread="true"], .has-unread, .unread-indicator, .status-dot.active, .dot-blue');
                                    if (blueDots.length > 0) {
                                        var activeDot = Array.from(blueDots).find(function(d) { return d.offsetParent !== null; });
                                        if (activeDot) {
                                            var dotLabel = (activeDot.getAttribute('aria-label') || activeDot.getAttribute('title') || 'New unread message from Antigravity Agent').slice(0, 100);
                                            if (dotLabel !== lastPrompt) {
                                                lastPrompt = dotLabel;
                                                triggerNativeAlert('🔵 Antigravity Agent Replied', dotLabel);
                                            }
                                        }
                                    }

                                    // B. Assistant message turn finish
                                    var assistantMessages = document.querySelectorAll('.assistant-message, .agent-response, .model-response, [data-role="assistant"], [data-role="model"], .turn-complete');
                                    if (assistantMessages.length > 0) {
                                        var latestAssistant = assistantMessages[assistantMessages.length - 1];
                                        var fullTxt = (latestAssistant.innerText || latestAssistant.textContent || '').trim();
                                        if (fullTxt.length > 10 && fullTxt !== lastTurnText) {
                                            lastTurnText = fullTxt;
                                            var snippet = fullTxt.slice(0, 130).replace(/\n+/g, ' ');
                                            triggerNativeAlert('⚡ Agent Finished Turn', snippet);
                                        }
                                    }

                                    // C. Approval buttons
                                    var buttons = Array.from(document.querySelectorAll('button, .btn, [role="button"]'));
                                    var approvalBtn = buttons.find(function(b) {
                                        var t = (b.innerText || b.textContent || '').trim().toLowerCase();
                                        return t === 'proceed' || t === 'allow' || t === 'approve' || t === 'run command' || t === 'accept';
                                    });

                                    if (approvalBtn && approvalBtn.offsetParent !== null) {
                                        var promptText = 'Agent requires approval to proceed with execution.';
                                        var parentModal = approvalBtn.closest('.modal, .dialog, [role="dialog"], .action-box, .turn-bubble, .card');
                                        if (parentModal) {
                                            promptText = (parentModal.innerText || parentModal.textContent || '').trim().slice(0, 140);
                                        }
                                        if (promptText !== lastPrompt) {
                                            lastPrompt = promptText;
                                            triggerNativeAlert('⚠️ Attention Required', promptText);
                                        }
                                    }

                                    // D. Continuous Autonomous Background Settings & Models Quota Probe
                                    function probeSettingsModelsQuota() {
                                        try {
                                            // 1. Check if a hidden probe iframe already exists
                                            var probeFrame = document.getElementById('_agQuotaProbeFrame');
                                            if (!probeFrame) {
                                                probeFrame = document.createElement('iframe');
                                                probeFrame.id = '_agQuotaProbeFrame';
                                                probeFrame.style.display = 'none';
                                                probeFrame.style.width = '0px';
                                                probeFrame.style.height = '0px';
                                                probeFrame.style.position = 'absolute';
                                                probeFrame.style.top = '-9999px';
                                                document.body.appendChild(probeFrame);
                                            }

                                            // 2. Load settings/models route into invisible background iframe
                                            probeFrame.onload = function() {
                                                try {
                                                    var frameDoc = probeFrame.contentDocument || probeFrame.contentWindow.document;
                                                    if (frameDoc && frameDoc.body) {
                                                        var frameText = frameDoc.body.innerText || '';
                                                        var pctM = frameText.match(/(\d{1,3})%\s*(?:remaining|left|available|quota)?/i);
                                                        var resetM = frameText.match(/(?:resets? in|reset:?|in)\s*([\d\w\s]+?)(?:\n|\.|\)|$)/i);
                                                        if (pctM && window.AndroidBridge && window.AndroidBridge.updateQuota) {
                                                            var n = parseInt(pctM[1], 10);
                                                            if (!isNaN(n) && n >= 0 && n <= 100) {
                                                                var p = n / 100.0;
                                                                var r = resetM ? resetM[1].slice(0, 14).trim() : 'Auto';
                                                                window.AndroidBridge.updateQuota(p, r, p, r);
                                                            }
                                                        }
                                                    }
                                                } catch(e){}
                                            };

                                            // Target internal settings paths
                                            var baseUrl = window.location.origin;
                                            probeFrame.src = baseUrl + '/settings/models';
                                        } catch(e){}
                                    }

                                    // Trigger immediately upon pairing / launch, then continuously every 60 seconds
                                    if (!window._agProbeInitiated) {
                                        window._agProbeInitiated = true;
                                        setTimeout(probeSettingsModelsQuota, 2000);
                                        setInterval(probeSettingsModelsQuota, 60000);
                                    }

                                    // Active DOM Scanner (If user opens modals or switches models in chat)
                                    var allText = (document.body ? document.body.innerText || '' : '');
                                    var pctMatches = allText.match(/(\d{1,3})%\s*(?:remaining|left|available|quota)?/i);
                                    var timeMatches = allText.match(/(?:resets? in|reset:?|in)\s*([\d\w\s]+?)(?:\n|\.|\)|$)/i);
                                    
                                    if (pctMatches && window.AndroidBridge && window.AndroidBridge.updateQuota) {
                                        var num = parseInt(pctMatches[1], 10);
                                        if (!isNaN(num) && num >= 0 && num <= 100) {
                                            var pctFloat = num / 100.0;
                                            var resetStr = timeMatches ? timeMatches[1].slice(0, 14).trim() : 'Auto';
                                            window.AndroidBridge.updateQuota(pctFloat, resetStr, pctFloat, resetStr);
                                        }
                                    }
                                } catch(err){}
                            }, 1000);
                        }
                    })();
                """.trimIndent()

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript(chromeNotificationBridge, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(chromeNotificationBridge, null)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                        request?.grant(request.resources)
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: android.webkit.GeolocationPermissions.Callback?
                    ) {
                        callback?.invoke(origin, true, false)
                    }

                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                        return super.onJsAlert(view, url, message, result)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallbackParam: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = filePathCallbackParam
                        val mimeTypes = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }?.toTypedArray()
                        val typesToUse = if (!mimeTypes.isNullOrEmpty()) mimeTypes else arrayOf("*/*")
                        filePickerLauncher.launch(typesToUse)
                        return true
                    }
                }
                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { /* Keep WebView instance warm in memory across lifecycle switches */ },
        modifier = Modifier.fillMaxSize()
    )
}
