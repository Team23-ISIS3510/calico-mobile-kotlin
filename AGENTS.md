# AGENTS.md - Calico Mobile Kotlin

## Build & Run
```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
./gradlew test           # Unit tests
./gradlew connectedCheck # Instrumented tests (requires emulator/device)
```

## Architecture
- **Single module**: `:app`
- **Layers**: UI (compose) -> Domain (use cases, repos) -> Data (API, local storage)
- **DI**: ServiceLocator pattern (manual, see `di/ServiceLocator.kt`)

## Key Commands
- Tests use **Kotest** (`io.kotest`), not JUnit 5 directly
- Compose with Material3 enabled in `app/build.gradle.kts`

## Important URLs
- **API Base**: `http://157.253.162.173:3000/` (hardcoded in `RetrofitClient.kt`)
- Uses Retrofit + OkHttp with auth interceptors and token refresh

## Environment
- `.env` file exists but is NOT automatically loaded by Android
- API keys stored in `.env` - must be manually managed or configured in `local.properties`

## Special Features
- Shake detection triggers bug report dialog (`ShakeDetector.kt`)
- Session alerts via notifications
- Telemetry with latency measurement

## Testing
- Unit tests: `app/src/test/java/`
- Instrumented tests: `app/src/androidTest/java/`