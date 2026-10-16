# CareLink – Welfare Check Coordination App

An Android app that coordinates welfare check visits between residents, care coordinators, support workers and safeguarding reviewers. Built in Kotlin with Jetpack Compose as a university module project.

## What it does

- **Residents** submit welfare requests and follow their progress on a status timeline.
- **Care coordinators** review an inbox of incoming requests and assign them to support workers, with deadlines.
- **Support workers** see their assigned visits and record each visit's outcome.
- **Safeguarding reviewers** work through a review queue and sign off completed visits.
- Every action is written to an **audit log**, and users get **notifications** for changes that affect them.

## Technical highlights

- **Kotlin + Jetpack Compose** (Material 3) with Navigation Compose
- **Role-based access**: each of the four roles gets its own screens and permissions
- **SQLite database** with a repository layer (users, requests, visits, reviews, audit entries)
- **Secure passwords**: salted SHA-256 hashing, never stored as plain text
- Session management, deadline formatting and a seeded demo dataset

## Run it

Requirements: Android Studio (Hedgehog or later), JDK 17, Android SDK 34.

```bash
./gradlew assembleDebug
```

Or open the folder in Android Studio and press **Run**.

## Demo accounts

All demo accounts use the password `password123` (local demo data only).

| Role | Email |
|---|---|
| Care Coordinator | coordinator@carelink.test |
| Support Worker | worker@carelink.test |
| Safeguarding Reviewer | reviewer@carelink.test |
| Resident | resident@carelink.test |

On first launch the app seeds about 50 rows of demo data. On a slow emulator, tap **Wait** if Android asks; later launches are instant.
