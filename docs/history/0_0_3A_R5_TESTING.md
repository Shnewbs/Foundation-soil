# Foundations Soil 0.0.3a.R5 Runtime Checklist

## Build

Run:

```text
clean test build
```

Expected JAR:

```text
build/libs/foundations_soil-0.0.3a.R5.jar
```

## 1. TFC hoe baseline

1. Find TFC farmland.
2. Aim at the same farmland block while holding a TFC-compatible hoe.
3. Record TFC's displayed values:
   - current Hydration;
   - Min Hydration;
   - Max Hydration;
   - Nitrogen;
   - Phosphorus;
   - Potassium.

## 2. Soil Probe live HUD parity

Switch from the hoe to the Foundations Soil Probe without moving the crosshair.

The precision Probe HUD should show:

```text
TFC Soil: <variant> Farmland   Hydration <current>% (Min <min>% Max <max>%)
(N) Nitrogen <xx.xx>%   (P) Phosphorus <xx.xx>%   (K) Potassium <xx.xx>%
```

The values should match the TFC hoe for that exact block. Foundations Soil diagnostics should continue underneath it.

## 3. Field Guide snapshot

1. Right-click the same farmland with the Soil Probe to scan it.
2. Open the Foundations Field Guide.
3. Select the scanned Soil Cell.

The TerraFirmaCraft section should contain:

- TFC soil/farmland type;
- current Hydration;
- Seasonal Range (TFC Min/Max Hydration);
- N / P / K with two-decimal percentage precision.

The Field Guide is a stored snapshot. The live Probe HUD can change with TFC conditions; re-probe the cell to refresh the stored Field Guide values.

## 4. R4 migration behavior

Open a farmland cell that was scanned before R5 but has not been re-probed.

Expected:

- existing R3/R4 Hydration and N/P/K remain readable;
- the new range is not invented;
- the page asks for a re-probe to record the TFC seasonal range;
- after re-probing, Min/Max values populate normally.

## 5. Non-farmland TFC soil

Aim the Soil Probe at a supported TFC soil block that is not farmland.

Expected:

- the TFC soil type is shown;
- Hydration/N/P/K are identified as farmland-only;
- Foundations Soil diagnostics still render normally.

## 6. Named Field page

For a named Field containing several scanned farmland cells:

- current Hydration is averaged across member snapshots;
- TFC Min/Max range is averaged only across R5 cells that actually contain range snapshots;
- N/P/K are averaged across farmland snapshots;
- older cells do not contribute fake zero ranges.

## 7. Compatibility / log checks

Expected contracts:

```text
Mod version:          0.0.3a.R5
Field Ledger schema:  3
Network registrar:    6
Soil Cell schema:     3
Soil model:           3
```

There should be no packet decode errors, Field Ledger migration exceptions, or TFC class/method linkage errors.
