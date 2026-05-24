# Changelog

All notable changes to this project will be documented in this file.

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
