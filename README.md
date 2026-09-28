# ATOM HUNTER
**Powered by Time Density**

ATOM HUNTER is an experimental game mode built on top of the **REZONATOR** technical core. It transforms frequency-tuning mechanics into a challenging visual and auditory hunt for atomic elements.

## Architecture

- **REZONATOR**: The underlying technical core and engine processing frequency tolerance and matching mathematics. It relies on the external `:core` module (`ResonanceEngine`).
- **ATOM HUNTER**: The game mode UI and gameplay loop implemented using Jetpack Compose and MVVM (`GameEngine`, `GameUI`).
- **ResonatorGameAdapter**: The translation layer mapping game rules (hysteresis, magnetic snap, extended hold durations) to the strict `ResonanceEngine` without modifying the core directly.

## Gameplay Mechanics

The player tunes a Carrier frequency using the TUNE slider to hunt for elements. The resonance lifecycle follows three strict phases:

1. **MATCHED**: The Carrier frequency enters the accepted entry tolerance zone (±30 Hz) of the Target element's frequency.
2. **STABLE**: The player holds the frequency within the exit tolerance zone (±50 Hz) for a specific duration (800 ms). Hysteresis and grace periods prevent micro-movements from failing the capture instantly.
3. **TRANSFORMED**: The atom is successfully captured.

Once captured, the atom is pulled into the central **ATOM VAULT** (a pulsing cosmic black hole) through a spiral animation accompanied by a golden flash, and its count is added to the player's collection.

## Features

- **TUNE Slider**: A precise gradient slider with magnetic snap feedback (`±12 Hz`) assisting the player in locking onto frequencies.
- **WaveformDisplay**: A live visual oscilloscope plotting the `Target` (Neon Violet) and `Carrier` (Electric Cyan) waves, reacting with a golden glow when resonance is stable.
- **ATOM VAULT**: Visual collection tracking the player's catch of six distinct elements.

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
- The ATOM VAULT currently only tracks the element counts. Future updates will introduce mechanics to forge captured matter into a new universe and generate a musical composition out of the caught frequencies.
- Only the 6 baseline elements are implemented.

## Running the Project
```
./gradlew app:assembleDebug
./gradlew core:test
```