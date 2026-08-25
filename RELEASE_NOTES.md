# Release Notes - Antigravity Remote

## v1.1.0 - AGENT HUD, Biometric Lock & Voice Bridge Release (2026-08-25)

We are excited to release **Antigravity Remote v1.1.0** with major UX improvements, layout polish, biometric privacy, and a plug-and-play voice assistant bridge!

### 🌟 What's New in v1.1.0:

- **⚡ Dedicated Non-Overlapping AGENT HUD**:
  - Moved overlay pill into a dedicated `38dp` native top status bar.
  - Zero web UI obstruction—back buttons, navigation tabs, and agent controls remain 100% accessible.
- **🔒 Dedicated Biometric Lock Screen**:
  - Lock your active workspace behind AndroidX `BiometricPrompt` (Fingerprint / PIN) with a single tap.
- **🎙️ Plug-and-Play Voice Assistant Bridge**:
  - Added native on-device speech engine (`com.example.antigravityremote.SPEAK`).
  - Developers can connect their own AI agents (Claude, GPT, custom personas) to speak summaries autonomously without exposing API keys or private scripts.
- **🧼 100% Genuine UI**:
  - Removed artificial quota dials and static estimates in favor of a clean, minimalist control console.
- **📱 Anti-Refresh Lifecycle**:
  - Implemented `singleInstance` launch mode and warm memory retention across app minimizes.

---

## v1.0.0 - Initial Open Source Release (2026-08-23)

We are thrilled to announce the initial open-source release of **Antigravity Remote** for Android!

### Highlights & Features

- **⚡ Live Agent Stream & Steer**: Real-time inspection of multi-turn subagent runs, shell command outputs, tool invocations, and workspace edits.
- **📷 CameraX Zero-Config QR Pairing**: Instant session handshake with Antigravity IDE and CLI via camera scan and ML Kit Barcode Analyzer.
- **🔒 Biometric Security**: Android Hardware Keystore & BiometricPrompt integration for authorizing high-impact agent tools and terminal commands.
- **📊 Model Quotas & Telemetry**: Dedicated dashboard monitoring rate limits, token usage, and context headroom across Gemini and Claude models.
- **🔔 Actionable Heads-Up Notifications**: Rich background notifications when agent workflows require human review or finish long-running tasks.
- **📱 Android 15 & 16KB Page Compatibility**: Full support for Android 15 (API 35) with 16KB page-aligned native binaries.
- **🎨 Edge-to-Edge Material You**: Dynamic color theming, dark mode, smooth gesture navigation, and crisp typography.

---

### CI & Automated Builds
- Continuous Integration workflow configured via GitHub Actions for automated debug and release APK packaging.
