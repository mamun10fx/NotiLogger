# Changelog

All notable changes to this project will be documented in this file.

## [2.5.1] - 2026-05-24

### Changed
- **Privacy & Branding:** Replaced personal name in navigation header with the app name.
- **Dynamic Versioning:** Navigation drawer now automatically detects and displays the correct app version.
- **Metadata:** Added Fastlane folder structure for F-Droid/IzzyOnDroid compatibility.

## [2.5.0] - 2026-05-24

### Changed
- **Navigation Update:** Replaced the notification icon/logo in the main toolbar with a standard **hamburger menu** icon to improve navigation discoverability.
- **UI Polish:** General refinements to the Dark Mode interface for better consistency.

## [2.4.0] - 2026-05-24

### Added
- **Backup & Restore:** Added full JSON export and import functionality with multi-app selection support.
- **Real-time UI:** Home screen now updates instantly when new notifications arrive using Room Flow.
- **Unread Status:** Added visual indicators (badges and tags) for new/unseen notifications.
- **Per-App Export:** Export logs for a specific app directly from the home screen via long-press.

## [2.3.0] - 2026-05-24

### Added
- **Telegram Forwarding:** Forward notifications to multiple Telegram bots and chats in real-time.
- **Hierarchical Filtering:** Multi-level filtering support (Global > Bot > Chat) for both apps and keywords.
- **Auto-prefill Logic:** New custom filters now automatically pre-fill with parent settings for easier configuration.

## [2.2.0] - 2026-05-24

### Added
- **Keyword Filtering:** Introduced global and app-specific keyword filtering to block unwanted notifications (e.g., OTPs, Promos).
- **Database Encryption:** Secured local storage using SQLCipher (AES-256).
- **Advanced App Lock:** PIN/Password protection with auto-lock timeout options (Immediate, 1 min, 2 min).
- **Smart Search:** Added search bars in Main, Filter, and Details screens.
- **UI Improvements:** Better state handling for empty logs and refined security settings.

## [2.1.0] - 2026-05-24

### Added
- Search functionality in **MainActivity** to filter notifications by app name.
- Search functionality in **FilterActivity** to search apps by name or package.
- Search functionality in **DetailsActivity** to search within notification content.
- Screenshots added to **README.md**.

### Fixed
- Fixed UI bug where empty app groups remained visible after clearing logs or deleting the last notification.

## [1.0.0] - 2026-05-23

### Initial Release
- Notification logging with Room database.
- App filtering (Blacklist/Whitelist).
- JSON export functionality.
- Material Design 3 interface.
- Bug fix: Resolved crash in FilterActivity due to layout inflation error.
