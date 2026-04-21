# Code Conventions

**Analyzed:** 2026-04-21

## Package Structure

- Root package: `com.example.pompeiarunners`
- Shared: `com.example.pompeiarunners` (commonMain)
- Server: `com.example.pompeiarunners` (Application.kt)
- Web: TypeScript modules in `src/components/{Feature}/`

## Kotlin Naming

| Element | Convention | Examples |
|---------|-----------|---------|
| Classes | PascalCase | `Platform`, `Greeting`, `AndroidPlatform`, `JsPlatform` |
| Composables | PascalCase | `App()`, `AppAndroidPreview()` |
| Functions | camelCase | `getPlatform()`, `greet()`, `onCreate()` |
| Constants | UPPER_SNAKE | `SERVER_PORT = 8080` |
| Interfaces | PascalCase, no `I` prefix | `Platform` (not `IPlatform`) |

## TypeScript Naming

| Element | Convention | Examples |
|---------|-----------|---------|
| Components | PascalCase | `Greeting`, `JSLogo` |
| Functions | camelCase | `handleAnimationEnd` |
| State | camelCase | `isVisible`, `isAnimating` |
| Types | Explicit generics | `useState<boolean>()`, `AnimationEvent<HTMLDivElement>` |

## File Organization

### Kotlin (KMP)
- Common code: `src/commonMain/kotlin/...`
- Platform-specific: `src/{androidMain,iosMain,jvmMain,jsMain}/kotlin/...`
- Tests: `src/commonTest/kotlin/...`
- Flat structure inside packages (no subdirectories yet)

### TypeScript / React
- Feature folders: `src/components/{Feature}/{Component.tsx + Component.css}`
- Co-located CSS per component
- Entry at `src/index.tsx`

### Gradle
- Kotlin DSL throughout (`build.gradle.kts`, not Groovy)
- Version catalog is single source of truth: `gradle/libs.versions.toml`
- Plugin aliases: `alias(libs.plugins.kotlinMultiplatform)`
- JVM target explicitly `JVM_11` in `compilerOptions`

## Code Style — Kotlin

```kotlin
// Modifier chaining (fluent, multi-line)
Column(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.primaryContainer)
        .safeContentPadding()
        .fillMaxSize()
)

// State
val showContent by remember { mutableStateOf(false) }

// Ktor route handlers — inline lambda with receiver
get("/") { call.respondText("...") }
```

## Code Style — TypeScript

```typescript
// Explicit types on params and state
const [isVisible, setIsVisible] = useState<boolean>(false);
const handleAnimationEnd = (event: AnimationEvent<HTMLDivElement>) => { ... }
```

## tsconfig Enforcement
- `strict: true`
- `noUnusedLocals: true`
- `noUnusedParameters: true`

## Observed Anti-patterns

1. Strings hardcoded in UI (`"Click me!"`, `"Compose: "`) — no string resources
2. No logging statements (Logback configured but unused)
3. Flat package hierarchy with no feature subdirectories
4. No inline comments or documentation
