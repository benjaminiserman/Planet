# Ecology production refactor report

## Current production boundary

The numerical model, traits and effects, catalog, taxonomy, suitability checks, stochastic solver, tile state, configuration, and planet runtime now live under `dev.biserman.planet.planet.ecology`. The notebook imports that package and retains only example climates, regression probes, plots, and exploratory runs. Every tile owns serialized ecosystem state; history turns advance it one season; biota distributions expose a taxonomic order; and the UI can randomize, clear, inspect, graph, and render ecosystem state.

This is a workable first production boundary. The following refactors are recommended before ecology becomes a large, frequently simulated game system.

## Highest-priority refactors

### Make simulation input and output explicitly immutable

`EcosystemModel`, climate input, configuration, and starting state should be passed to a pure seasonal step that returns a new state plus diagnostics. Keep planet lookup, mutation, and UI refresh in `EcologyRuntime`. This makes the solver independently testable and allows safe parallel execution.

### Replace shared random state with deterministic scoped streams

Using `Planet.random` means adding a tile or changing iteration order can change every later ecological result. Derive random seeds from the planet seed, turn, tile id, species id, and process name. Randomizing ecosystems, local process noise, disturbances, and migration should use separate streams so one feature cannot perturb another.

### Cache compiled food-web structure

Building `EcosystemModel` currently derives guilds and consumer overlap every season for every occupied tile. Split it into:

- a climate- and area-independent compiled community keyed by a canonical sorted species-id set;
- lightweight per-tile inputs containing area, altitude, climate, and biomass.

Cache compiled communities and invalidate only when membership changes. For 10,000 tiles this is likely the single most valuable near-term CPU refactor.

### Establish save-schema compatibility

Species ids and taxonomic-order names are serialized identifiers and must be treated as stable API. Add an ecology schema version to saved planets, explicit migrations for renamed or removed species, and a policy for unknown ids. Prefer retaining unknown biomass in a diagnostic bucket over silently discarding it. Add round-trip tests using an older save fixture.

### Make configuration an immutable validated value

The mutable singleton matches the existing project configuration style but makes tests and parallel simulations harder. Deserialize `ecology_config.json` into an immutable `EcologySettings`, validate it once, and attach the settings snapshot or revision to a run. The current `validate()` function is an interim guard.

## Modeling boundaries

### Separate habitat state from climate samples

Precipitation is only a proxy for wetland availability, snow cover, soil water, and tunnel flooding. Introduce a per-tile seasonal habitat sample derived from climate, hydrology, vegetation structure, and substrate. Traits should consume named habitat axes; the solver should not calculate planet hydrology itself.

### Generalize population state before age structure is needed

Scalar biomass is efficient for resident populations. Migration, reproduction seasonality, juvenile survival, and natal homing need cohort state. Use a hybrid representation: keep scalar populations by default and allocate cohort records only for species whose compiled life-history effects require them.

### Clarify establishment versus persistence

The current randomizer uses strict all-month suitability and falls back to the habitat pool when no species passes. Production assembly should distinguish establishment suitability, seasonal stress survivability, and long-term persistence. Return scored suitability rather than only a Boolean so procedural generation can favor good matches without making boundaries absolute.

### Move catalog assertions to tests

Catalog construction should fail for structurally invalid blueprints, but scenario claims such as specific predator preferences, biome survival, and competitive outcomes belong in automated tests and benchmark fixtures. Keep the notebook for visual exploration, not as the only regression suite.

## Solver and performance

Measure the seasonal step against a target workload of 10,000 occupied tiles at the configured species limit. Track model-build time separately from derivative evaluation. Likely improvements are:

- structure-of-arrays biomass and pre-indexed diet pathways instead of string-keyed maps inside RK4;
- cached community matrices and species-id integer indices;
- sparse execution for empty or dormant populations;
- batched or parallel tile stepping with deterministic random streams;
- an adaptive or positivity-preserving seasonal integrator if RK4 substeps remain the bottleneck;
- diagnostics that can be disabled outside debug builds.

Retain a high-accuracy reference solver for regression comparisons even if production uses a faster integrator. A benchmark should set acceptable error for final biomass, extinctions, and annual variability rather than require bitwise agreement between solvers.

## Catalog and procedural generation

Replace fixed global lists with a catalog registry that validates unique ids, taxonomic membership, trait compatibility, and resource closure. Procedural species should receive stable generated ids and retain their authored trait set in save data; derived parameters can then be regenerated when formulas change. Version the trait compiler so older saves can either preserve old compiled values or deliberately migrate to new behavior.

An order is a broad distribution pool, not a guarantee that every member exists everywhere. Add weighted prevalence and dispersal constraints at species or clade level so globally available orders do not make polar, desert, and tropical members equally likely on every tile.

## Runtime, statistics, and UI

The generated per-species color modes are useful now but will make the display menu unwieldy as the catalog grows. Replace them with two reusable modes—species range and species density—backed by a searchable species selector. Add log-scale legend values and distinguish biomass density from individual density.

History stats should eventually be accumulated during the seasonal pass instead of rescanning all tiles for every stat. Useful additions include primary production, trophic transfer, starvation loss, climate-stress loss, extinctions, colonizations, and migration flux. Store aggregates rather than per-tile histories unless a debugging session explicitly requests traces.

## Suggested package shape

```text
planet.ecology.catalog     traits, blueprints, taxonomy, compiler, registry
planet.ecology.model       immutable community and population state
planet.ecology.solver      derivatives, integration, noise, diagnostics
planet.ecology.habitat     suitability and seasonal habitat inputs
planet.ecology.migration   movement profiles, intents, routes, cohorts
planet.ecology.runtime     planet adapters and seasonal orchestration
planet.ecology.ui          display-mode adapters and inspection models
```

This split should be introduced only as files acquire independent responsibilities; the current single subpackage is preferable to empty abstraction layers.

## Recommended rollout

1. Add deterministic scoped RNG, old-save fixtures, and a seasonal performance benchmark.
2. Split and cache compiled community structure; convert inner-loop ids to integer indices.
3. Introduce immutable settings and pure seasonal-step diagnostics.
4. Add scored establishment suitability and habitat samples.
5. Implement two-phase neighboring dispersal.
6. Add sparse cohort state, periodic routes, and finally natal homing.

