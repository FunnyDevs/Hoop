# Hoop architecture

Modular Clean Architecture, Domain-Driven Design in the domain, dependency injection with Koin, and a design system following [Anatomy of a design system](https://developer.android.com/develop/ui/compose/designsystems/anatomy).

## Modules and the dependency rule

```
composeApp ──► presentation:onboarding ─┐
   │       └──► presentation:home ──────┴──► domain ◄── data
   │                    │
   └────────────────────┴──► core:designsystem
```

| Module | Contents | May depend on |
|---|---|---|
| `domain` | Entities, Value Objects, aggregates, domain services, repository interfaces, use cases | only Kotlin and coroutines |
| `data` | Repository implementations (Parse via Ktor), DTOs, mappers, technical errors → `DomainError`, fake repositories | `domain` |
| `core:designsystem` | Tokens, theme, fonts, reusable components with no text and no business logic | nothing |
| `presentation:onboarding` | Welcome, sign-in, sign-up and recovery screens, ViewModels, Intent/Effect contracts, localized strings, error → text mapping | `domain`, `core:designsystem` |
| `presentation:home` | Home: map with Hoop areas and pins, Hoop chips, post carousel, full-screen card with replies, ask bar and composer, notifications | `domain`, `core:designsystem` |
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

The Parse server is never in the code: `ParseConfig` comes from `local.properties` (not versioned) on Android and from `Info.plist` on iOS. On Android there is one build flavor per server (`local`, `staging`, `production`): each reads `hoop.parse.<flavor>.baseUrl|applicationId|clientKey`, falling back to `hoop.parse.baseUrl|applicationId|clientKey`. Only `local` allows plain http. Without configuration the app uses `FakeAuthRepository`.

## Theme and design system

Anatomy: **tokens → theme → components**.

- Tokens: `HoopPalette` (the only place with hexadecimal values, internal), `HoopColors` (semantic roles), `HoopTypography`, `HoopShapes`, `HoopSpacing`, `HoopSizes`, `HoopElevation`, `HoopMotion`.
- Theme: `HoopTheme { }` provides the tokens through `CompositionLocal` and maps Material3 to them; read them with `HoopTheme.colors`, `HoopTheme.typography`, ...
- Components: `HoopButton` (+ `HoopButtonDefaults`), `HoopBrandIconButton`, `HoopLabeledDivider`, `HoopLogoBadge`, `HoopCardFrame`, `HoopMediaCard`, `HoopQuestionCard`, `HoopUserChip`, `HoopPlayBadge`, `HoopMapBackdrop`, `HoopRadiusMap` (real MapLibre map with an offline fallback). Texts and images come from the caller.
- Light theme only. Static Bricolage Grotesque and Figtree fonts in `core/designsystem/src/commonMain/composeResources/font`.

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

## Live camera (photos and videos)

Photos and videos are only taken live, with the camera of the app, never picked from the gallery: it is the guarantee that they are authentic and from that moment. The domain enforces it (`PostDraft` refuses a `MediaRef` without `capturedAtEpochMillis`, `Violation.PostMediaNotLive`); the capture time and, when known, the position travel with the media.

- UI flow in `presentation:home` (`camera/HomeCamera.kt`): live camera, confirmation of the shot, composer with the single thumbnail. State in `HomeUiState.capture` and `ComposerUi.media`.
- What depends on the platform sits behind `CameraHandle` and a few `expect` functions (`camera/CameraPlatform.kt`): CameraX on Android, AVFoundation on iOS. Files go to the private cache of the app.
- Camera and microphone are both required (a video without sound is not accepted): without them the camera screen explains why and sends the user to the settings. A tap takes a photo, a press held for 0.5 s records up to 15 s (`PostPolicy`).
- iOS: written but never compiled (no Xcode here); the Info.plist needs `NSCameraUsageDescription` and `NSMicrophoneUsageDescription`.

## Publishing posts

`PublishPostUseCase` builds a `PostDraft` (type rules, live media, current position) and hands it to `PostsRepository`. `ParsePostsRepository` (data) uploads the photo or video with `POST /files/{name}` and then calls the Cloud Function `saveNewPost` (`type` Q/P/V, position, `hoopId` only for extra Hoops, the uploaded file as a Parse File reference, `cuuid`). The bytes of the camera file are read through the `MediaFileReader` port, implemented per platform in `composeApp` (Android `File`, iOS `NSData`). Server refusals map to domain errors: 5003 `PostTypeNotAllowedInHoop`, 5004 `PositionOutsideHoop`, 5005 `HoopNotFound`. Without server configuration (or without a reader) the fake store is used.

The street of a post is found on the device through the `AddressResolver` port (Android `Geocoder`, iOS `CLGeocoder`) and sent as the optional `address`; the server never geocodes. The lookup is best effort and bounded (`PostPolicy.ADDRESS_LOOKUP_TIMEOUT_MILLIS`): without a street the post is published all the same. The address is capped at `PostPolicy.ADDRESS_MAX_LENGTH`.

Media metadata is produced on the device by the camera code and travels in `MediaRef` (`width`/`height` as the viewer sees them, `durationMillis`, and for a video `thumbnailUri`, its first frame saved as a JPEG). `MediaRef.orientation` (landscape/portrait/square) is derived from the size. `saveNewPost` receives `orientation`, `width`, `height`, `durationMillis` (videos only) and `thumbnail` (a file uploaded as `{cuuid}-thumb.jpg`); the thumbnail is best effort and never blocks publishing.

## Offline queue of posts

When publishing fails because there is no connection (`DomainError.Network`), `PublishPostUseCase` does not lose the post: it saves the `PostDraft` through the `PendingPostsRepository` port and answers `PublishOutcome.Queued`. `SendPendingPostsUseCase` sends the waiting posts oldest first: it removes what went out, stops (keeping the rest) when the network or the session fails again, and discards what the server refuses for good. The home runs it when it opens and after each successful publication. The same `clientId` is used on every try, so the server never creates a post twice.

The queue lives in a local SQLite database (SQLDelight 2.0.2, schema in `data/src/commonMain/sqldelight`, Android and native drivers provided by the platform modules through `SqlDriverFactory`; without one the queue is in memory). Only the data of the draft is saved: the photo or video stays in the private cache of the app, so if the system clears it before the post is sent, the post is discarded.

### Publishing feedback in the home

The composer closes as soon as the user taps publish. A card at the top of the home (`HomePublish.kt`) follows the post: upload progress with an X to cancel (`PostsRepository.publish(draft, onProgress)`), the reason and a button when it failed (too far, too many requests, generic), or the waiting posts when there is no connection. Tapping the queue card opens the sheet "In attesa di rete", where each waiting post can be cancelled. When the session expired the posts are kept too and the card asks to sign in again (`HomeRoute(onSignInRequested)`: the host signs out and goes to the welcome screen). Any change to the local database schema needs a numbered migration (`data/src/commonMain/sqldelight/.../N.sqm`).
