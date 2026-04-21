# Project Structure

**Analyzed:** 2026-04-21

## Directory Tree (3 levels)

```
PompeiaRunners/
├── composeApp/
│   ├── src/
│   │   ├── androidMain/kotlin/.../   MainActivity.kt, AndroidManifest.xml, res/
│   │   ├── commonMain/kotlin/.../    App.kt (Compose root)
│   │   ├── commonMain/composeResources/
│   │   ├── commonTest/kotlin/.../    ComposeAppCommonTest.kt
│   │   └── iosMain/kotlin/.../      MainViewController.kt
│   └── build.gradle.kts
│
├── shared/
│   ├── src/
│   │   ├── commonMain/kotlin/.../   Platform.kt, Greeting.kt, Constants.kt
│   │   ├── androidMain/kotlin/.../  Platform.android.kt
│   │   ├── iosMain/kotlin/.../      Platform.ios.kt
│   │   ├── jvmMain/kotlin/.../      Platform.jvm.kt
│   │   ├── jsMain/kotlin/.../       Platform.js.kt
│   │   └── commonTest/kotlin/.../   SharedCommonTest.kt
│   └── build.gradle.kts
│
├── server/
│   ├── src/
│   │   ├── main/kotlin/.../         Application.kt
│   │   ├── main/resources/          logback.xml
│   │   └── test/kotlin/.../         ApplicationTest.kt
│   └── build.gradle.kts
│
├── webApp/
│   ├── src/
│   │   ├── index.tsx                React entry point
│   │   └── components/
│   │       ├── Greeting/            Greeting.tsx + Greeting.css
│   │       └── JSLogo/              JSLogo.tsx + JSLogo.css
│   ├── index.html
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── package.json
│
├── iosApp/
│   ├── iosApp/                      iOSApp.swift, ContentView.swift, Assets.xcassets/
│   ├── iosApp.xcodeproj/
│   └── Configuration/
│
├── gradle/
│   ├── libs.versions.toml           Version catalog (single source of truth)
│   └── wrapper/
│
├── .specs/                          Project analysis & planning docs
├── build.gradle.kts                 Root Gradle (plugin declarations)
├── settings.gradle.kts              Module includes
├── gradle.properties                JVM args, caching flags, AndroidX flags
├── package.json                     npm workspace root
├── AGENTS.md
├── CLAUDE.md
└── gradlew / gradlew.bat
```

## Module Summary

| Module | Purpose | Key Files |
|--------|---------|-----------|
| `composeApp` | Mobile UI (Android + iOS) | `App.kt`, `MainActivity.kt`, `MainViewController.kt` |
| `shared` | Common logic + platform abstraction | `Platform.kt`, `Greeting.kt`, `Constants.kt` |
| `server` | Ktor REST API backend | `Application.kt`, `logback.xml` |
| `webApp` | React admin/web frontend | `index.tsx`, `Greeting.tsx` |
| `iosApp` | Swift shell wrapping composeApp | `ContentView.swift`, `iOSApp.swift` |

## Where Things Live

**Platform abstraction:**
- Interface + expect: `shared/src/commonMain/.../Platform.kt`
- Actuals: `shared/src/{androidMain,iosMain,jvmMain,jsMain}/.../Platform.*.kt`

**Shared constants:**
- `shared/src/commonMain/.../Constants.kt` (e.g. `SERVER_PORT = 8080`)

**Server config:**
- Port: `Constants.kt` (shared)
- Logging: `server/src/main/resources/logback.xml`

**Dependency versions:**
- `gradle/libs.versions.toml`
