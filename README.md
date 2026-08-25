<div align="center">

<img src="./assets/banner.png" alt="Antigravity Remote Hero Banner" width="100%" style="border-radius: 12px; margin-bottom: 20px;" />

# 🚀 Antigravity Remote

**The Next-Generation Mobile Companion & Telemetry Console for Antigravity AI**

[![GitHub Release](https://img.shields.io/github/v/release/Densuper/AntigravityRemote?style=for-the-badge&color=00D2FF&label=Latest%20Release)](https://github.com/Densuper/AntigravityRemote/releases/latest)
[![Download APK](https://img.shields.io/badge/Download-APK%20v1.0.0-38EF7D?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Densuper/AntigravityRemote/releases/download/v1.0.0/AntigravityRemote-v1.0.0.apk)
[![License: MIT](https://img.shields.io/badge/License-MIT-FEE140?style=for-the-badge)](https://opensource.org/licenses/MIT)

<br/>

[![Android Target SDK](https://img.shields.io/badge/Target%20SDK-36%20(Android%2015)-blue.svg)](https://developer.android.com)
[![16KB Page Size](https://img.shields.io/badge/Android%2015-16KB%20Page%20Aligned-purple.svg)](https://developer.android.com/guide/practices/page-sizes)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)

---

### 📲 [⚡ **Download Latest Release (v1.0.0 APK)** ⚡](https://github.com/Densuper/AntigravityRemote/releases/tag/v1.0.0)

Monitor AI agent workflows, review diffs, approve tool executions, and track real-time LLM token quotas directly from your Android phone with zero latency.

</div>

---

## 🌟 Key Features

- 📷 **Instant CameraX QR Pairing**: Zero-configuration setup. Scan the dynamic QR code displayed on your Antigravity IDE terminal/companion to securely bind mobile and desktop hosts via encrypted handshakes.
- 🔒 **Biometric Security & Hardware Keystore**: Protect your agent remote controls and approvals behind hardware-backed Android `BiometricPrompt` (Fingerprint & Face Unlock) with cryptographic token storage.
- ⚡ **Real-Time Live Agent Streaming**: Track multi-turn subagent execution, terminal commands, workspace edits, and background tasks in real time over low-latency WebSockets.
- 📊 **Model Quotas & Token Telemetry**: Dedicated dashboard tracking daily/monthly LLM quotas, context window consumption, cache hits, and rate limits across models.
- 🎙️ **On-Device Voice Assistant & Spoken Briefings**: Native on-device Text-to-Speech (TTS) integration that delivers spoken agent summaries, task execution updates, and project status directly to the device speaker or connected headphones without disturbing desktop audio.
- 🔔 **Full Chromium WebPush & Lockscreen Sync**: Multi-stream EventSource, WebSocket, ServiceWorker, and Blue Dot unread indicators with zero-redaction lockscreen notifications.
- 📱 **Android 15 Ready (16KB Memory Page Support)**: Built and verified with 16KB ELF page size alignment for next-gen Android 15 performance and battery efficiency.
- 🎨 **Modern Material You & Jetpack Compose**: Full dynamic theme support, edge-to-edge UI, dark mode, smooth gesture animations, and haptic feedback.

---

## 🏗️ Architecture Overview

```mermaid
graph TD
    subgraph "Desktop Host (Antigravity IDE / CLI)"
        IDE[Antigravity IDE Core] --> Sidecar[Antigravity Remote Bridge]
        Sidecar --> QR[Dynamic Session QR Generator]
        Sidecar --> WS_Server[Authenticated WebSocket / HTTPS Server]
    end

    subgraph "Mobile Device (Antigravity Remote)"
        Camera[CameraX QR Scanner] -.->|Scan & Extract Session| ClientAuth[Biometric Authenticator]
        ClientAuth --> WS_Client[OkHttp / Coroutine WebSocket Client]
        WS_Client <==>|Encrypted Real-Time Stream| WS_Server
        WS_Client --> Repo[Agent State Repository]
        Repo --> ViewModel[Compose State Holders]
        ViewModel --> UI[Material 3 UI / Quotas / Approvals]
    end
```

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose, Material 3, Edge-to-Edge Navigation
- **Camera & Scanning**: AndroidX CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`), Google ML Kit Barcode Scanning
- **Security**: AndroidX Biometric (`androidx.biometric:biometric`), Android KeyStore Provider
- **Networking**: OkHttp 4, Kotlinx Coroutines & Flow, Moshi / Kotlinx Serialization
- **Target OS**: Android 8.0 (API 26) through Android 15 (API 35, 16KB Page Compatible)

---

## 🚀 Quickstart Guide

### Prerequisites
1. Android device running Android 8.0 (Oreo) or later.
2. PC / Mac running Google Antigravity IDE or Antigravity CLI.

### 1. Running the Android Application

#### Option A: Download Pre-built APK
Download the latest `AntigravityRemote-v1.0.0.apk` from the **[GitHub Releases](https://github.com/Densuper/AntigravityRemote/releases)** tab.

#### Option B: Build from Source
```bash
# Clone the repository
git clone https://github.com/Densuper/AntigravityRemote.git
cd AntigravityRemote

# Build debug APK using Gradle wrapper
# On Linux/macOS:
./gradlew assembleDebug

# On Windows:
gradlew.bat assembleDebug
```
The resulting APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Pairing with Antigravity Desktop

1. Open your Antigravity IDE workspace or start the remote server from the Antigravity companion interface.
2. Launch **Antigravity Remote** on your Android device.
3. Tap **Scan QR Code** and point your camera at the screen QR code.
4. Verify with your fingerprint / face ID when prompted.
5. Your live session dashboard, agent execution queue, and quota indicators are now active!

---

## 🔒 Security & Privacy

- **Local Network Priority**: Session pairing and real-time streaming operate peer-to-peer over local network channels or end-to-end encrypted tunnels.
- **Biometric Enclave**: Sensitive session credentials and auth tokens are stored encrypted in the Android Hardware Keystore and cleared on disconnect.
- **Zero Third-Party Telemetry**: Your code snippets, diffs, agent prompts, and responses remain strictly private to your machine and paired mobile device.

---

## 🤝 Contributing

We welcome contributions from the open-source community! Please review our [CONTRIBUTING.md](CONTRIBUTING.md) guide for instructions on filing issues, submitting pull requests, and setting up the development environment.

---

## 📄 License

Antigravity Remote is licensed under the **[MIT License](LICENSE)**.

Copyright &copy; 2026 Denver Colaco. All rights reserved.
