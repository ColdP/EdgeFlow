# EdgeFlow

<p align="center">
  <img src="readme-images/ic_edgeflow.png" width="96" alt="EdgeFlow Icon" />
</p>

<p align="center">
  A full-screen negative screen (负一屏) alternative for Android,
  built with Jetpack Compose + Kotlin + Material Design 3.
</p>

<p align="center">
  <a href="https://github.com/ColdP/EdgeFlow/releases">
    <img src="https://img.shields.io/github/v/release/btm-m/EdgeFlow?color=green" alt="Latest Release" />
  </a>
  <img src="https://img.shields.io/badge/license-GPL%20v3.0-blue" alt="License: GPL v3.0" />
  <img src="https://img.shields.io/badge/compose-2024.12-orange" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/kotlin-2.0.21-blue" alt="Kotlin 2.0.21" />
  <a href="README_zh.md">
    <img src="https://img.shields.io/badge/zh_CN-中文版-blue" alt="中文版" />
  </a>
</p>

---

## Preview

| Main Screen | Sidebar | About |
|:---:|:---:|:---:|
| ![Main](readme-images/Screenshot_mainactivity.png) | ![Sidebar](readme-images/Screenshot_sidebar.png) | ![About](readme-images/Screenshot_aboutactivity.png) |

---

## Features

- **Full-screen negative screen replacement** — a clean, gesture-driven alternative to the stock Android sidebar
- **Shizuku / Root support** — elevated privileges for deeper system integration
- **Jetpack Compose + Material Design 3** — modern, fluid UI
- **Custom sidebar apps** — pick and arrange the apps you want
- **Media listener service** — show now-playing info on the negative screen
- **Custom quick links** — configurable shortcut buttons
- **Multi-language support** — English + Chinese (more languages welcome!)

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose (BOM 2024.12.01) + Material Design 3 |
| Language | Kotlin 2.0.21 |
| DI | Hilt 2.51.1 |
| Database | Room 2.6.1 |
| Image Loading | Coil 3.0.4 |
| Preferences | DataStore Preferences 1.1.1 |
| System Access | Shizuku API 13.1.5 |
| Navigation | Compose Navigation 2.8.5 |

**Min SDK:** 28 (Android 9) &nbsp;|&nbsp; **Target SDK:** 35 &nbsp;|&nbsp; **Compile SDK:** 35

---

## Build from Source

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11+
- Android SDK Platform 35 installed
- (Optional) Shizuku installed on device for full functionality

### Steps

```bash
# Clone the repo
git clone https://github.com/ColdP/EdgeFlow.git
cd EdgeFlow

# Build debug APK
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Install & Use

1. Install the APK on your Android device
2. Grant **Display over other apps** permission
3. (Recommended) Install and pair **Shizuku** for full feature access
4. Open EdgeFlow — configure trigger gestures and sidebar content
5. Swipe from the edge to invoke

---

## Project Structure

```
EdgeFlow/
├── app/
│   ├── src/main/
│   │   ├── java/btm/m/edgeflow/   # Kotlin source code
│   │   ├── res/                     # Resources (XML, images, etc.)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── README.md          # English documentation
├── README_zh.md       # 中文文档
└── LICENSE
```

---

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a PR.

1. Fork the repo
2. Create a feature branch (`git checkout -b feat/your-feature`)
3. Commit your changes (`git commit -m "feat: add your feature"`)
4. Push to the branch (`git push origin feat/your-feature`)
5. Open a Pull Request

---

## License

[GNU General Public License v3.0](LICENSE)

---

## Links

- **GitHub:** [github.com/btm-m/EdgeFlow](https://github.com/ColdP/EdgeFlow)
- **Developer's Site:** [btm-m.site](https://btm-m.site)
- **Blog:** [btm-m.live](https://btm-m.live)

---

> Made with love by [btm_m](https://github.com/ColdP)
