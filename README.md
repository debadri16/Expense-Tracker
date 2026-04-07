# Expense Tracker

An Android app that automatically tracks expenses by reading and parsing bank SMS messages. Built with Kotlin, Jetpack Compose, and Room.

## Features

- **Automatic SMS parsing** — detects debits and credits from bank transaction messages
- **Real-time notifications** — get notified when a new transaction is detected, with quick-add actions
- **Transaction management** — review, categorize, and browse your transaction history
- **Spending analysis** — visual breakdown of your expenses
- **Local-first** — all data stays on your device using Room (SQLite)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | ViewModel |
| Database | Room (with KSP) |
| Build | Gradle (Kotlin DSL), AGP 9.1 |
| Min SDK | 26 (Android 8.0) |

## Project Structure

```
app/src/main/java/com/expensetracker/app/
├── MainActivity.kt                  # Entry point
├── ExpenseTrackerApp.kt             # Application class
├── data/
│   ├── db/                          # Room database, DAO, entities
│   └── sms/                         # SMS broadcast receiver, parser, reader
├── notification/                    # Transaction notifications
├── ui/
│   ├── screens/                     # Compose screens (list, review, analysis)
│   └── theme/                       # Material 3 theme and colors
└── viewmodel/                       # ViewModels
```

## Getting Started

### Prerequisites

- Android Studio (Ladybug or newer recommended)
- JDK 17
- Android SDK 36

### Build & Run

```bash
# Clone the repository
git clone <repo-url>
cd expense_tracker

# Build a debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

### Permissions

The app requires these runtime permissions:

- `READ_SMS` — to scan existing bank messages
- `RECEIVE_SMS` — to detect new transactions in real time
- `POST_NOTIFICATIONS` — to show transaction alerts

## License

This project is licensed under the [Apache License 2.0](LICENSE). See the [LICENSE](LICENSE) file for details.
