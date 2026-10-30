# Contributing to SpyShield

Thanks for helping make SpyShield better! Contributions of all sizes are welcome.

## Ways to help

- **Report bugs**, especially phone-specific ones. Camera and microphone behaviour varies a lot between phone makers, so reports from different models are really valuable. Use the [bug report template](https://github.com/weby-dev/spyshield/issues/new?template=bug_report.md) and include your phone model and Android version.
- **Report false alarms** (an alert when nothing was spying) and say what you were doing at the time, e.g. face unlock, a call or voice typing.
- **Improve the docs**, such as battery-setting paths for phones not yet listed in [docs/INSTALL.md](docs/INSTALL.md).
- **Send code**: bug fixes, detection improvements, accessibility and translations.

## Making a code change

1. Fork the repository and create a branch from `main`.
2. Open the project in Android Studio (or use `./gradlew assembleDebug`). See [Build from source](README.md#build-from-source).
3. Keep changes focused; one fix or feature per pull request.
4. Follow the existing Kotlin style and the design system in [docs/DESIGN.md](docs/DESIGN.md) for UI work.
5. Run the unit tests with `./gradlew test` and add tests for new logic where you can.
6. Test on a real phone if your change touches detection, and mention the model and Android version in the pull request.
7. Open a pull request explaining what changed and why.

## Ground rules

- **Privacy first.** SpyShield must never send user data off the device. Pull requests that add analytics, ads, tracking or network calls for monitoring data will not be accepted.
- **Be honest with users.** Don't add text that promises more than Android allows (for example, silently blocking apps without root).
- Be kind and respectful in issues and reviews.

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
