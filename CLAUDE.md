# Hoop: rewrite in Compose Multiplatform

This file summarizes the decisions made in a long design session. Read it in full before writing code.

## The product

Hoop is a proximity-based social network. Each user sees what happens within an adjustable radius around them. They can add other areas, called **Hoops**, by picking a point and a radius on the map.

- Three post types: **question**, **photo**, **video**.
- Photos, videos and questions can be published **only in the initial Hoop** ("Home"). In the other Hoops **only questions** can be published. The UI must reflect this: the compose menu offers Photo, Video and Question in Home and only Question elsewhere.
- All posts are always mixed together, **with no filters**.

## The old project (to reuse from)

`/Users/pierfrancesco/Projects/Hoop`: Android with Litho, Conductor, Dagger, RxJava and MapLibre. It has a `shared` module with a small Compose skeleton. Read-only: never modify it.

- `domain/commonMain` has models, repositories and use cases meant to be multiplatform: SignIn, SignUp, ResetPassword, CheckIfEmailAlreadyExists, SendNewQuestionPost, SendNewMediaPost and others. Half of it still uses RxJava/GeoFire, so only models and use case signatures are ported.
- Colors are in `common/src/main/res/values/colors.xml`.
- The old home was Facebook Paper style: map in the middle, posts below, a sliding panel (`SlidingUpPanelLayout`), long press on the map to hide the posts and see the full-screen map.

## Decided design

**Light** theme (no night theme). The design is a fusion of two proposals: a full-screen map with large serif cards, and Hoops as Stories-like circles with a mini-map inside.

- **Colors:** orange accent `#E67E22` (`accent` of the old project), dark gray primary `#373E41` (`outer_space`), dark orange `#CD6509` for text. For links on white use `#A65300` (sufficient contrast).
- **Fonts:** Fraunces (headings) and Figtree (body).
- **Post cards:** photos and videos fill the **whole** card frame, with the text overlaid. Questions are colored cards (yellow `#FFE8A8`, etc.).
- **Welcome:** the "Fading map" proposal, i.e. a collage of tilted cards over the map that fades into the light background, with "Create an account", "Sign in" and Google, Facebook, Apple. No hard white panel.

## Gestures and animations (Paper style)

Spring physics, not fixed curves.

- The card carousel scrolls with snapping.
- Tapping a card **expands it from its on-screen position to full screen**, interpolating its rectangle. Dragging down closes it.
- Dragging the cards down or a **long press on the map** enters full-screen map mode. There the Hoops are edited: radius handle, removal, adding with a ghost circle, and moving.

## Sign-in flow

Screens: welcome, sign in (including wrong credentials), 4-step sign-up (email and password with a typo hint such as `gmial.com` and a strength meter, username with availability check and terms, location permission, radius of the first Hoop), final screen. 4-step password recovery: email, check your inbox with a resend after 20 seconds, new password, confirmation.

## References (prototypes and design)

All pages are private to the user's account. The behavior of the HTML prototypes is the specification to replicate.

- Sign-in flow canvas (Claude Design, 12 screens): https://claude.ai/artifact/R8ACsZzGLFngr4fc14Nf5c (the latest welcome design, including the bottom buttons, is here)
- Clickable sign-in flow prototype, with errors and animations: https://claude.ai/artifact/QEnyfmHVEm4dH7Wor3wuxt
- Animated home prototype (carousel, full-screen cards, map mode, composer, notifications): https://claude.ai/artifact/NovYX12Pom4FHzjuAsECpj
- Static home in the chosen version: https://claude.ai/artifact/5LbNN2xNRkoB11GHWWMLW1
- Welcome variants: https://claude.ai/artifact/L7UBqCfDAc3MpNGFaQvDRy

The photos in the prototypes are placeholders from Pexels.

## Decisions made

- Architecture from scratch: Kotlin 2.1, Compose Multiplatform 1.7.3, JetBrains navigation, Koin, Ktor, kotlinx.serialization. SQLDelight will be added when local persistence is needed.
- Map: MapLibre Compose 0.6.0 (`dev.sargunv.maplibre-compose`, the last version compatible with Kotlin 2.1 / Compose Multiplatform 1.7.3). It is used in the radius step of sign-up (`HoopRadiusMap`); style and fallback center come from `gradle.properties` (`hoop.map.*`, free OpenFreeMap light style). The welcome and location screens still use drawn illustrations (`HoopMapBackdrop`, `HoopLocationMap`). When the style cannot load (offline) the radius step shows the drawn map as a fallback.
- Backend: Parse is kept and called via REST with Ktor (a PHP backend would still need a VPS, so no savings). The new server is Parse Server 9 in `/Users/pierfrancesco/WebstormProjects/Hoop-server` (functions `signUp`, `checkAvailability`, `requestPasswordReset`); the old server is in `/Users/pierfrancesco/Projects/Hoop-server` (read-only). The old project's `settings.gradle` contains a secret Mapbox token in plain text: it must be revoked and never copied.
- Static fonts generated from the OFL variable fonts (Fraunces opsz 72, Figtree) in `core/designsystem/src/commonMain/composeResources/font`.
- Location: the `LocationProvider` port (domain) is implemented per platform in `composeApp`: Android uses the platform `LocationManager` plus a runtime permission bridge (`ActivityPermissionRequester`, attached by `MainActivity`); iOS uses CoreLocation (`IosLocationProvider`, written but never compiled, needs `NSLocationWhenInUseUsageDescription`). Both are wired through `initKoin(platformModules)`.
- iOS: the code is there (`MainViewController`) but there is no Xcode project, and Xcode is not installed on this machine, so iOS has never been compiled.

## Architecture

Multi-module project with Clean Architecture and DDD: see `ARCHITECTURE.md` (modules `domain`, `data`, `core:designsystem`, `presentation:onboarding`, `composeApp`). Rules are verified by `./gradlew checkArchitecture`, which also runs on every app build. No hardcoded string, color or address: strings in `composeResources/values`, colors in tokens, server from `local.properties` (`hoop.parse.baseUrl`, `hoop.parse.applicationId`, `hoop.parse.clientKey`). All code comments are written in English.

## Commands

- Android build: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :composeApp:assembleDebug`
- Tests: `./gradlew :domain:testDebugUnitTest :data:testDebugUnitTest`
- Architecture rules: `./gradlew checkArchitecture`

## How to work

Ask the user before making big architectural choices. The user writes in Italian. Do not touch the HTML prototypes: they stay as a reference.
