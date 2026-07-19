# Ecology migration plan

## Objectives

Migration should move biomass without making results depend on tile iteration order, preserve habitat barriers, remain deterministic for a planet seed, and support three distinct biological patterns:

1. continuous short-distance dispersal to neighboring tiles;
2. periodic migration between seasonal ranges, including long-distance routes;
3. natal homing, where a cohort returns to its own spawning or breeding location.

The current `TileEcosystem` stores one biomass value per species. That is sufficient for ordinary dispersal, but periodic migration and natal homing eventually require cohorts because members of the same species can have different origins and destinations.

## Phase 1: neighboring-tile dispersal

Run dispersal once per season after every tile has completed its local population step.

1. Take an immutable snapshot of all tile biomass.
2. For each motile species, calculate a pressure to leave from crowding, food shortage, climate stress, and recent population decline.
3. Score valid neighboring tiles using climate suitability, expected food, competition, predators, terrain slope, and terrestrial/aquatic compatibility.
4. Emit transfer intents containing source tile, destination tile, species, and biomass. Cap emigration to a configured fraction of the source population.
5. Apply all outgoing and incoming transfers simultaneously, then apply the individual-count extinction threshold.

The two-phase intent buffer is essential: directly changing a neighbor while traversing tiles would make the result depend on map iteration order. Transfer totals should be checked so that migration conserves biomass except for an explicit travel mortality cost.

Suggested initial configuration:

- maximum seasonal dispersal fraction;
- minimum departure pressure;
- climate, food, competition, and predation score weights;
- distance or terrain travel mortality;
- establishment threshold and a small founder-group floor.

Use a deterministic random stream derived from `(planet seed, history turn, source tile id, species id, migration phase)` instead of the shared mutable planet RNG.

## Phase 2: periodic migration

Add a declarative `MigrationEffect` to traits rather than checking named traits in the runtime. It should describe departure seasons, maximum range, navigation precision, travel cost, and the ecological signal used to choose destinations. Concrete traits can then express behaviors such as seasonal grazing migration, altitudinal migration, or pelagic migration with their own trade-offs.

Short periodic routes can evaluate tiles inside a bounded graph radius. Long routes should use cached paths over a habitat-specific movement graph. Route endpoints should be re-evaluated when climate or terrain changes, while paths can be reused between changes. Stopover tiles can impose temporary capacity and food constraints.

At this phase, replace a single biomass value with a compact collection of `PopulationCohort` values only for migratory species. A cohort needs at least biomass, current tile, destination tile, route progress, and migration phase. Non-migratory species should retain the cheaper scalar representation.

## Phase 3: natal homing and salmon-like life cycles

Natal homing needs persistent origin identity. A salmon-like cohort should contain:

- natal spawning tile or watershed id;
- life stage and age;
- current habitat phase (freshwater juvenile, marine feeding, returning adult);
- current route and route progress;
- spawning season and remaining reproductive biomass.

Juveniles imprint their natal location, disperse downstream to an ocean feeding range, mature, then pathfind back to that same watershed during the configured spawning season. Reproduction creates new cohorts with the spawning tile as their natal origin. Post-spawning mortality should be a species-derived life-history parameter rather than a special case for the species name.

If terrain changes invalidate a natal site, use an explicit policy: fail the run, lose the cohort, or redirect it to the nearest compatible watershed. The default should be loss or reduced-success straying so that damming, uplift, and drainage capture have ecological consequences.

## Data and API changes

- Introduce `MigrationProfile` as compiled species data derived from trait effects.
- Add stable `PopulationCohortId`, origin ids, and route state to serialized ecosystem state.
- Add a `MigrationRuntime.plan(snapshot)` and `MigrationRuntime.apply(intents)` boundary.
- Expose habitat-specific movement graphs from planet topology instead of embedding terrain rules in ecology.
- Add watershed and ocean-basin ids before implementing anadromous life cycles.
- Record incoming, outgoing, and travel-loss biomass in history stats and tile inspection UI.

## Performance strategy

Neighbor dispersal is linear in active species populations times average tile degree. Periodic route searches are potentially much more expensive, so cache habitat graphs and endpoint-to-endpoint paths, schedule cohorts only in their departure seasons, and keep route state sparse. Transfers for different source tiles can be planned in parallel once inputs are immutable; merge intents deterministically by destination tile and species before applying them.

## Verification

Add automated tests for biomass conservation, iteration-order independence, barrier enforcement, deterministic replay, emigration caps, bidirectional seasonal routes, route interruption, and save/load continuation. Natal-homing tests should prove that two cohorts of the same species return to different origins and that offspring inherit the actual spawning location.

