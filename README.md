# NotiLogger 📱

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)

**NotiLogger** is a privacy-focused, open-source Android application that allows you to capture, store, and manage your system notifications locally. Never miss a notification again.

---

## ✨ Features

- 📝 **Auto-Logging:** Automatically saves every notification to a local SQLite database.
- 🛡️ **Advanced Filtering:** 
    - **App Filtering:** Whitelist or Blacklist specific apps.
    - **Keyword Filtering:** Block notifications globally or per-app based on custom keywords (e.g., OTP, Promo).
- 🚀 **Telegram Forwarding:**
    - **Multi-Bot Support:** Forward notifications to multiple Telegram bots and chats in real-time.
    - **Hierarchical Logic:** Powerful multi-level filtering (Chat > Bot > Global) for precise control.
    - **Auto-prefill:** Custom filters automatically pre-fill with parent settings for quick setup.
- 🔒 **Security & Privacy:**
    - **Database Encryption:** All logs are secured with AES-256 encryption using **SQLCipher**.
    - **App Lock:** Secure the interface with a PIN or Password, featuring auto-lock timeouts.
    - **Privacy First:** Internet permission is used **exclusively** for forwarding to Telegram. All other data remains strictly on-device.
- 🔍 **Smart Search:** Quickly find notifications by app name, package name, or notification content.
- 📤 **Backup & Restore:**
    - **JSON Export:** Export logs for all or selected apps with timestamped filenames.
    - **JSON Import:** Easily restore previously exported logs back into the database.
- ⚡ **Real-time UI:** 
    - **Instant Refresh:** Home screen updates immediately as new notifications arrive.
    - **Unread Badges:** Clear visual indicators for new/unseen notification logs.
- 🎨 **Modern UI:** Built with Material 3 components, featuring a clean Dark Mode interface.

## 🚀 Installation

1. Download the latest APK from the [Releases](https://github.com/mamun10fx/NotiLogger/releases) page.
2. Install the APK on your Android device.
3. **Grant Permission:**
    - Go to `Settings` -> `Apps` -> `Special app access` -> `Notification access`.
    - Enable **NotiLogger**.

## 🛠️ Built With

- **Kotlin** - Primary programming language.
- **Room Persistence** - Local SQLite database management.
- **SQLCipher** - 256-bit AES database encryption.
- **Coroutines & Flow** - Asynchronous programming.
- **HttpURLConnection** - Lightweight, library-free networking for Telegram API.
- **Material Components** - UI design system.
- **Gson** - JSON serialization.

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 👤 Author

**Abdullah Al Mamun**
- GitHub: [@mamun10fx](https://github.com/mamun10fx)
- Email: mamun10fx@gmail.com
- Socials: [Facebook](https://www.facebook.com/profile.php?id=61583220766712) | [Telegram](https://t.me/mamun10sc) | [Instagram](https://www.instagram.com/mamun10xc)
