# Shuttlers

An offline court organiser for social badminton sessions on Android. You check players in as they
arrive and Shuttlers fills every booked court with a balanced doubles match: it picks who plays
next by priority (arrival order, games played, time on the bench), keeps the skill gap on each court
within limits, prefers same-gender courts when possible, and splits the four players into two even
teams.

- Kotlin + Jetpack Compose, Material 3, minSdk 26 (Android 8.0), targetSdk / compileSdk 37
- Room database on the device; no network permission, no login, no ads, no analytics
- Permanent club roster with skill tier (S, A, B, C, D, X) and gender per player
- Live court board, waiting lounge sorted by priority, substitutions and swaps mid-match
- Winner recording: finishing a match asks which pair won; wins and losses build each player's W–L record
- Undo for the last finished matches, substitutions and swaps (button and snackbar)
- Session history with each player's final games played, tier and record
- Dark court theme (deep court green + coral, Plus Jakarta Sans and Barlow Condensed), adaptive launcher icon

## Project layout

```
BadmintonAdmin/
└── app/                            Android app
    └── src/main/java/com/mdp/badmintonadmin/
        ├── MainActivity.kt         Entry point, screen routing (Home, Dashboard, Roster), back handling
        ├── domain/
        │   ├── session/            SessionRules (match credit, undo restore, history lines), UndoStack
        │   ├── model/Models.kt     PlayerMatchmakingProfile, CourtAssignment
        │   └── matchmaker/
        │       ├── PriorityEngine.kt   Priority score (arrival, bench time, game variance)
        │       └── Matchmaker.kt       Court search, skill-gap matrix, team balancing
        ├── data/local/
        │   ├── AppDatabase.kt      Room database (players, session_participants, session_history)
        │   ├── dao/PlayerDao.kt    Roster, active-session and history queries
        │   └── entity/             PlayerEntity, SessionParticipantEntity, SessionHistoryEntity
        └── ui/
            ├── dashboard/          DashboardScreen (routing) + DashboardViewModel; SessionSetupScreen,
            │                       SessionBoard, CourtCard, WaitingLounge, SessionHistoryScreen, DashboardDialogs
            ├── player_mgmt/        RosterScreen + RosterViewModel (add, edit, search, delete)
            ├── components/         Shared composables
            └── theme/              Colours, typography, Material 3 theme
```

### Architecture in one paragraph

The Room database holds the permanent roster, tonight's checked-in participants and past sessions.
`DashboardViewModel` turns each checked-in participant into a `PlayerMatchmakingProfile` and asks
`Matchmaker.autoGenerateLineups(...)` to fill the free courts; courts already in play are kept as
they are. Every time something changes (a check-in, a finished match, a substitution, a tier change,
a player marked absent) the view model writes the participant update to Room and recomputes the
lineups, so the court board and the waiting lounge always come from the same calculation. The UI
never picks players itself.

## Matchmaking rules

| Behaviour | Where |
|---|---|
| Priority = arrival bonus (`(11 − arrivalIndex) × 0.5`, min 0) + bench bonus (`15 × matches sat out in a row`) − game penalty (`20 × (games played − lowest games played in the room)`) | `PriorityEngine.calculatePriorityScore` |
| The highest-priority player always anchors the next court; the other three are searched from the rest of the queue | `Matchmaker.autoGenerateLineups` |
| Tier weights: S 5.0 · A 4.0 · X 3.5 · B 3.0 · C 2.0 · D 1.0 | `Matchmaker.getTierWeight` |
| Max skill gap on a court is 2.0 (S can play down to B) | `isCourtSkillBalanced` |
| Safety valve: if anyone on the court has sat out 3+ matches in a row, the gap relaxes to 3.0 | `isCourtSkillBalanced` |
| A balanced all-men or all-women court wins over a balanced mixed one | `autoGenerateLineups` |
| If no four players pass the skill check, the top four by priority play anyway | `autoGenerateLineups` |
| Teams: 1st + 4th strongest vs 2nd + 3rd strongest | `createBalancedCourtAssignment` |
| Finishing a match: its four players get +1 game and a bench count of 0; everyone else present gets +1 bench count; the winning pair gets +1 win and the other pair +1 loss (or nothing on "No result") | `SessionRules.creditFinishedMatch` |
| Wins, losses and games reach the permanent roster on Save & End | `DashboardViewModel.saveAndEndSession` |
| Undo restores the courts and the affected players' stats from a snapshot (last 10 actions, in memory) | `DashboardViewModel.undoLastAction` |
| Substituting a player credits them with the game and marks them absent | `DashboardViewModel.substitutePlayer` |
| Absent players are skipped until marked present again | `togglePlayerPresence` |

`PriorityEngine.getConstraintsForArrival` (minimum and maximum games by arrival zone: first 25%,
25–50%, after 50% of the session) is implemented but not yet wired into the lineups.

## Build

Requirements: JDK 17 or newer and the Android SDK with platform 37. The project uses AGP 9.2.1,
Kotlin 2.2.10, KSP and Gradle 9.4.1 (wrapper included), so use a current Android Studio release.

### Android Studio
1. *File → Open…* and choose the `BadmintonAdmin` folder.
2. Let Gradle sync, then press **Run** with a device or emulator selected.

### Command line
```bash
cd BadmintonAdmin
# point Gradle at your SDK once
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew testDebugUnitTest     # matchmaker unit tests
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # app/build/outputs/apk/release/app-release.apk (not minified)
```

### Release signing

The release build reads its signing key from environment variables or Gradle properties (for
example in `~/.gradle/gradle.properties`), never from files in the repository:

| Variable | Meaning |
|---|---|
| `BADMINTON_KEYSTORE_PATH` | Path to your keystore (`.jks`), absolute or relative to the repo root |
| `BADMINTON_KEYSTORE_PASSWORD` | Keystore password |
| `BADMINTON_KEY_ALIAS` | Key alias |
| `BADMINTON_KEY_PASSWORD` | Key password |

When they are not set, the release build falls back to the debug key so it still builds and
installs. Keystores and `keystore.properties` are git-ignored; never commit them.

### Signed releases on GitHub

`.github/workflows/release.yml` builds and signs the APK whenever a release is published. Add four
repository secrets under **Settings → Secrets and variables → Actions**: `BADMINTON_KEYSTORE_BASE64`
(the `.jks` file, base64-encoded), `BADMINTON_KEYSTORE_PASSWORD`, `BADMINTON_KEY_ALIAS` and
`BADMINTON_KEY_PASSWORD`. Bump `versionCode` / `versionName` in `app/build.gradle.kts`, publish a
release tagged `v<versionName>` (e.g. `v1.3`), and the workflow attaches `Shuttlers-<version>.apk`.
It fails rather than publish a debug-signed APK.

## Install on a phone

**From the APK file**
1. Copy `app-debug.apk` to the phone.
2. Open it from the Files app. If asked, allow *Install unknown apps* for that app.
3. Tap **Install**, then open **Shuttlers**.

**With USB debugging**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Using the app

1. Home → **Manage Roster** to add club members with a name, tier and gender (once; they stay
   saved). Search, edit or delete players from the same screen.
2. Home → enter the **Number of Booked Courts** and **Session Duration (Minutes)**, then
   **Start New Session**.
3. Tap **+** to check players in as they arrive, or register a new member and check them in at
   once (**Add & Check-In**). Courts fill automatically as soon as four players are available.
4. When a court finishes, tap **Match Finished ✔️**; the next balanced match is assigned right
   away. The **Waiting Lounge** shows who is next, with games played and matches benched.
5. **Edit Players ⚙️** on a court lets you swap two players between teams or substitute someone from
   the lounge. In the lounge you can change a player's tier for tonight, edit their profile or
   mark them absent.
6. **Save & End** credits the matches in progress, adds each player's games to their lifetime
   stats and saves the session to **Session History**. **Reset** clears the board without saving.

## Tests

`./gradlew testDebugUnitTest` runs the JVM tests in `app/src/test/`:

1 same-gender courts are preferred when available · 2 mixed courts are built when same-gender is not
possible · 3 skill balance wins over gender.
