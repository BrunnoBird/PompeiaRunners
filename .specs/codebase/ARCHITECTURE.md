# Architecture

**Analyzed:** 2026-04-21

**Pattern:** Kotlin Multiplatform starter template — modular monorepo, no MVVM/MVI/Clean layers yet.

## Module Structure

```
PompeiaRunners (root)
├── composeApp/    # KMP Mobile UI (Android + iOS)
├── shared/        # Common business logic + platform abstractions
├── server/        # Ktor REST API backend (JVM)
├── webApp/        # React + TypeScript admin/web frontend
└── iosApp/        # Native Swift shell (wraps composeApp)
```

## Expect/Actual Pattern

`shared/src/commonMain/.../Platform.kt` defines the interface and expect function:

```kotlin
interface Platform { val name: String }
expect fun getPlatform(): Platform
```

Each target provides the `actual` implementation:

| Target | File | Implementation |
|--------|------|----------------|
| Android | `Platform.android.kt` | `android.os.Build.VERSION.SDK_INT` |
| iOS | `Platform.ios.kt` | `UIKit.UIDevice.currentDevice.systemName()` |
| JVM | `Platform.jvm.kt` | `System.getProperty("java.version")` |
| JS | `Platform.js.kt` | Returns `"Web with Kotlin/JS"` |

## Data Flow

### Mobile (Android / iOS)
```
MainActivity.kt / MainViewController.kt
  → App() composable (commonMain)
    → mutableStateOf() for local UI state
    → Greeting().greet() (from shared)
      → getPlatform() (expect/actual per target)
```

### Backend (Ktor)
```
Application.kt main()
  → embeddedServer(Netty, port = SERVER_PORT)
    → module() → routing { get("/") { respondText(...) } }
      → Greeting().greet() (same shared class)
```

### Web Frontend (React)
```
index.tsx → ReactDOM.render(<Greeting />)
  → new KotlinGreeting() (KMP-compiled JS library)
    → .greet() method call
  → useState<boolean>() for local UI state
```

### Frontend → Backend: NOT IMPLEMENTED
Mobile and web apps do not call the Ktor server. There is no HTTP client configured.

## Dependency Graph

```
composeApp  ──► shared
server      ──► shared
webApp      ──► shared (compiled to JS)
iosApp      ──► composeApp (Swift framework)
```

## Current Architectural Gaps

| Gap | Impact |
|-----|--------|
| No DI (Koin/Hilt) | Direct instantiation; untestable |
| No Networking (Ktor Client) | Mobile/web can't talk to server |
| No Database (SQLDelight/Room) | No persistence anywhere |
| No Authentication | No user identity or access control |
| No ViewModel / StateFlow | State lives in composables, doesn't survive config changes |
| No Navigation | Single-screen apps on mobile and web |
| No error handling layer | No error states in UI or server |
| No API contract | Server returns plain text, not JSON |
