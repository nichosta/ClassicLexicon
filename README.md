# ClassicLexicon

An Android app for browsing the Liddell-Scott-Jones (LSJ) Ancient Greek-English Lexicon.

## Features

- Full-text search across LSJ lexicon entries
- Detailed entry view with XML-rendered definitions
- Offline access via pre-built SQLite database
- Built with Jetpack Compose and Material 3

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Database:** Room (SQLite) with FTS (full-text search)
- **Navigation:** Jetpack Navigation Compose
- **Build:** Gradle with KSP

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle and build

**Requirements:** Android SDK 35, JDK 17, min API 26

## Data Sources

This app uses lexicon data from:

- [LSJ_GreekUnicode](https://github.com/gcelano/LSJ_GreekUnicode) — Liddell-Scott-Jones Greek-English Lexicon (Unicode XML)
- SmithHall1871 — Smith & Hall English-Greek Dictionary

These repos are not included in this repository and should be cloned separately if needed for data processing.

## License

Lexicon data is in the public domain. App source code is provided as-is.
