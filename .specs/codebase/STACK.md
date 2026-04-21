# Technology Stack

**Analyzed:** 2026-04-21

## Core Platform & Kotlin

- **Kotlin:** 2.3.20
- **Kotlin Multiplatform:** 2.3.20
- **Gradle:** 8.11.2 (AGP)
- **Gradle Configuration Cache:** Enabled
- **Gradle Build Cache:** Enabled

## Mobile UI — Compose Multiplatform

- **Compose Multiplatform:** 1.10.3
- **Compose Material3:** 1.10.0-alpha05
- **Compose Runtime / Foundation / UI:** 1.10.3
- **Compose Components Resources:** 1.10.3
- **Compose UI Tooling / Preview:** 1.10.3
- **Compose Compiler Plugin:** 2.3.20

## Android

- **Compile SDK:** 36 | **Min SDK:** 28 | **Target SDK:** 36
- **Activity Compose:** 1.13.0
- **AppCompat:** 1.7.1
- **Core KTX:** 1.18.0
- **Lifecycle ViewModel Compose / Runtime Compose:** 2.10.0

## Backend — Ktor Server

- **Ktor Server Core + Netty:** 3.4.1
- **Ktor Server Test Host:** 3.4.1
- **Logback:** 1.5.32
- **JVM Target:** 11

## Web Frontend

- **React + React DOM:** 18.2.0
- **TypeScript:** 5.0.2
- **Vite:** 7.1.6
- **@vitejs/plugin-react:** 4.0.0

## Testing

- **Kotlin Test + Kotlin Test JUnit:** 2.3.20
- **JUnit:** 4.13.2
- **Ktor Server Test Host:** 3.4.1
- **AndroidX Test Ext JUnit:** 1.3.0 (declared, unused)
- **Espresso Core:** 3.7.0 (declared, unused)

## Build Tools

- **Android Gradle Plugin:** 8.11.2
- **Kotlin Multiplatform Plugin:** 2.3.20
- **Compose Multiplatform Plugin:** 1.10.3
- **Ktor Plugin:** 3.4.1
- **npm** (workspace root + webApp)

## Target Platforms

| Target | Details |
|--------|---------|
| Android | minSdk 28, targetSdk 36, JVM 11 |
| iOS | ARM64 + Simulator ARM64 (Compose Framework) |
| JVM | Java 11+ (server) |
| JS/Web | ES2015+, TypeScript definitions output |
