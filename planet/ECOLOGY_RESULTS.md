# Ecology Prototype Results

Date: 2026-07-26

This report accompanies `ECOLOGY_DESIGN.md` and the standalone
`dev.biserman.planet.planet.ecology` package. The prototype is not connected
to saves, UI, `PlanetTile`, or `BiotaDistribution`, and it does not modify
`ecology.ipynb`.

## Outcome

The prototype demonstrates that the agreed mechanics fit comfortably inside the
performance budget without exposing optimized storage to trait authors.

- Descriptive traits compile into immutable numeric species profiles.
- Every authored trait exposes a player-facing description that is stored
  independently from its compiled effects.
- Habitat and strategy support derive a local niche from a curated whitelist.
- Resources are inferred from the niche's strategy and habitat.
- Carpet plants, bugs, plankton, small aquatic life, and aeroplankton are invariant aggregate
  species using ordinary populations, niches, climate fitness, predation, and
  extinction rather than climate-regenerated filler pools.
- Terrestrial grazers now consume modeled sessile producers; the old
  climate-derived ground-producer pool has been removed.
- Deterministic two-octave climate variability adds bounded seasonal and
  multi-decade temperature and precipitation anomalies.
- Consumer edges are compiled only from strategy, size, motility, and shared
  habitat; consumers do not inspect invariant metadata or guild ids.
- Ordinary predation and exact/lineage-targeted relationships compile into a
  dense directed matrix.
- Obligate relationships can require a particular local target, allowing host
  loss to cause a genuine secondary extinction.
- Interspecific niche crowding is normalized by size-class density before it is
  applied, so raw biomass differences do not make grasses automatically exclude
  trees or small consumers automatically exclude large ones.
- Seasonal populations use active biomass, reserve energy, and dormant biomass.
- Temperature, water, hard-coded light color, canopy light, freshwater versus
  saltwater physiology, coastal, sunlit/dark water, reefs, and snow/ice affect
  the relevant mechanics.
- Captured food is usable only while climate fitness clears a simple activity
  threshold. This prevents abundant prey from allowing a profoundly
  heat-, cold-, or water-mismatched consumer to thrive.
- Neighbor dispersal and precomputed seasonal routes use two-phase transfers so
  results do not depend on tile iteration order.
- Thermal foundation traits compile to an explicit runtime `ThermalStrategy`;
  the strategy now changes temperature/activity fitness rather than serving only
  as an unused label.
- Authored light compatibility begins as a strongly typed map and compiles once
  into ordinal-indexed arrays for the seasonal loop.
- Minimum viable populations are checked after arrivals. The local one-tile
  threshold is two individuals; later regional extinction logic must reject a
  handful scattered across several tiles.

## Measured performance

The Gradle/JUnit benchmark creates:

- 20,000 cells;
- 24 established populations per cell;
- 480,000 population updates;
- 11,040,000 ordered local species-pair checks;
- alternating land and ocean communities;
- 53 species in the global compiled catalog, including all five invariant
  guild definitions.

After three warm-up turns, seven measured turns produced:

| Metric | Result |
|---|---:|
| Median seasonal turn | 110.906 ms |
| Slowest measured turn | 146.488 ms |
| Requested ceiling | a few hundred ms |

The benchmark runs the climate/niche/resource, competition, dense interaction,
reserve, dormancy, mortality, growth, and local-extinction passes. It does not
run world movement because migration frequency and route density should be
measured from real generated worlds. Movement itself uses preallocated primitive
transfer arrays.

These are development-machine measurements, not a portable guarantee. The test
checks workload shape and numerical validity but deliberately does not fail on a
wall-clock threshold.

## Behavior experiments

### Unchanged environment

A four-population producer/grazer/predator/scavenger community was advanced for
900 seasons. Variation was measured over the final 160 seasons.

| Metric | Result |
|---|---:|
| Final biomass | 241,442,711,300 |
| Tail coefficient of variation | 0.000000 |
| Surviving modeled populations | 4 |

The system converged rather than oscillating or growing without bound. This is a
useful numerical check, not evidence that the current rates reproduce a
particular real ecosystem.

### Invasion

A producer and resident grazer were allowed to settle for 300 seasons. A grazer
with water-storage tissue was then introduced into the same niche and the
community ran for another 240 seasons.

| Metric | Result |
|---|---:|
| Resident before introduction | 5,912,718,947 |
| Resident after introduction | 5,295,249,963 |
| Invader after introduction | 4,735,092,542 |
| Final total biomass | 222,088,898,984 |

The invader established and perturbed the resident without forcing deterministic
exclusion. Both partitioned the broad niche and total biomass remained bounded.

### Temporary climate shock and recovery

A producer with whole-body anhydrobiosis and a grazer with seasonal torpor were
settled for 220 seasons, subjected to 12 seasons at 68 C with almost no rain,
then returned to the original climate for 180 seasons.

| Metric | Result |
|---|---:|
| Pre-shock biomass | 199,950,429,529 |
| Post-shock biomass | 62,042,061,353 |
| Peak dormant biomass | 175,956,377,985 |
| Recovered biomass | 199,932,830,438 |
| Recovery relative to baseline | 99.9% |

Dormancy preserved biomass without allowing it to feed, and reactivation
supported recovery after conditions improved.

## Authored ecosystem outcomes

The world-ecosystem notebook now contains 23 one-tile, 1,000-year scenarios with
exact extinction contracts rather than a permissive minimum-survivor score.

- All 16 representative stable ecosystems retain every authored species and
  every invariant guild exposed by their tile.
- The added aeroplankton-skies ecosystem is the only representative tile that
  opts into aeroplankton and supports a small aerial filter feeder.
- The climate-maladaptation control loses all three maladapted species.
- The invasive-superpredator control loses the resident mesopredator and the
  invader after trophic structure collapses.
- Competitive drought loses only the shallow-rooted meadow grass.
- Island predator introduction loses the flightless ground bird, followed by
  the introduced specialist after its prey is gone.
- Host removal loses the host shrub followed by its obligate sap feeder.
- Canopy collapse loses the canopy tree and canopy-specialist browser.
- Reef warming and structural loss remove only the heat-sensitive coral.

The final calibration uses full intraspecific density dependence, 0.15
interspecific competition within the same broad niche, size-overlap
density-normalized competitor biomass, and a 20-individual minimum starting
seed for consumer populations. Local extinction uses a two-individual
per-tile threshold. The Serengeti representative tile includes
major-river access, which also exposes its modeled freshwater invariant guilds.

Regression checks additionally require spruce budworm biomass to remain below
the two modeled spruce populations together, Canada lynx biomass below snowshoe
hare biomass, and snow leopard biomass below its named pika-plus-bharal prey.
They also reject surviving species whose best season never reaches viable
climate fitness, require evolving ocean filter feeders to remain below 10% of
plankton biomass, and require each stable food web to retain its expected
trophic roles.

## Standing-biomass calibration

Population biomass is interpreted as live mass. The calibration uses deliberately
broad order-of-magnitude expectations rather than a fitted target for each
ecosystem:

- terrestrial producers should ordinarily outweigh all modeled animals by tens
  to hundreds of times, with a lower floor for heavily grazed grasslands and
  productive wetlands;
- primary consumers should outweigh predators as a group;
- predators and superpredators should remain a small fraction of producer
  biomass, without requiring a rigid tenfold step between every trophic label;
- ocean producer standing stock may be similar to or lower than consumer stock
  because phytoplankton turns over much faster than large consumers;
- a representative 40,000 square kilometre ocean tile should carry roughly
  `10^8` to `10^10` kg of live plankton, while `10^11` kg is reserved for an
  exceptionally dense or vertically extensive bloom.

`EcologyBiomass` now authors producer density as strongly typed `SizeClass`
maps and compiles those maps to primitive arrays for the seasonal loop. Large
terrestrial producers retain much more structural tissue than carpet plants,
but only a small size-dependent fraction of that standing tissue is accessible
to ordinary grazing. Aquatic producer densities use a separate scale so rapid
plankton turnover does not require forest-like standing stock. Consumers retain
the previous size-derived density scale.

Across the eleven stable land and mixed representative tiles used by the
biomass regression check, the median producer-to-animal biomass ratio is about
124:1. The most consumer-heavy cases are the Okavango wetland at about 4.6:1
and Serengeti grassland at about 7.2:1; the Amazon rainforest is about 106:1.
Primary-consumer biomass exceeds combined predator and superpredator biomass in
every stable authored food web.

The Southern Ocean settles near `2.9e10` kg of plankton and the productive
Humboldt Current near `4.7e9` kg per tile. A dedicated whale-shark/plankton
regression runs for 1,000 years and requires the huge filter feeder to remain
below 2% of its plankton stock. Size-authored filtering efficiency preserves
the agreed prey rule: medium and smaller filter feeders consume minuscule life,
while huge filter feeders can also consume tiny life.

The broad regression guardrails require at least a 3:1 producer-to-animal ratio
for each stable terrestrial tile and a median of at least 50:1 across them.
Pure-ocean plankton must settle between `1e8` and `1e11` kg per tile, and total
predatory biomass may not exceed primary-consumer biomass. These checks are
intentionally loose enough to preserve meaningful biome differences.

## Broad species catalog and randomized communities

`EarthSpeciesCatalog` contains 159 readable prototype definitions:

| Group | Definitions |
|---|---:|
| Mammals | 48 |
| Familiar extinct species | 13 |
| Birds | 20 |
| Reptiles and amphibians | 15 |
| Fish | 18 |
| Invertebrates | 25 |
| Producers and fungi | 20 |

The catalog spans domestic animals, iconic terrestrial and marine
megafauna, small vertebrates, insects and other invertebrates, reef organisms,
vascular plants, algae, fungi, and recognizable extinct forms including major
dinosaur body plans, mammoths, megalodon, ammonites, and trilobites. It is a
stress-test and authoring catalog rather than a calibrated claim about exact
adult masses.

The trait vocabulary now has 111 entries. New descriptive coverage includes
gliding, silk webs, venom, constriction, talons, echolocation,
electroreception, shells and armor, quills, ink, jet propulsion, tentacles,
bioluminescent lures, digestive specializations, nectar and seed feeding,
social hunting and colonies, brood care, waterproof plumage, digging, leaping,
tool use, suction cups, and floating aquatic fronds.

`ecology_random_communities.ipynb` calls the production
`RandomEcosystemExperiment` harness for six displayed seeds. The automated pass
ran 64 seeds, each with a random authored Hersfeldt climate, a compatible
one-tile habitat, ten structurally compatible catalog species, ordinary
invariant guilds, and 400 seasons. Seven land profiles are preserved from
`ecology.ipynb`; four additional marine profiles cover tropical reefs,
temperate shelves, polar seas, and the dark deep ocean.

An additional audit runs twelve independently assembled communities for 4,000
seasons each (1,000 years). It records every species' best climate fitness
across a sampled year and fails if an evolving species remains extant despite
never having a viable season.

- All 64 runs remained finite and non-negative, and all organic resource levels
  remained inside `[0, 1]`.
- No active direct consumer survived after all of its modeled food disappeared.
- Every consumer selected without any initial modeled food eventually became
  extinct.
- Randomly unassembled communities range from complete collapse to retention of
  nearly every selected population; this spread is expected because food-web
  completeness is deliberately not enforced.
- Stable non-collapsed tails generally had low variation; complete collapses
  correctly report an undefined/infinite coefficient of variation.
- The long audit correctly removed examples such as saguaro and reindeer lichen
  from an oceanic-temperate tile, tropical mammals and birds from a desert,
  temperate organisms from an ice cap, and warm-water fishes from polar seas.
- Random food webs can still collapse even when their individual species fit
  the climate; this is expected when ten unstructured draws omit a required
  producer, prey size, or intermediate trophic level.

The random pass found and corrected three composition problems. Plankton is no
longer inserted into wholly dark water, a generic photosynthetic surface no
longer grants both terrestrial and aquatic habitat by itself, and kelp now uses
floating fronds rather than terrestrial canopy growth. Wetland papyrus and
reeds were given explicit freshwater support after that separation exposed
their previously accidental aquatic feeding edge.

One intentional limitation remains visible. Flying species can use the aerial
compartment over ocean because nesting substrate is not modeled. Salinity is
now represented at the deliberately coarse level of saltwater-only,
freshwater-only, or broad tolerance; it does not model salinity gradients.

## Verification

The complete project test task passed:

- 95 tests;
- 0 failures;
- 0 errors.

Ecology tests cover:

- compiler foundations and trait tradeoffs;
- rooted-body/terrestrial-locomotion consistency;
- size-derived parameters;
- derived niches and competition-aware establishment;
- exact-species relationship edges;
- invariant metadata, invariant-only trait validation, and species-kind-agnostic
  filter-feeding edges;
- obligate target loss and secondary extinction;
- hard-coded stellar/pigment color matches;
- insolation-triggered seasonal coats;
- single-value water availability;
- lower and optional upper water-tolerance bounds;
- freshwater, coastal, sunlit-water, and dark-water availability;
- saltwater-only, freshwater-only, and broad aquatic tolerance;
- reserve use and starvation;
- dormancy and the immersed anhydrobiosis restriction;
- reef construction and decay;
- motile-only carrion, sessile-only detritus, living-motile waste, and
  marine-snow flux;
- decomposition, coprophagy, and waste-fertilized producer growth;
- iteration-order independence;
- finite, non-negative long runs;
- individual-count extinction;
- large-predator extinction after the last eligible modeled prey disappears;
- large-predator persistence in the notebook's four-species unchanged setup;
- neighbor radiation, fixed migration routes, and habitat-gated rescue;
- the full-scale benchmark and event experiments.
- deterministic climate-anomaly bounds and organic-pool accessibility;
- explicit terrestrial grazing edges to carpet plants and rejection of
  medium-predator edges to tiny aggregate insects, while medium aquatic
  predators can exploit tiny aquatic life without opening that shortcut to
  large marine hunters;
- exact survivor/extinction outcomes for all 23 authored ecosystem scenarios.
- complete strongly typed biomass tables, terrestrial producer structural-mass
  scaling, and order-of-magnitude plausible ocean plankton capacity;
- broad trophic-biomass guardrails across all stable authored ecosystem
  scenarios.
- 1,000-year persistence of coastal carpet-plant and plankton guilds without
  animal recycling;
- tropical-reef extinction of emperor penguins even when aquatic prey is
  initially abundant;
- 64 short randomized communities and twelve 1,000-year randomized
  climate-mismatch audits.

## What remains intentionally approximate

### Biomass and resource calibration

The runtime uses kilograms of biomass, but area productivity, maintenance,
assimilation, and invariant-guild rates are provisional. They create stable
feedback and interpretable responses; they are not fitted to Earth biomass
densities. Calibration should occur after actual Planet climate and terrain
distributions are connected.

Tiny and minuscule life now consists of locally depletable invariant aggregate
populations. Their broad resistance represents many interchangeable local
lineages, but it remains finite and does not reseed an extinct tile. Carrion,
detritus, waste, and marine snow remain stateful seasonal pools derived from
biological fluxes. Ground-level production is the modeled Carpet plants
population rather than a separate climate resource.

### Temperature distinctions

Species with the same compiled four-bound envelope behave alike even if their
real mechanisms would differ in humidity sensitivity, daily extreme tolerance,
body-part temperature, or juvenile survival. Seasonal coats add a low-insolation
adjustment, and dormant lifecycles can skip hostile seasons, but there is no
within-season thermal history.

### Water distinctions

One water value cannot separately represent drinking water, soil water, leaf
humidity, flooding, or temporary pools. It now supports lower and upper
tolerance bounds, but the upper bound still means only "this cell-season is too
wet." Dehydration remains distinct from heat because water need and hot
tolerance are separate compiled parameters. The model does not track body
water, burrow drainage, or soil oxygen.

### Light distinctions

Five star classes and seven biological colors cannot represent continuous
spectra, pigment mixtures, atmospheric filtering, or wavelength changes beneath
different canopies. The table is intentionally inspectable and easy to explain.

### Ecological structure

Niches are a curated `Habitat x Strategy` whitelist, so adding a genuinely new
role requires a strategy definition. Competition is presently strongest within
the exact established niche. Cross-niche overlap is a useful later tuning knob
if diversity proves too high after world integration.

### Planet-scale fluxes

The cell runtime emits marine-snow, carrion, detritus, and waste biomass.
It does not yet average marine snow through ocean circulation regions. Reef
cover changes are emitted as deltas; the world layer must store and clamp the
resulting cover.

## Integration requirements

### 1. Add a read-only environment adapter

Construct one `SeasonalCellEnvironment` per tile and season from existing world
data:

- `tile.area`;
- the relevant `ClimateDatumSample`;
- surface `StoneComponent.fertilityModifier`,
  `moistureCapacityMultiplier`, and `acidityModifier`;
- `riverUpstreamSegmentCounts` using an explicitly tuned major-river threshold;
- land/ocean adjacency and water depth;
- canopy and reef state;
- ocean circulation-region id.

`ClimateDatumSample.insolation` is in W/m2, while Ecology deliberately uses a
normalized `[0, 1]` value with a default optimum of `0.8`. The adapter must
normalize it using the planet/star's meaningful maximum. Passing W/m2 directly
would make every light-sensitive species wrong.

### 2. Add a world ecology owner

The owner should hold:

- one compiled global species catalog;
- one compact `TileCommunity` per cell;
- one cached seasonal environment per cell;
- movement neighbors and compiled seasonal routes;
- marine-snow circulation-region accumulators;
- global extinction records.

The turn order should be:

1. refresh seasonal environments;
2. advance every community without finalizing extinction;
3. aggregate carrion, marine snow, and reef deltas;
4. apply two-phase dispersal/migration;
5. finalize local viability;
6. record species with no active or dormant population globally.

### 3. Connect biogeography

Keep three different masks:

- the ancestor's accessible `BiotaDistribution`;
- each descendant's realized range;
- current propagule reach.

Seed the prototype ancestor into a subset of viable cells inside the accessible
mask, then let ordinary establishment and neighbor dispersal fan descendants
out. A global clade simply begins with a global accessible mask; it should not
receive automatic populations in every tile.

### 4. Expose explanations before adding more traits

For a selected population, the UI should be able to show:

- established habitat and strategy;
- temperature, water, light, and habitat fitness;
- resource support and competition;
- maintenance, stress, starvation, and predation losses;
- active, reserve, and dormant state;
- strongest helpful and harmful traits.

This will reveal whether a mechanic is player-legible before the trait catalog
becomes large.

### 5. Calibrate with generated worlds

Tune in this order:

1. normalize insolation and water availability;
2. fit producer capacity to cell area and fertility;
3. tune maintenance and reproduction by size;
4. tune invariant-guild production, climate resilience, and feeding edges;
5. tune predation/assimilation;
6. tune local minimum viable individuals;
7. observe local diversity and only then add cross-niche competition.

Retain the synthetic benchmark as a regression workload, then add a second
benchmark sampled from a serialized generated planet.

## Interactive experiments

`src/main/kotlin/dev/biserman/planet/notebooks/ecology_experiments.ipynb`
recreates the stability, invasion, and climate-shock scenarios against the real
prototype API. Its Kandy graphs show each population through time and split
active from dormant biomass during the climate shock. It uses the normal
two-individual local extinction threshold, ends a population's line when it is
removed, and prints each local extinction season. The notebook is intended for
design exploration; the JUnit versions retain a zero threshold so they can
measure asymptotic numerical behavior deterministically.

## Recommended next decision

Before production integration, review the author-facing trait list and the
whitelisted niches. Those are the lasting game-design API. Numeric rates are
cheap to tune later; changing what a trait or niche means after species and UI
content depend on it will be more expensive.
