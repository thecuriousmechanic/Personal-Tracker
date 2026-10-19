# SoloLeveller APK reverse-engineering report

## Executive summary

`SoloLeveller.apk` is **SoloLeveller 1.0.26**, package `com.app.sololeveller`. It is not primarily an action game. It is a **Solo Leveling-inspired, gamified self-improvement and fitness tracker** built with React Native. Users complete daily tasks, workouts, and themed quests to gain experience, levels, streaks, skill points, and character statistics.

The app includes Google/Firebase authentication, Firebase Realtime Database storage, local task storage, profile customization, a leaderboard, inventory/shop and dungeon-themed screens, Google Mobile Ads, and RevenueCat-managed purchases. Its dungeon battle screen explicitly describes itself as a preview, so the battle system appears incomplete in this version.

This conclusion comes from static analysis of the decoded manifest/resources, Android bytecode, and the embedded Hermes JavaScript bytecode. The APK was not installed or exercised on a device.

## APK identity

| Field | Value |
|---|---|
| Input file | `SoloLeveller.apk` |
| SHA-256 | `E1CF099F2D2ACC7FC458FACA2E3A9BCBCF098FA8DC6882E6CCA315C1B8436E62` |
| File size | 59,352,456 bytes |
| Application ID | `com.app.sololeveller` |
| App label | `SoloLeveller` |
| Version | `1.0.26` (`versionCode` 26) |
| Minimum Android | API 24 (Android 7.0) |
| Target/compile Android | API 34 (Android 14) |
| Android bytecode | Five DEX files |
| App framework | React Native with Hermes enabled |
| Hermes bytecode | HBC v96, 16,780 functions, 26,793 strings, 1,742 Metro modules |
| Signing | Valid APK Signature Scheme v2 and v3 signatures; valid Google Play source stamp |

The manifest declares `base__abi` and `base__density` as required split types, while this APK contains no native libraries. This strongly indicates that the supplied file is the base APK from an Android App Bundle and may require matching ABI and density configuration APKs to install or run correctly.

## What the app does

### Core progression

- Maintains player state including username, aura, class, experience, player level, thresholds, streak, skill points, inventory, premium flags, and stats.
- Starts the five stats—strength, mental/intelligence, discipline/agility, spirit/perception, and vitality—at 10.
- Awards 7 experience per progression event. On level-up, the next threshold is calculated using a 1.15 exponential multiplier and the player receives a skill point.
- Increases or decreases individual stats in 0.25 increments. The user-facing penalty text says failed daily tasks decrease stats.
- Calculates leaderboard points from the average of strength, mental, discipline, and spirit.

### Tasks, quests, and fitness

- Daily tasks include push-ups, sit-ups, outdoor running, and a 15-minute meditation target.
- Users can create personal quests with low, medium, or high difficulty and persist them locally.
- Tracks task completion and streaks and displays a daily countdown/reset flow.
- Provides themed quest collections for strength, intelligence/mental training, discipline, spirituality, secret quests, and personal quests.
- Includes many exercise illustrations for abs, back, biceps, chest, glutes, quads, and triceps.
- Includes a workout screen and a separate spiritual-task screen.

### Game-like presentation

- Profile/status screen with ranks, stats, available ability points, aura, class, title, and profile picture.
- Inventory, inventory shop, milestones, item unlocks, and profile customization.
- Dungeon and dungeon-battle screens. The bundled battle screen says it is only a preview and that raid penalties/rewards will not be given.
- Main tabs are `DailyTask`, `Inventory`, `Dungeon`, `Milestone`, and `Profile`.
- Additional routes include `Settings`, all quest categories, `LeaderBoard`, `Workout`, `SpiritTasks`, `Paywall`, `DungeonBattle`, and `InventoryShop`.

### Accounts, data, and monetization

- Google Sign-In is exchanged for a Firebase Auth credential.
- User records are read from and written to Firebase Realtime Database under `/users/<uid>`.
- Player state is also placed in device credential/keychain storage; personal quests and language selection use local asynchronous storage.
- Firebase Cloud Messaging and notification support are bundled.
- Google Mobile Ads/AdMob support is present, including interstitial/rewarded infrastructure.
- RevenueCat checks two entitlements: `remove_ads_3` and `premium_sub`. Google Play Billing 7.1.1 and Amazon IAP components are also bundled.
- Seven UI languages are present: English, French, Spanish, German, Portuguese, Turkish, and Vietnamese.

## Permissions and externally connected services

The decoded manifest requests or declares access for:

- Internet and network state.
- Vibration, wake lock, foreground service, and notifications.
- Google Play billing and license checking.
- Firebase/Google cloud messaging.
- Advertising ID and Android ad-services APIs.
- Biometric/fingerprint authentication support.
- Legacy external-storage read/write permissions.
- Numerous launcher badge permissions used to display notification counts on different Android launchers.

The application sets `android:allowBackup="false"`. It bundles Firebase Auth, Realtime Database, Messaging, Google Sign-In, Google Mobile Ads, RevenueCat, image picking, AndroidX WorkManager, and notification/badge libraries. The embedded Firebase project is named `sololevller` (with that spelling). An automated Hermes string scan found no obvious hardcoded secret beyond localhost/emulator URLs; that is not a security guarantee.

## Editable outputs

### 1. Rebuild-oriented Android tree

Directory: `SoloLeveller_editable/`

- `AndroidManifest.xml`: decoded and editable manifest.
- `res/`: decoded XML, images, icons, and values.
- `smali/` through `smali_classes5/`: editable Android/Dalvik bytecode (43,636 Smali files).
- `assets/index.android.bundle`: original Hermes bytecode containing the React Native app logic.
- `apktool.yml`: package/build metadata used by Apktool.

This is the most reliable tree for resource or Smali edits. An unchanged rebuild completed successfully.

### 2. Readable Android source export

Directory: `SoloLeveller_source/`

- JADX-exported Gradle-shaped project.
- 23,201 Java source files, including the Kotlin-origin `MainActivity` and `MainApplication` reconstructed as Java.
- Readable library and Android wrapper code plus decoded resources.

JADX finished with 88 method-level decompilation errors. The project also has placeholder dependencies and no Gradle wrapper, so it is an analysis/editing aid rather than a guaranteed compilable recreation of the original Android project.

### 3. Readable Hermes/React Native logic

Directory: `SoloLeveller_hermes/`

- `index.android.decompiled.js`: one editable JavaScript-like pseudocode file for the whole Hermes bundle (about 722,000 generated lines).
- `modules/`: 1,742 separately extracted Metro modules, which are easier to search and edit during analysis.

Important limitation: this JavaScript is reconstructed pseudocode, not the original React Native source. Editing it does **not** automatically update the APK, and the decompiler does not round-trip this JavaScript back into the original Hermes bundle. Functional changes should be made through resource/Smali edits, carefully targeted Hermes bytecode patches, or a new React Native project that reimplements the recovered behavior.

### 4. Rebuild proof

File: `SoloLeveller_rebuilt-unsigned.apk`

- Rebuilt from the unchanged `SoloLeveller_editable/` tree with Apktool 3.0.3.
- SHA-256: `BA94C83DE9E8879A991A5A997EEBFEC7E89386E13F6C37D9A917E94B7B616E94`.
- Contains five DEX files and no analysis-cache artifacts.
- Deliberately unsigned; `apksigner verify` fails as expected. It is not install-ready until signed with a certificate controlled by the editor.
- The rebuild proves that the decoded tree can be assembled. It does not prove runtime correctness, and the missing configuration splits remain a packaging concern.

## Useful evidence locations

- App identity, components, permissions, Firebase, ads, billing, and split requirements: `SoloLeveller_editable/AndroidManifest.xml`
- SDK/version metadata: `SoloLeveller_editable/apktool.yml`
- React Native entry activity: `SoloLeveller_source/app/src/main/java/com/app/sololeveller/MainActivity.java`
- Hermes-enabled React Native application wrapper: `SoloLeveller_source/app/src/main/java/com/app/sololeveller/MainApplication.java`
- Player state, levels, stats, streak, leaderboard, premium flags, and Firebase persistence: `SoloLeveller_hermes/modules/440_calculateLeaderBoardPoints.js`
- Navigation and screen map: `SoloLeveller_hermes/modules/515__getRequireWildcardCache.js`
- Daily/custom task behavior and penalties: `SoloLeveller_hermes/modules/516__getRequireWildcardCache.js`
- Google/Firebase onboarding and aura selection: `SoloLeveller_hermes/modules/1495__getRequireWildcardCache.js`
- RevenueCat entitlement handling: `SoloLeveller_hermes/modules/1566__getRequireWildcardCache.js`
- Language configuration: `SoloLeveller_hermes/modules/1581_changeLanguage.js`
- English feature/quest text: `SoloLeveller_hermes/modules/module_1583.js`
- Dungeon battle preview: `SoloLeveller_hermes/modules/1592__getRequireWildcardCache.js`
- React Native app configuration and AdMob application ID: `SoloLeveller_hermes/modules/module_1741.js`

## Verification status

| Check | Result |
|---|---|
| Original APK SHA-256 calculated | Passed |
| Original APK signature verification | Passed (v2, v3, and source stamp) |
| Manifest/resources decoded | Passed, with unresolved-reference warnings from Apktool |
| All five DEX files disassembled to Smali | Passed |
| Readable Android source export | Partial: output created; JADX reported 88 errors |
| Hermes HBC v96 parsed and decompiled | Passed |
| All detected Metro modules extracted | Passed (1,742 modules) |
| Apktool rebuild from editable tree | Passed |
| Rebuilt APK signed/installable | Not done; output is intentionally unsigned |
| Runtime/device behavior | Not tested |

## Bottom line

The supplied APK is a signed Google Play base package for a React Native **gamified habit, wellness, and fitness tracker** themed around Solo Leveling. Its editable/research forms have been recovered at the Android resource/Smali level, the readable Java wrapper level, and the Hermes JavaScript-pseudocode level. Exact original source code, original build configuration, signing keys, and missing App Bundle configuration splits cannot be recovered from this APK alone.
