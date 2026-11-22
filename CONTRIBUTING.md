# Contributing to WidgetLite

Thank you for helping make WidgetLite the best completely free, open-source Android widget app!

## Core Principles

All contributions must preserve WidgetLite's core commitments:
1. **No ads, trackers, telemetry, or paywalls.**
2. **No proprietary closed-source SDKs** that prevent independent compilation.
3. **Least-privilege permissions** compliant with Google Play policies (e.g., use Android Photo Picker instead of broad storage permissions).

## How to Contribute

1. Fork the repository and create a feature branch (`git checkout -b feat/my-widget`).
2. Follow Kotlin & Jetpack Compose Material 3 conventions.
3. Add or update unit/Robolectric tests for new widget payloads or calculations.
4. Run `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` to verify compilation.
5. Open a pull request describing your changes clearly.
