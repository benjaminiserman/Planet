# Hersfeldt climate tuning

The headless tuner runs the real Planet climate simulation inside Godot, compares
the resulting land classifications with `earth_hersfeldt_reference.png`, and uses
a bounded coordinate search to improve selected values from
`ClimateSimulationGlobals`/`climate_config.json`.

## Quick start

The Earth inputs are intentionally not included in the repository yet. Before
running the tuner, provide both of these local files:

- `save/earth.json`: an imported Earth planet save; and
- `earth_hersfeldt_reference.png`: the aligned Hersfeldt reference image.

Those locations are the command defaults and are ignored by Git. Files stored
elsewhere can be selected explicitly with `--planet FILE` and
`--reference FILE`.

From the `planet` directory:

```powershell
.\tune_climate.ps1 --max-evaluations 9
```

After `classes copyJars` has already completed, `--skip-build` runs the existing
compiled tuner offline without invoking Gradle again.
`--runtime-project PATH` can point Godot at an isolated copy of the compiled
scene/JVM metadata, which is useful when the editor project is waiting for a
debugger or otherwise must remain untouched.
`--godot-bin FILE` selects a specific Godot executable for that runtime.

The default run:

- reloads the imported Earth fixture at `save/earth.json` before every candidate,
  so state from one simulation cannot leak into the next;
- takes parameter bounds and step sizes from `climate_tuning.json`;
- writes the current winner to `build/climate-tuning/best-climate_config.json`;
- writes every score, diagnostic, and failure to `build/climate-tuning/report.json`;
- renders baseline and winning simulated/difference maps under
  `build/climate-tuning/artifacts`; and
- leaves `climate_config.json` unchanged.

Add `--apply` to update `climate_config.json` only when the winner beats the
starting score. The original is copied to
`build/climate-tuning/original-climate_config.json` the first time this happens.

```powershell
.\tune_climate.ps1 --max-evaluations 17 --apply
```

Continue from the prior winner with `--resume`:

```powershell
.\tune_climate.ps1 --resume --max-evaluations 17 --apply
```

For a focused batch, choose a comma-separated subset. A baseline plus both
directions for four parameters needs nine evaluations:

```powershell
.\tune_climate.ps1 `
  --parameters baseTemperature,baseTemperatureInsolationScalar,moistureToMm,landPrecipitationScalar `
  --max-evaluations 9
```

To test a small number of hypotheses that require two parameters to move
together, reserve four trials per named pair:

```powershell
.\tune_climate.ps1 `
  --parameters baseTemperature,moistureToMm `
  --interactions baseTemperature+moistureToMm `
  --max-evaluations 9
```

The coordinate search runs first. Each interaction then tests the four paired
`+/-` combinations using the configured steps. Interaction trials are useful
when neither individual move improves the score, but the combination does.

Set `GODOT_BIN` if the matching Godot Kotlin/JVM editor is neither on `PATH` nor
already running:

```powershell
$env:GODOT_BIN = "C:\path\to\godot.windows.editor.x86_64.jvm.exe"
```

## Scoring

Only simulated land tiles with a painted reference classification are scored.
White and transparent reference pixels are an unscored ocean/background mask;
they never contribute to climate loss, confusion counts, or diagnostic maps.
Land tiles that hit this white mask are reported separately as
`referenceMisses` and `referenceCoveragePercent`, which makes import/reference
alignment problems visible without rewarding climate changes for fixing them.

The reference class for a tile is the majority painted class within half of the
coarse tile footprint, capped at 16 reference pixels. This sampling depends only
on tile geometry and the reference image, so changing the simulated class cannot
change its own target.

The minimized loss is:

```text
mismatch rate + 0.25 * normalized mean Hersfeldt condition distance
```

Zero is a perfect result. Classification mismatch is the dominant term, while
the secondary term is the shortest-path distance through a classification graph.
Graph edges represent one Hersfeldt decision-boundary change (for example,
desert to semidesert, Mediterranean to dry savanna, temperate to boreal, or a
monsoon/pluvial variant). Distances are capped at five conditions for scoring.

For Mediterranean-focused batches, pass `--objective mediterranean-f1`. This
maximizes precision/recall F1 across every Hersfeldt classification whose name
contains `mediterranean`. Candidates whose global mean condition distance rises
above the batch baseline are rejected by default; use
`--max-mean-distance-regression` only to explicitly allow a tolerance.

Difference maps use green for an exact match, lime for one condition away,
yellow for two, orange for three, red for four, and purple for five or more.
Magenta remains reserved for a reference-land/simulated-ocean mask mismatch.

## Reading a report

Every successful evaluation includes:

- a reference-to-simulated confusion matrix with graph hop counts, a complete
  condition-distance histogram, and class-frequency differences;
- exact-match and condition-distance scores by 10-degree latitude and elevation band,
  plus named Mediterranean, Andes, and northern Myanmar priority regions;
- the largest class confusions, with averaged temperature, precipitation,
  evapotranspiration, aridity, growing-degree, and season-type classifier inputs;
- a delta from the baseline showing corrected tiles, regressed tiles, changed
  simulated classes, and which reference classes gained or lost matches; and
- coverage and mask-miss fields kept separate from the climate objective.

`artifacts/baseline-simulated.png` and `artifacts/best-simulated.png` show the
simulated Hersfeldt classes on the reference canvas. Their corresponding
`*-difference.png` images use green for exact classifications, then lime,
yellow, orange, red, and purple for one through five-or-more condition hops.
White remains the unscored reference mask; magenta indicates a painted-reference
pixel whose nearest imported tile is not land.

These diagnostics are intended for hypothesis selection: first find the largest
confusion or weakest geographic band, inspect its averaged classifier inputs,
then choose a focused temperature, moisture, or circulation parameter batch.

## Tuning safely

`climate_tuning.json` intentionally starts with high-impact temperature and
moisture values and conservative physical bounds. Add another numeric
`ClimateSimulationGlobals` property there only after choosing a defensible range
and step. Very sensitive values such as `moisturePropagationMultiplier` should
use small steps.

Each candidate reloads the 72 MB Earth save and simulates all twelve months, so a
large evaluation budget can take substantial time. Prefer several focused,
resumable batches and inspect `report.json` between them.
