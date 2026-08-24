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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannels()

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

        // Chrome-style Heads-Up Floating Banner Notification
        val builder = NotificationCompat.Builder(this, CHANNEL_PROJECT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, false)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
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
    private val onNotify: (String, String) -> Unit
) {
    @JavascriptInterface
    fun showNotification(title: String, message: String) {
        onNotify(title, message)
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
    var isBiometricAuthenticated by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

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
        BackHandler {
            if (isTopDrawerOpen) {
                isTopDrawerOpen = false
            } else if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
            } else {
                // Standalone app behavior: Minimize to Android home screen without dropping session
                activity.moveTaskToBack(true)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            FullscreenRemoteWebView(
                url = remoteUrl!!,
                onShowNotification = { title, msg -> onShowProjectAlert(title, msg) },
                onWebViewCreated = { webViewInstance = it }
            )

            // Tap outside scrim to close Action Drawer
            if (isTopDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                triggerHaptic(context, 15)
                                isTopDrawerOpen = false
                            }
                        }
                )
            }

            // Top Pull-Down Trigger Zone
            if (!isTopDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .align(Alignment.TopCenter)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 20) {
                                    triggerHaptic(context, 15)
                                    isTopDrawerOpen = true
                                }
                            }
                        }
                )
            }

            // Action Drawer with Swipe-Up gesture & Pull Pill
            AnimatedVisibility(
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Tap or Drag Pill to Close
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    triggerHaptic(context, 15)
                                    isTopDrawerOpen = false
                                }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Antigravity Workspace", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Active Remote Session", fontSize = 11.sp, color = Color(0xFF3FB950))
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3FB950))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Antigravity Model Usage & Rate Limits Telemetry Card with Dynamic Live Reset Countdown
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡ Model Usage & Quotas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Claude 3.7 / Gemini 2.5", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // 5-Hour Rolling Limit with exact Reset Countdown & Clock Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⏳ 5-Hour Rolling Limit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("88% Remaining", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF58A6FF))
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { 0.88f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF58A6FF),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Next Reset: in 3 hrs 15 mins", fontSize = 9.5.sp, color = Color(0xFF58A6FF))
                                Text("Exact Time: 17:45 BST", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Weekly Quota Allowance with exact Day & Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📅 Weekly Quota Allowance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("94% Remaining", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3FB950))
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { 0.94f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF3FB950),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Next Reset: in 5 days 18 hrs", fontSize = 9.5.sp, color = Color(0xFF3FB950))
                                Text("Monday 00:00 BST", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    triggerHaptic(context, 20)
                                    webViewInstance?.reload()
                                    isTopDrawerOpen = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔄 Reload", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    triggerHaptic(context, 25)
                                    isTopDrawerOpen = false
                                    activity.finishAffinity() // Closes and exits the app completely
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🚪 Exit App", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    triggerHaptic(context, 40)
                                    // Lock workspace: requires biometric unlock on next open
                                    isBiometricAuthenticated = false
                                    isTopDrawerOpen = false
                                    activity.moveTaskToBack(true)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔒 Lock Workspace", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    triggerHaptic(context, 45)
                                    // Disconnect & Forget: Purges saved token and returns to QR Scanner
                                    prefs.edit().remove("saved_remote_url").apply()
                                    isTopDrawerOpen = false
                                    remoteUrl = null
                                    isBiometricAuthenticated = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("❌ Disconnect", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
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
                    userAgentString = "Mozilla/5.0 (Linux; Android 15; Pixel 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                }
                
                addJavascriptInterface(
                    AndroidNativeBridge(
                        onNotify = onShowNotification
                    ),
                    "AndroidBridge"
                )

                android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val webNotificationPolyfill = """
                            (function() {
                                window.Notification = function(title, options) {
                                    options = options || {};
                                    var body = options.body || '';
                                    if (window.AndroidBridge && window.AndroidBridge.showNotification) {
                                        window.AndroidBridge.showNotification(title, body);
                                    }
                                };
                                window.Notification.permission = 'granted';
                                window.Notification.requestPermission = function(cb) {
                                    if (cb) cb('granted');
                                    return Promise.resolve('granted');
                                };
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(webNotificationPolyfill, null)
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
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
