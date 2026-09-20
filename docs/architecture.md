# Architecture

## Separation of responsibility

```text
Host application
├── GameField / visual scene
├── ResonanceHUD
├── TuneControl
├── AudioEngine
└── progression and content
        │
        ▼
TimeDensity-Resonator
├── target selection
├── carrier tuning
├── precision calculation
├── hold/stability detection
└── resonance events
```

The host provides input and content. The module returns deterministic state and events.

## First implementation principles

1. Keep the core deterministic and testable.
2. Keep UI adapters outside the core.
3. Keep audio rendering outside the core.
4. Make the frequency model replaceable.
5. Version the public API before connecting multiple applications.
6. Prefer small interfaces over a monolithic game engine.

## Planned adapters

- Kotlin/JVM or Android adapter for the native REZONATOR build;
- TypeScript/Web adapter for browser and PWA experiments;
- optional audio adapter for Web Audio or Android audio output.
