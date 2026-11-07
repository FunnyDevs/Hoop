# Hoop architecture

Modular Clean Architecture, Domain-Driven Design in the domain, dependency injection with Koin, and a design system following [Anatomy of a design system](https://developer.android.com/develop/ui/compose/designsystems/anatomy).

## Modules and the dependency rule

```
composeApp ──► presentation:onboarding ──► domain ◄── data
   │                    │
   └────────────────────┴──► core:designsystem
```

| Module | Contents | May depend on |
|---|---|---|
| `domain` | Entities, Value Objects, aggregates, domain services, repository interfaces, use cases | only Kotlin and coroutines |
| `data` | Repository implementations (Parse via Ktor), DTOs, mappers, technical errors → `DomainError`, fake repositories | `domain` |
| `core:designsystem` | Tokens, theme, fonts, reusable components with no text and no business logic | nothing |
| `presentation:onboarding` | Screens, ViewModels, Intent/Effect contracts, localized strings, error → text mapping | `domain`, `core:designsystem` |
| `composeApp` | Composition root: Koin, navigation, platform configuration | all of them |

The rule is verified automatically: `./gradlew checkArchitecture` (it also runs on every app build). The rules live in `build-logic/convention/src/main/kotlin/ArchitectureCheckPlugin.kt`.

## Domain-Driven Design

- **Value Objects** (immutable, validated at creation, `create()` returns a `DomainResult`): `Email`, `Username`, `Password`, `LoginIdentifier`, `GeoPoint`, `Radius`, `PostText`, `MediaRef`, the ids.
- **Entities** (identity = id): `User`, `Hoop` (`Home` / `Extra`), `Post`.
- **Aggregate Root**: `UserHoops` guarantees the invariants (a single, non-removable initial Hoop, at most 20 extras).
- **Domain rules**: what can be published where lives in `Hoop.acceptedPostTypes` and `PostDraft.create` (Photo/Video/Question in Home, only Question in the other Hoops).
- **Domain Service**: `EmailTypoDetector` (suggests `gmail.com` for `gmial.com`).
- **Numeric policies** are named in `common/Policies.kt` (lengths, radius 200 m–20 km, default 1 km). No magic numbers.
- **Errors**: `DomainError` and `Violation`, with no text. Language is a presentation concern (`DomainErrorMessages.kt`).

## Dependency injection and inversion of control

The domain defines the interfaces (`AuthRepository`, …); `data` implements them; the app wires them in `composeApp/.../di/Koin.kt`. The domain does not know Koin: use cases are registered in the app's `di/DomainModule.kt`. Each layer exposes its own module (`dataModule(config)`, `onboardingPresentationModule`). ViewModels receive their dependencies through the constructor.

The Parse server is never in the code: `ParseConfig` comes from `local.properties` (`hoop.parse.baseUrl`, `hoop.parse.applicationId`, `hoop.parse.clientKey`, not versioned) on Android and from `Info.plist` on iOS. Without configuration the app uses `FakeAuthRepository`.

## Theme and design system

Anatomy: **tokens → theme → components**.

- Tokens: `HoopPalette` (the only place with hexadecimal values, internal), `HoopColors` (semantic roles), `HoopTypography`, `HoopShapes`, `HoopSpacing`, `HoopSizes`, `HoopElevation`, `HoopMotion`.
- Theme: `HoopTheme { }` provides the tokens through `CompositionLocal` and maps Material3 to them; read them with `HoopTheme.colors`, `HoopTheme.typography`, ...
- Components: `HoopButton` (+ `HoopButtonDefaults`), `HoopBrandIconButton`, `HoopLabeledDivider`, `HoopLogoBadge`, `HoopCardFrame`, `HoopMediaCard`, `HoopQuestionCard`, `HoopUserChip`, `HoopPlayBadge`, `HoopMapBackdrop`, `HoopRadiusMap` (real MapLibre map with an offline fallback). Texts and images come from the caller.
- Light theme only. Static Fraunces and Figtree fonts in `core/designsystem/src/commonMain/composeResources/font`.

## Text and constants

No text in code: every string is in the module's `composeResources/values/strings.xml` (Italian). Layout measurements live in named objects (`WelcomeLayout`, `HoopCardDefaults`, ...). Colors only from tokens. Addresses and keys only from configuration. Code comments are in English.

## Anti-patterns in the old project and migration path

| Anti-pattern found | Why it is a problem | Migration |
|---|---|---|
| `domain/commonMain` with RxJava (81 of 164 files), `SchedulerProvider`, GeoFire | The domain depends on frameworks and on Android; it is not multiplatform | Removed: coroutines and `Flow`, geography as a Value Object (`GeoPoint`). The geohash moves to the server |
| Room entities and network DTOs (`PostEntityModel`, `UserApiModel`) used in use cases | The domain knows about persistence and the server format | Dedicated domain models; DTOs and mappers only in `data` |
| Use cases that create data (UUID, `Date`, geohash, `SyncState`) | Domain logic mixed with persistence and synchronization | Rules in the entities (`PostDraft.create`); synchronization in the repository |
| Parse SDK and direct calls in repositories, `YOUR_APP_ID` placeholders and tokens in Gradle files | Secrets in the repository, impossible to change server | External `ParseConfig`, REST via Ktor, errors mapped to `DomainError` |
| Strings, colors and measurements scattered in the UI (Litho, XML) | Impossible to change theme or language in one place | Tokens, `HoopTheme`, `strings.xml`, automatic verification |
| Dagger with a global graph, Conductor, Activity-centric | Tied to Android, hard to test | Koin, Compose navigation, ViewModels with Intent/Effect |
| The rule "only questions outside Home" only in the UI | Can be bypassed, duplicated | In the domain (`Hoop.accepts`) and on the server |

To migrate another feature: 1) model and rules in `domain` with tests; 2) repository interface; 3) implementation in `data` and mapper; 4) use case; 5) ViewModel and screen with strings and design system components; 6) `checkArchitecture` must stay green.

## Adding a feature

A new `presentation:<name>` module with the same `build.gradle.kts` as `presentation:onboarding` (no dependency on `data`), its own Koin module, and an entry in `Koin.kt` and in the navigation. Add its path to the rules in `ArchitectureCheckPlugin.kt`.
