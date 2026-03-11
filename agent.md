# AGENTS.md - PholusChat Development Guide

This document provides guidelines for AI agents working on the PholusChat Android project.

---

## Build & Development Commands

### Gradle Wrapper
```bash
# Use Gradle wrapper (preferred)
./gradlew <task>

# If wrapper not available, use system gradle
gradle <task>
```

### Build Tasks
```bash
# Debug builds (all variants)
./gradlew assembleDebug
./gradlew assembleOssDebug
./gradlew assemblePlayDebug

# Release builds (all variants)
./gradlew assembleRelease
./gradlew assembleOssRelease
./gradlew assemblePlayRelease

# Clean build
./gradlew clean assembleDebug
```

### Code Quality
```bash
# Run lint
./gradlew lint

# Run lint with fixes
./gradlew lintFix

# Format code (if spotless is configured)
./gradlew spotlessApply
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.pholuschat.data.util.CurlParserTest"

# Run a specific test method
./gradlew test --tests "com.pholuschat.data.util.CurlParserTest.parse*"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Build with debug info for debugging
./gradlew assembleDebug -Dorg.gradle.debug=true
```

### Dependency Management
```bash
# Show dependency tree
./gradlew dependencies

# Show dependencies for specific configuration
./gradlew app:dependencies --configuration debugRuntimeClasspath
```

---

## Code Style Guidelines

### Project Structure
```
app/src/main/java/com/pholuschat/
├── data/           # Data layer (repositories impl, utilities)
├── domain/         # Domain layer (models, repository interfaces, use cases)
├── ui/             # Presentation layer (screens, components, theme)
└── *.kt           # Application entry points
```

### Kotlin Conventions

#### Naming
- **Classes/Interfaces**: `PascalCase` (e.g., `ChatScreen`, `ApiConfig`)
- **Functions**: `camelCase` (e.g., `sendMessage`, `parseCurl`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_TOKEN_LIMIT`)
- **Files**: `PascalCase.kt` for classes, `snake_case.kt` for utilities

#### Imports
- Group imports in this order:
  1. Android/Kotlin stdlib
  2. Jetpack Compose
  3. Third-party libraries
  4. Internal project imports
- Use explicit imports (no wildcard `*`)

#### Functions
- Keep functions short (under 30 lines)
- Use early returns for error conditions
- Use meaningful parameter names
- Add KDoc for public APIs

```kotlin
// Good
fun parseCurlCommand(command: String): CurlConfig {
    if (command.isBlank()) {
        return CurlConfig()
    }
    return CurlParser.parse(command)
}

// Avoid
fun parse(command: String): CurlConfig {
    return try {
        CurlParser.parse(command)
    } catch (e: Exception) {
        CurlConfig()
    }
}
```

### Compose Guidelines

#### State Management
- Use `remember` for UI state
- Use `rememberSaveable` for persistent state
- Use `mutableStateOf` with proper scoping
- Prefer `StateFlow` for ViewModel state

```kotlin
// Good - ViewModel with StateFlow
class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
}

// Good - Compose state
@Composable
fun ChatScreen() {
    var text by remember { mutableStateOf("") }
}
```

#### Component Structure
- Extract reusable components
- Use modifier chains for readability
- Keep preview functions for debugging

```kotlin
@Composable
fun MessageBubble(
    message: ChatMessage,
    onReply: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}

@Preview
@Composable
private fun MessageBubblePreview() {
    MessageBubble(
        message = ChatMessage(...),
        onReply = {}
    )
}
```

### Error Handling

- Use `Result<T>` for operations that can fail
- Never silently swallow exceptions
- Log errors appropriately
- Show user-friendly error messages

```kotlin
// Good
suspend fun sendMessage(config: ApiConfig): Result<String> {
    return try {
        val response = apiClient.send(config)
        Result.success(response)
    } catch (e: IOException) {
        Result.failure(NetworkException("Failed to send message", e))
    } catch (e: Exception) {
        Result.failure(UnknownException(e))
    }
}
```

### Testing Guidelines

- Test one thing per test
- Use descriptive test names
- Follow AAA pattern: Arrange, Act, Assert
- Mock external dependencies

```kotlin
class CurlParserTest {
    @Test
    fun `parse extracts method from curl command`() {
        // Arrange
        val curl = "curl -X POST https://api.example.com"

        // Act
        val config = CurlParser.parse(curl)

        // Assert
        assertEquals("POST", config.method)
    }
}
```

### Dependency Injection

- Use Hilt for dependency injection
- Inject interfaces over implementations
- Use `@HiltViewModel` for ViewModels

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val apiRepository: ApiRepository
) : ViewModel() {
    // ...
}
```

### Git Commit Messages

- Use imperative mood
- Keep subject line under 72 characters
- Reference issues/PRs

```
Add cURL converter feature

- Parse curl commands to extract method, url, headers, body
- Generate Python code from parsed config
- Add import to API configuration

Closes #12
```

---

## Architecture

PholusChat follows Clean Architecture with MVVM:

1. **UI Layer**: Compose screens, components, ViewModels
2. **Domain Layer**: Models, repository interfaces, use cases
3. **Data Layer**: Repository implementations, API clients, local storage

Data flows: UI → ViewModel → Repository → Data Source

---

## Notes

- This is a privacy-focused chat app - no analytics/tracking
- Supports universal REST API configuration
- Uses Material You dynamic theming
