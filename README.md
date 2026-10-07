# ATOM HUNTER
**Powered by Time Density**

ATOM HUNTER is an experimental game mode built on top of the **REZONATOR** technical core. It transforms frequency-tuning mechanics into a challenging visual and auditory hunt for atomic elements.

## Project layout

- **Android app (repository root):** Jetpack Compose game in `app/` and the reusable Kotlin resonance engine in `core/`.
- **Browser game (`web/`):** standalone React + Vite implementation. It does not use the Android Gradle build or any hosted app-builder service.

Keep platform-specific source and dependencies in their respective folders. The Android and browser versions share the game concept and element seed set, but have separate platform implementations.

## Architecture

- **REZONATOR**: The underlying technical core and engine processing frequency tolerance and matching mathematics. It relies on the external `:core` module (`ResonanceEngine`).
- **ATOM HUNTER**: The game mode UI and gameplay loop implemented using Jetpack Compose and MVVM (`GameEngine`, `GameUI`).
- **ResonatorGameAdapter**: The translation layer mapping game rules (hysteresis, magnetic snap, extended hold durations) to the strict `ResonanceEngine` without modifying the core directly.

## Gameplay Mechanics

The player tunes a Carrier frequency using the TUNE slider to hunt for elements. The resonance lifecycle follows three strict phases:

1. **MATCHED**: The Carrier frequency enters the accepted entry tolerance zone (±30 Hz) of the Target element's frequency.
2. **STABLE**: The player holds the frequency within the exit tolerance zone (±50 Hz) for a specific duration (800 ms). Hysteresis and grace periods prevent micro-movements from failing the capture instantly.
3. **TRANSFORMED**: The atom is successfully captured.

Up to four atoms remain visible at once. The player chooses one target from the field before tuning; each target still follows the `MATCHED → STABLE → TRANSFORMED` resonance lifecycle. Captured atoms spiral into the central **ATOM VAULT**, and their count is added to the collection.

## Features

- **TUNE Slider**: A keyboard/screen-reader-accessible frequency control with magnetic snap feedback (`±12 Hz`).
- **Gravity field**: A low-cost animated lensing grid reacts to live atoms and stable resonance, and becomes static when Android animations are disabled.
- **WaveformDisplay**: A live oscilloscope plots the target and carrier waves in the cyan/violet palette.
- **ATOM VAULT**: Tracks captures of six distinct elements while keeping their original frequencies.
- **Generated composition**: Each capture stores its element, exact frequency, order, elapsed time, precision, hold duration, and transformed phase. A deterministic 120 BPM arrangement quantizes the capture rhythm, uses each real element frequency as its lead with an octave bass, and layers procedural industrial percussion without external samples.
- **Finale galaxy**: A seeded layout built from the same capture events highlights the corresponding atom during playback and supports replay/reduced-motion behavior.
- **Save and navigation**: Save the session pattern as JSON through Android's document picker; playback has stop and volume controls, and Home/Exit actions are available with back-button handling.

### Target Elements
| Element | Symbol | Frequency (Hz) |
|---|---|---|
| Hydrogen | H | 659 |
| Helium | He | 392 |
| Carbon | C | 293 |
| Nitrogen | N | 261 |
| Oxygen | O | 220 |
| Iron | Fe | 110 |

## Current Limitations & Future Plans
- Android and browser gameplay/session formats are not yet shared; the event, composition, and seeded-layout models are platform-neutral Kotlin to support a future port.
- Only the 6 baseline elements are implemented.

## Running the Project

### Android

```
bash gradlew app:assembleDebug
bash gradlew core:test
```

### Browser game

Run these commands from `web/`:

```
npm install
npm run dev
```

Create a production build with `npm run build` and run the browser-game checks with `npm run lint` and `npm run typecheck`.