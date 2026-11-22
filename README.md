# WidgetLite

**Your Home Screen. Your Freedom. Completely Free.**

WidgetLite is a 100% free, open-source, privacy-first Android home screen widget studio built with Kotlin, Jetpack Compose, Material Design 3, Room, and native Android `AppWidgetProvider` integration.

## Promises

- **100% Free Forever** — No subscriptions, no paywalls, no locked widgets.
- **Zero Ads & Zero Tracking** — No advertising SDKs, no analytics, no accounts required.
- **Local-First Storage** — Configurations live locally in Room & DataStore, with portable JSON export/import.
- **Full Customization Studio** — Customize background color, text color, accent color, font family, font size, font weight, transparency, border radius, padding, alignment, dimensions (`2×2`, `4×2`, `4×4`, `4×1`), light/dark theme overrides, and background photos.

## Widget Collection (39 Native Widgets)

- **Productivity**: Digital Clock, Analog Clock, Calendar, Daily Agenda, To-do List, Notes, Countdown, Stopwatch, Pomodoro Timer, Daily Goals, Habit Tracker.
- **System Information**: Battery Percentage, Storage Information, RAM Information, Device Information, Internet Connection, Wi-Fi Information, Network Speed.
- **Weather & Time**: Weather Widget, Temperature, Weather Forecast, Multiple Time Zones, World Clock, Sunrise & Sunset.
- **Personalization**: Custom Text, Inspirational Quotes, Photo Widget, Minimalist Clock, Custom Color Widget, Themed Calendar, Custom Icon Shortcut.
- **Utilities**: Quick Settings Shortcuts, Flashlight Shortcut, Calculator Shortcut, Website Shortcut, App Shortcut, QR Code Widget, Random Number Generator, Daily Reminder.

## Installation & Build Instructions

### Prerequisites
- Android Studio (Ladybug or newer) or Android SDK with JDK 11+
- Android device or emulator running Android 7.0+ (API 24+)

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/widgetlite/widgetlite-android.git
   cd widgetlite-android
   ```
2. Build the debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
3. Run unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. Install `app/build/outputs/apk/debug/app-debug.apk` onto your device or run directly from Android Studio.

## Platform Transparency

- **Refresh Intervals**: Android enforces a 30-minute minimum `updatePeriodMillis` for standard AppWidgets to conserve battery.
- **Wi-Fi SSID**: Android 10+ restricts SSID access without fine/background location; WidgetLite avoids unnecessary location permissions and reports private link speed and signal strength instead.
- **Quick Settings**: Android prohibits third-party apps from toggling system radios silently, so Quick Settings buttons open the exact system settings panels directly.

## License

Licensed under the [MIT License](LICENSE).
