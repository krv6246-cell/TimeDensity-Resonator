# Core implementation

The first implementation is Kotlin and platform-neutral by design.

## Public surface

- `ResonanceEngine`: state machine and deterministic update loop.
- `ResonanceConfig`: tolerance and hold-duration settings.
- `ResonanceSnapshot`: immutable state returned to a host application.
- `ResonancePhase`: IDLE, TUNING, MATCHED, STABLE, TRANSFORMED.

The host application owns UI, audio, content, scoring and progression. The core only evaluates the relationship between target and carrier frequencies.

## Next boundary

The Android/Compose adapter and the REZONATOR visual field will be added outside this core package.
