# Resonance model

## Core inputs

- `targetFrequency`: frequency selected by the current entity or state;
- `carrierFrequency`: current value controlled by the host input;
- `tolerance`: permitted distance from the target;
- `holdDuration`: time required to confirm stability.

## Derived values

```text
distance = abs(targetFrequency - carrierFrequency)
precision = clamp(1 - distance / tolerance, 0, 1)
matched = distance <= tolerance
stable = matched continuously for holdDuration
```

The exact formula remains provisional until the first implementation and tests.

## State sequence

```text
IDLE → TUNING → MATCHED → STABLE → TRANSFORMED
                  ↘
                   LOST
```

A host may map these states to animation, audio, scoring, molecule creation, or another domain-specific result.

## Six-element seed set

| Element | Seed frequency | Role in REZONATOR |
|---|---:|---|
| H | 659 Hz | light carrier |
| He | 392 Hz | stabilizing tone |
| C | 293 Hz | structural tone |
| N | 261 Hz | transition tone |
| O | 220 Hz | bonding tone |
| Fe | 110 Hz | deep carrier |

These values are game/audio parameters for the first implementation, not a claim that they are the elements’ universal physical resonance frequencies.
