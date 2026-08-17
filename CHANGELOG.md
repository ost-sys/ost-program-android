**Latest Phone Version:** v4.0.0-beta6
**Latest Wear OS Version:** v3.0.0-beta6

---

## Changelog — Android (v4.0.0-beta6)

### New Features & Improvements
- **LocalSend Cross-Platform Sharing (Protocol v2.1)**: Full integration of LocalSend Protocol v2.1, enabling bidirectional file transfers with official LocalSend clients on iOS, macOS, Windows, Linux, Android, and Wear OS.
- **Continuous Multi-Subnet Discovery**: Added automated parallel subnet scanning (`SubnetScanner`) across all active network interfaces with automatic bidirectional device registration (`/api/localsend/v2/register`).
- **Encrypted TLS File Server**: Integrated embedded Netty HTTPS server with dynamic PKCS12 SSL certificate generation, SHA-256 fingerprint verification, and streaming multi-file upload handling.
- **System-as-Root Detection**: Added System-as-Root partition architecture detection to Device Information.
- **Infinite Burn-In Recovery**: Updated Fix Dead Pixels / Burn-In Recovery tool to support infinite duration mode.
- **Localization Updates**: Consolidated and updated translation resources across all supported language locales (including Belarusian, Spanish, Russian, Ukrainian, and English).

### Bug Fixes & Stability
- **Burn-In Recovery 64-bit Fix (PR #5)**: Fixed a critical bug causing "Fix dead pixels" to break on 64-bit-only devices.
- **Navbar Background Fix (PR #4)**: Resolved navigation bar background rendering glitch in MainActivity.
- **R8 Release Startup Crash**: Fixed Ktor reflection crash (`CallableUtilsKt.executeModuleFunction` / `Array has more than one element`) by eliminating dynamic module reloading under R8.
- **iOS/macOS Transfer Compatibility**: Mapped file metadata to strict standard protocol enums (`image`, `video`, `pdf`, `text`, `other`), eliminating transfer rejections from official iOS/macOS clients.
- **Coroutine Body Parsing**: Resolved `NoSuchMethodError` during JSON body parsing in `LocalSendClient` with direct Gson deserialization.

---

## Changelog — Wear OS (v3.0.0-beta6)

### New Features & Improvements
- **Standalone LocalSend for Smartwatches**: Full-featured LocalSend file transfer engine running natively on Wear OS without requiring a companion phone.
- **Material 3 Wear OS Interface**: Custom interface featuring Rotary Crown hardware scrolling, circular transfer progress rings, and file acceptance confirmation dialogs.
- **Standalone Wi-Fi Management**: Automatic Wi-Fi lock acquisition and network routing ensuring transfers remain connected on standalone watches.
- **Continuous Subnet Scanning**: Background cyclic subnet discovery for instant detection of nearby mobile and desktop devices.
- **Burn-In Recovery Updates (PR #5)**: Added 64-bit device compatibility and infinite duration recovery mode to Wear OS display tools.
- **Localization Coverage**: Added full translations for all new Wear OS sharing and system features.

### Bug Fixes & Stability
- **ProGuard & Release Optimization**: Added comprehensive keep-rules and fixed Netty engine startup under R8 release minification.
- **TLS & Network Hardening**: Enhanced SSL/TLS certificate handling and connection stability across local wireless networks.

**This is a pre-release version! Bugs and instability are possible.**
