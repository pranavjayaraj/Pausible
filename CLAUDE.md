# Feature Implementation Guide

> **Read this before implementing ANY new feature.** Every feature must mirror the

> structure, conventions, and rules below so the codebase stays consistent.
>

---

## 1. Architecture in one paragraph

 Each feature is a **stateless `XxxRoute` composable** placed into that graph —
**features are NOT Activities and never build a `NavController`**. UI state is driven by an
**Orbit MVI ViewModel** (`BaseViewModel<State, SideEffect>`) using `intent { }`, `reduce { }`,
and `postSideEffect(...)`. Cross-feature navigation and app-exit go through an injected
**`Navigator` seam** (the leaf `:navigation` module): the ViewModel emits a
`NavEvent`/`XxxDestination`, and a single app-owned host (`ObserveNavigation`) applies it to
the real `NavController`. Intra-feature screen changes are **just state** (rendered from a
`sealed` step in the state), not navigation. Dependencies are injected with **Hilt**; data
flows through **UseCases/Repositories** returning `ApiResult.Success/Error`; reusable
side-capabilities (audio record/play, timers) are **Delegates** mixed into the ViewModel with
Kotlin `by` delegation. Composables are **stateless** and communicate only through passed
lambdas.

---

## 2. Package / file layout

Replace `Xxx` with the feature name. Create exactly this structure:

```
feature/.../<feature>/
  api/                     # SUBMODULE :feature:<feature>:api — the feature's navigation contract
    XxxDestination.kt      # @Serializable destination(s) implementing Screen; args = ctor properties
  XxxState.kt              # @[Stable Keep] state + nested @Stable state + sealed statuses/steps
  XxxViewModel.kt          # Orbit MVI ViewModel: intent()/reduce()/postSideEffect() + injected Navigator
  XxxConstants.kt          # all event/page/action/status strings + dimens
  navigation/
    XxxIntent.kt           # sealed interface of user intents
    XxxSideEffect.kt       # sealed class of FEATURE-LOCAL one-shot effects (NOT navigation)
  ui/
    XxxRoute.kt            # feature entry composable: hiltViewModel(), collect state, side effects, BackHandler
    XxxScreen.kt           # stateless @Composable screens/sheets (+ @StringDef screen/tab markers)
  utils/                   # injected delegates (interface + @Inject impl) + pure util objects

navigation/                # shared LEAF module — pure Kotlin, KMP-ready, NO androidx in its public API
  Navigator.kt             # Navigator interface + NavEvent (Navigate/Pop/Exit)
  NavigatorImpl.kt         # @Singleton impl over a buffered Channel + Hilt @Binds module
  ObserveNavigation.kt     # the single host: collects events (repeatOnLifecycle) → NavController
  Screen.kt                # Screen marker interface (destinations live in feature api modules)

app/
  MainActivity.kt          # the ONE Activity: owns the NavHost, wires ObserveNavigation,
                           #   registers each feature as composable<XxxDestination> { XxxRoute(...) }
```

> The `:navigation` module is a **leaf** every feature depends on for the pure-Kotlin seam
> (`Navigator`, `NavEvent`, `Screen`). It declares `androidx.navigation` as
> `implementation` (never `api`) so `NavController`/`NavHost` stay confined to `:navigation`
> + `:app`; a feature can **not** import `androidx.navigation.*`. Only `:app` knows the graph.

---

## 3. Conventions (extracted from the reference)

### Feature entry (`ui/XxxRoute.kt`)
- Each feature exposes **one stateless entry composable** `XxxRoute(...)`. Features are **not**
  Activities and never create a `NavController`.
- Obtain the ViewModel with `hiltViewModel()`; read state with
  `viewModel.stateFlow().collectAsStateWithLifecycle()`.
- Handle **feature-local** one-shot effects (chime, toast — **not** navigation) with
  `LifecycleAwareLaunchedEffect(viewModel.sideFlow()) { when (it) { ... } }`.
- Own back press here, dispatching to the ViewModel — never navigate from the UI:
  ```kotlin
  BackHandler { viewModel.handleXxxIntent(XxxIntent.HandleBackPress) }
  ```
- Render screens **from state**; an intra-feature screen change is a `sealed` step in the
  state (`when (state.screen) { Step.A -> AScreen(...) ; Step.B -> BScreen(...) }`), not a
  navigation call.

### App host (`app/MainActivity.kt`)
- The **only** Activity. `@AndroidEntryPoint`; inject the `Navigator`.
- Owns the single `NavHost` and registers each feature route:
  ```kotlin
  ObserveNavigation(navigator = navigator, navController = navController, onExit = { finish() })
  NavHost(navController, startDestination = HomeDestination) {
      composable<HomeDestination> { HomeRoute(...) }
      composable<SettingsDestination> { SettingsRoute() }
  }
  ```
- This is the **single place** that knows the navigation graph and the `NavController`.

### State (`XxxState.kt`)
- Root state is `@[Stable Keep] data class` with a `companion object { fun getDefault() }`.
- Split large state into **nested `@Stable` data classes**, each with its own `getDefault()`.
- Model transient statuses as `sealed class` (e.g. `VoiceRecordingStatus`), using `object`
  for stateless cases and `data class` for parameterised ones.
- Use `emptyString()` for default strings; keep every field immutable (`val`).

### ViewModel (`XxxViewModel.kt`)
- `@HiltViewModel`, extend `BaseViewModel<XxxState, XxxSideEffect>(savedStateHandle)`.
- Constructor-inject UseCases, Delegates, `Gson`, `AnalyticsManager`.
- Mix in Delegates with `by`:
  ```kotlin
  class XxxViewModel @Inject constructor(...) :
      BaseViewModel<XxxState, XxxSideEffect>(savedStateHandle),
      VoiceRecorderDelegate by voiceRecorderDelegateImpl,
      AudioMediaPlayerDelegate by audioMediaPlayerDelegateImpl {
  ```
- Read navigation args (the constructor properties of the typed `@Serializable`
  `XxxDestination`, surfaced via `SavedStateHandle` under their property names) with
  `argumentNullable(KEY)` — never from an Activity `Intent` extra:
  ```kotlin
  private val chatRoomId: String? by argumentNullable(CHATROOM_ID) // = the property name on the destination
  ```
- Override `initialState()` → `XxxState.getDefault()`, and do startup work in `initData()`
  (register delegate flow collectors, fetch details).
- **All state mutation happens inside `intent { reduce { state.copy(...) } }`.** Never
  mutate state anywhere else.
- **Toasts / chimes / other feature-local one-shot effects go through `postSideEffect(...)`.**
- **Navigation is NOT a side effect.** Inject `Navigator` and call it from intent handlers:
  `navigator.navigate(XxxDestination)`, `navigator.pop()`, `navigator.exit()`.
- Expose a single intent entry point with an exhaustive `when`:
  ```kotlin
  fun handleXxxIntent(intent: XxxIntent) = intent {
      when (intent) {
          is XxxIntent.HandleClickEvent -> onClickCta(intent.ctaType)
          ...
      }
  }
  ```
- UseCase calls return `ApiResult`; branch on `Success`/`Error`:
  ```kotlin
  when (val result = createXxxUseCase(request)) {
      is ApiResult.Success -> postSideEffect(XxxSideEffect.CreatedSuccessfully(result.data))
      is ApiResult.Error   -> result.exception?.handleError()
  }
  ```
- Centralise error handling in one `Throwable?.handleError()` that distinguishes
  `NoNetworkException`, parses `HttpException` 4xx bodies with `gson`, and falls back to a
  generic "something went wrong" side effect.
- Collect delegate `MutableSharedFlow`s in `intent { ... .collect { reduce { ... } } }`.
- Clean up (`timerManager.stopTimer()`, etc.) in `onCleared()`.

### Intents & Side Effects
- `XxxIntent` is a **`sealed interface`** of user actions (`object` / `data class`).
- `XxxSideEffect` is a **`sealed class`** of **feature-local** one-shot effects only — show
  toast, play chime, success-with-payload. **Navigation and app-exit are NOT side effects**;
  they go through the injected `Navigator` (`navigate` / `pop` / `exit`).
  ```kotlin
  sealed interface XxxIntent {
      data class HandleClickEvent(@XxxCtaType val ctaType: String) : XxxIntent
      object HandleBackPress : XxxIntent
  }
  sealed class XxxSideEffect {
      data class ShowToast(val message: String) : XxxSideEffect()
      data class CreatedSuccessfully(val response: ...) : XxxSideEffect()
  }
  ```

### Navigation (the `Navigator` seam)
- Navigation is a **seam**, not a per-feature `NavGraph`/`NavigationAction`. There are **no**
  `XxxNavGraph.kt`, `XxxNavigationAction.kt`, `XxxNavigationRoutes.kt`, or `XxxRoutesPath.kt`
  files — those are the retired pattern.
- The feature layer depends **only** on the pure-Kotlin `:navigation` module. A feature must
  never touch `NavController`/`NavHost`.
- Each feature owns its destinations in a tiny **`api` submodule** (`:feature:xxx:api`),
  written in the Navigation 2.8 **type-safe routes DSL**: each destination is a
  `@Serializable` type implementing the `Screen` marker from `:navigation`, and the type
  itself is the route — no route strings, no shared catalog file. Args are constructor
  properties, compile-time-checked. A feature that navigates to another depends only on
  that feature's `api` module — **never** on its impl:
  ```kotlin
  // in :feature:settings:api
  @Serializable data object SettingsDestination : Screen
  // in :feature:detail:api — a destination with args
  @Serializable data class DetailDestination(val id: String) : Screen
  ```
- The ViewModel injects `Navigator` and navigates from intent handlers:
  ```kotlin
  private fun openSettings() = intent {
      reduce { state.copy(showReminderBanner = false) } // do work first if needed
      navigator.navigate(SettingsDestination)            // then navigate
  }
  ```
- `NavigatorImpl` is `@Singleton` and carries **pure data only** through a buffered `Channel`
  — never a `Context`, Activity, `NavController`, or callback. This keeps it leak-safe and
  KMP-portable regardless of scope.
- `ObserveNavigation` is the **single** collector: it reads events inside
  `repeatOnLifecycle(STARTED)` and applies them to the `NavController`. Adding nav logging /
  analytics / global redirects (e.g. auth gate → `navigate(Login)`) happens here, once.

### Launching Activities (leak-safe)
- Even this single-Activity app must launch **external / system / third-party** Activities:
  URLs, share sheets, camera, document/photo pickers, sign-in, payments.
- **Fire-and-forget:** emit a typed destination (e.g. `ExternalUrlDestination(url)`) or a
  `NavEvent.LaunchIntent(intent)`; the **host** builds the `Intent` with its own `Context` and
  calls `startActivity`. The `Context` is used transiently and never stored.
- **Result-returning:** register `rememberLauncherForActivityResult(...)` inside the
  `XxxRoute` composable (lifecycle-aware, auto-unregistered) and feed the result back to the
  ViewModel as an ordinary typed intent (`handleXxxIntent(XxxIntent.DocumentPicked(uri))`).
- **Never** put a `Context`, Activity, `NavController`, launcher, or result callback into the
  `@Singleton` Navigator, a `NavEvent`, or a ViewModel.

### UI (`ui/`)
- Composables are **stateless**: they receive `state` and lambdas
  (`onClickCta`, `setAge`, `onBackPress`, ...) — nothing else.
- They never reference the ViewModel directly; all events flow up via lambdas that the
  `XxxRoute` entry composable maps to `handleXxxIntent(...)`.
- Identify screens/tabs with `@StringDef` annotations (e.g. `OnboardingScreen`,
  `FriendZoneOnboardingTab`) and put index↔tab mapping / counts in their `companion object`.

### Utils / Delegates (`utils/`)
- Reusable capabilities are `interface` + `@Inject` `Impl` (e.g. `VoiceRecorderDelegate`),
  exposing a `MutableSharedFlow<Status>` that the ViewModel collects and reduces.
- Inject `@ApplicationContext` and `DispatcherProvider`; run blocking work on
  `dispatcherProvider.IO` — never block the main thread.
- Pure helpers live in an `object` (e.g. `OnboardingUtils`) as extension functions.

### Repositories (per-domain, never a god interface)
- Repositories are split **per domain** (preferences, stats, mood, presets, ...), one small
  interface per domain — never one shared interface that every feature injects.
- When writing a NEW repository (or adding to one), put it in its domain's folder:
  - interface → `model/src/main/java/com/reset/model/domain/<domain>/XxxRepository.kt`
    (package `com.reset.model.domain.<domain>`)
  - impl → `repository/src/main/java/com/reset/repository/data/<domain>/XxxRepositoryImpl.kt`
    (package `com.reset.repository.data.<domain>`), holding only that domain's DataStore keys
    and only the dependencies it needs; bind it in `HomeModule` with `@Binds`
  - impl test → `repository/src/test/java/com/reset/repository/data/<domain>/`
- If a method doesn't fit an existing domain, create a new domain folder — do NOT grow a
  neighboring interface.
- ViewModels inject only the repository interfaces they actually use; each feature's test
  fakes implement only those interfaces (e.g. `FakeMoodRepository`), in the feature's own
  test package.

### Constants & Analytics
- Every analytics page/action/event string, status string, and dimen lives in
  `XxxConstants` — **no magic strings** in ViewModel or UI.
- Wrap analytics in a single private `trackXxx(action, page)` over `AnalyticsManager`,
  called from intent handlers.

---

## 4. New feature checklist (in order)

1. `XxxState.kt` — root `@[Stable Keep]` state + nested `@Stable` states + sealed statuses + a sealed **step** for intra-feature screens, each with `getDefault()`.
2. `navigation/XxxIntent.kt` — sealed interface of user intents.
3. `navigation/XxxSideEffect.kt` — sealed class of **feature-local** one-shot effects only (navigation is NOT a side effect).
4. Create `:feature:xxx:api` with the feature's `@Serializable XxxDestination : Screen` (args as constructor properties); other features depend on this api module to navigate here.
5. `XxxViewModel.kt` — `BaseViewModel`, inject UseCases/Delegates **+ `Navigator`**, `handleXxxIntent`, `reduce`, `postSideEffect`, `navigator.navigate/pop/exit`, `handleError`.
6. `ui/XxxRoute.kt` — `hiltViewModel()`, `collectAsStateWithLifecycle`, `LifecycleAwareLaunchedEffect(sideFlow())`, `BackHandler`; render screens from `state.screen`.
7. `ui/` — stateless composables + `@StringDef` screen/tab markers.
8. `XxxConstants.kt` — all strings/dimens.
9. Register the route in `app/MainActivity` NavHost: `composable<XxxDestination> { XxxRoute(...) }`.
10. `utils/` — delegates (interface + `@Inject` impl) and pure util objects as needed.
11. all kinds of api calls will be done in the data module
12. all kinds of local data models will be created in the domain module
13. new repositories go in their domain folder on both sides — interface in
    `model/.../domain/<domain>/`, impl in `repository/.../data/<domain>/` (see
    "Repositories (per-domain, never a god interface)")

---

## 5. DON'Ts

- ❌ No business or back-navigation logic in composables — it lives in the ViewModel.
- ❌ No magic strings — put them in `XxxConstants` or a `@StringDef`.
- ❌ No state mutation outside `reduce { }`.
- ❌ No per-feature Activity, `NavHost`, `NavController`, `NavGraph`, or `NavigationAction` —
  features are `XxxRoute` composables in the app's single `NavHost`.
- ❌ No `androidx.navigation.*` imports in a feature module — depend only on the `:navigation`
  seam (`Navigator`, `NavEvent`, `Screen`) and on other features' `api` modules for their
  destinations. Never depend on another feature's impl module.
- ❌ No navigation from the UI or via `postSideEffect(...)` — navigate through the injected
  `Navigator` (`navigate` / `pop` / `exit`) from ViewModel intent handlers.
- ❌ No `Context`, Activity, `NavController`, launcher, or callback inside the `@Singleton`
  Navigator, a `NavEvent`, or a ViewModel — payloads are pure data; only the host builds
  `Intent`s and calls `startActivity`.
- ❌ No direct ViewModel access from leaf composables — pass lambdas down.
- ❌ No blocking / IO work off `DispatcherProvider`.

---

## 6. Sense — the ML microbreak engine

The `sense-*` modules (`:sense-ml`, `:sense-signals`, `:sense-store`,
`:sense-delivery`) and the `ml/` Python pipeline form the on-device ML system that
times microbreak notifications. They all live under the `library/` folder
(`library/sense-*/`, `library/ml/`); the Gradle module names stay flat (`:sense-ml`),
mapped via `projectDir` in `settings.gradle.kts`. **Before modifying ANY of them, read
[`docs/SENSE_ML.md`](docs/SENSE_ML.md)** — it documents the architecture, the
34-feature schema, the cross-language training↔inference contract, the label rules,
and step-by-step modification checklists (changing a feature REQUIRES a
`SCHEMA_VERSION` bump in both Python and Kotlin plus a retrain). These modules are
kept SDK-extractable: plain constructors, Hilt only at entry edges, no imports from
`app/` or feature modules, and zero Android types inside `:sense-ml`.

---

