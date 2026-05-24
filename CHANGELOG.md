# Changelog

All notable changes to this project will be documented in this file.

## [2.1.0] - 2026-05-24

### Added
- **Database Encryption:** Secured local storage using SQLCipher (AES-256).
- **Advanced App Lock:** Introduced PIN/Password protection with auto-lock timeout options (Immediate, 1 min, 2 min).
- **Setup Confirmation:** Added double verification when setting up a new lock password/PIN.
- **Modification Security:** Modifying or disabling the app lock now requires the current password.

## [2.0.0] - 2026-05-24

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
