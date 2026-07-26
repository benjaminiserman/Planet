# Ecology V2

Status: standalone prototype specification. This design deliberately does not
replace `src/main/kotlin/dev/biserman/planet/notebooks/ecology.ipynb`, alter save
data, or connect to the UI.

## Goals

- Advance roughly 20,000 cells by one season in a few hundred milliseconds.
- Keep species and trait definitions readable to a non-specialist.
- Make local climate and habitat central without simulating physiology in detail.
- Support unfamiliar combinations such as motile photosynthesizers,
  aeroplankton, aquatic reef builders, and organisms without plant/animal/fungus
  labels.
- Keep ecosystems usually self-limiting and approximately stable while allowing
  invasions and climate shifts to cause large changes.
- Leave clear seams for mutation, speciation, biogeography, and authored content.

The prototype favors player-comprehensible rules over physiological precision.
Every number should be explainable as habitat fit, strategy fit, climate stress,
food, competition, predation, or a visible trait cost.

## Three layers

### 1. Authored biology

`SpeciesDefinition` contains a size class, whether the organism is motile, and
descriptive traits such as `Baleen`, `GillPads`, `BurrowingEggs`,
`WholeBodyAnhydrobiosis`, or `ThinFur`.

Every trait has a player-facing description independent of its numerical
effects. Traits expose a short list of declarative effects. Effects may support a habitat
or strategy, change a tolerance, add a seasonal behavior, or create a targeted
relationship. Ordinary traits also carry a maintenance or opportunity cost.
Trait authors never manipulate packed arrays, numeric species indexes, or
interaction-matrix offsets.

`ROOTED_BODY` represents a stationary substrate-anchored organism and is
incompatible with a motile species. Ground-moving organisms use
`TERRESTRIAL_LOCOMOTION`; aquatic and aerial organisms use their corresponding
habitat adaptations. Burrowing is an adaptation within a surface habitat, not a
separate subterranean compartment.

Foundational properties are still represented explicitly:

- Size establishes mass, metabolic demand, fecundity, population density, and
  normal prey-size limits.
- Temperate, frigid, and hot biochemistry shift the whole temperature envelope
  and impose different productivity costs.
- Motile organisms select an ectothermic, endothermic, or heterothermic thermal
  strategy. Annual lifecycle and dormancy traits can avoid seasons in which the
  active organism could not function.

### 2. Compiled biology

Species are compiled only when they are created or mutate. Compilation produces:

- primitive scalar parameters for climate, maintenance, reproduction, reserves,
  dormancy, and dispersal;
- fixed habitat and strategy support arrays;
- fit scores for a whitelist of meaningful `Habitat x Strategy` niches;
- a dense directed species-pair interaction matrix;
- cached migration destinations or neighbor movement rules.

The pair matrix is rebuilt by row and column when a species is added. A few
hundred global species make this cheap, while local turns can perform direct
array lookups for their 20–30 populations.

### 3. Seasonal runtime

Each populated cell stores compact parallel arrays:

- species index;
- established niche index;
- active biomass;
- reserve energy;
- dormant biomass.

A turn calculates environmental production, competition, ordinary feeding,
targeted relationships, stress mortality, reserve use, dormancy transitions,
reproduction, dispersal, and local extinction. It allocates no trait objects and
does not discover ecological relationships.

## Climate model

### Temperature

The default active envelope has an optimum plateau of 15–25 C and an outer
viable range of 0–40 C. Outside the plateau, fitness declines linearly to zero at
the outer bound. Biochemistry shifts all four bounds. Descriptive structures
then shift or widen them at a cost.

This intentionally cannot distinguish:

- two species that share the same seasonal tolerance but achieve it through
  different fine-grained heat-transfer mechanisms;
- brief daily extremes from a mild seasonal average;
- humidity-dependent heat tolerance beyond the simple interaction between a
  cooling trait and water need;
- body-part temperatures or different juvenile and adult tolerances.

Those distinctions are traded for four player-visible temperature numbers.

Size can provide a small symmetric widening of the temperature envelope. No
thermal inertia is simulated.

The seasonal climate sample receives deterministic, smoothly interpolated
variability before ecology evaluates it. One octave changes over seasons and a
second over roughly 18 years. Their combined bounds are +/-2 C and +/-25%
precipitation. The tile id seeds the curves, so save/load and repeated tests
produce the same weather history.

### Water

Land habitats receive one `waterAvailability` value in `[0, 1]`. It is derived
once per cell-season from precipitation, a temperature/insolation evaporation
term, river and delta bonuses, snow/ice state, and the surface stone's moisture
capacity. Aquatic habitats are water-saturated.

Species have a minimum, an optimal maximum, and an absolute maximum water
availability. The default upper bounds are both `1.0`, so most organisms are not
penalized for a wet cell. Traits such as water storage lower the minimum.
Explicit flood-sensitive adaptations can lower the upper bounds. Generic
burrowing does not: wetland and intertidal burrowers demonstrate that burrowing
alone is not a dry-habitat commitment. `DRY_BURROW_NEST` is the initial
descriptive upper-limit trait.

Evaporative cooling can improve high-temperature tolerance while increasing the
minimum water requirement. This keeps dehydration and overheating distinct
enough for a dry-adapted animal and a water-dependent heat-dumping animal to
behave differently, without tracking body water.

### Light

Light uses:

- normalized seasonal insolation, also standing in for photoperiod;
- five hard-coded star-light classes: blue-white, white, yellow, orange, red;
- a small hard-coded pigment-color compatibility table;
- one canopy attenuation scalar.

Photosynthetic production is the product of usable light, pigment match, and
climate suitability. Non-photosynthetic camouflage uses a separate hard-coded
color/habitat match. This loses continuous spectra, wavelength-dependent canopy
filtering, multiple pigment mixtures, and color changes with atmospheric
conditions. It retains understandable choices such as a red-star pigment or a
shade-adapted canopy-floor producer.

## Habitats and niches

Habitats are spatial compartments:

- land surface
- canopy
- freshwater
- coastal
- sunlit water
- dark water
- aerial

Freshwater exists only on a land cell adjacent to a major river or on a future
lake cell. Coastal water is exposed by a land cell adjacent to ocean. Sunlit
water may be absent where no useful light reaches the surface; dark water may be
absent in shallow water. Bottom-dwelling organisms occupy the deepest available
aquatic compartment rather than a separate seafloor habitat.

A niche is a whitelisted `Habitat x Strategy` pair. Each strategy definition
owns its supported habitats and its resource-resolution rule; each habitat
definition owns properties such as aquatic identity, light transmission, and
default camouflage matching. Strategies include
photosynthesis, filter feeding, grazing, ambush predation, pursuit predation,
scavenging, decomposition, coprophagy, deposit feeding, parasitism, and
absorption.

Habitat and strategy definitions score the traits that support them. A new local
population establishes in the valid niche maximizing:

`trait fit * habitat availability * resource support / (1 + competition)`

It keeps that niche until local extinction. This makes niches legible and
editable, provides a soft diversity limit, and avoids surprising emergent roles.
It loses continuous niche splitting and cannot discover a wholly new strategy
unless a strategy definition is added.

Niches do four jobs:

1. explain a population's local ecological role to the player;
2. decide whether establishment is plausible;
3. group broad competition;
4. limit diversity through finite habitat/resource opportunity.

They do not replace climate fitness or exact pair interactions.

## Food and interactions

Food is normally inferred from habitat, strategy, size, and modeled
populations rather than authored independently:

- filter feeding -> motile minuscule populations, plus tiny populations only
  for huge filter feeders;
- predation -> eligible motile populations selected by shared habitat and
  relative size;
- aquatic and terrestrial grazing -> sessile photosynthetic populations;
- scavenging -> carrion;
- decomposition -> dead sessile detritus;
- coprophagy -> waste from living motile organisms;
- aquatic deposit feeding -> marine snow;
- photosynthesis -> usable light and fertility.

Five globally authored aggregate guilds replace the old implicit creature
pools:

- Carpet plants (`TINY`, terrestrial photosynthesizer)
- Bugs (`TINY`, terrestrial grazer)
- Small aquatic life (`TINY`, aquatic filter feeder)
- Plankton (`MINUSCULE`, aquatic photosynthesizer)
- Aeroplankton (`MINUSCULE`, aerial photosynthesizer)

They are ordinary `SpeciesDefinition` populations tagged `INVARIANT`. The tag
means they do not mutate or speciate; it does not grant ecological immunity.
They occupy niches, compete, feed, are eaten, enter propagule dormancy, and can
go locally or globally extinct under the normal rules. No background seed
recreates an extinct guild. `INVARIANT_RESISTANCE` supplies broad but finite
climate performance and is rejected on evolving species.

Carpet plants are exposed on land by default. Aeroplankton is opt-in because
most test ecosystems do not include an authored aerial food web.

The remaining functional pools represent matter rather than creatures:

- carrion
- detritus
- waste
- marine snow

Consumer definitions never name or inspect the invariant guilds. The compiler
applies the same size, motility, strategy, and shared-habitat rules to invariant
and evolving targets, so guild prevalence responds to climate, predation, and
competition.

Carrion, detritus, and waste are stateful seasonal inputs. Motile deaths alone
produce carrion, sessile deaths alone produce detritus, and living motile
assimilation produces waste. Decomposer and coprophage strategies consume the
corresponding availability index. Producer traits may turn waste into a
fertility bonus without replacing the producer's photosynthetic niche.
Organic pools retain and decay their previous state before adding this season's
new material. Consumption cannot spend biomass produced only at the end of the
same turn. At most 75% of retained detritus is accessible in one season, which
represents spatially or chemically protected litter and prevents a zero/refill
sawtooth.

Predation between modeled species is compiled from strategy, shared habitat,
relative size, defenses, capture traits, and camouflage. The seasonal runtime
looks up a coefficient and applies donor loss and consumer gain with explicit
efficiency. Capture rate controls the target population's seasonal mortality;
assimilation efficiency describes the gross usable intake available to the
consumer before maintenance, reserves, and reproduction.

The normal prey-size rule does not let a medium predator live on tiny aggregate
insects. Ecologically important exceptions use authored directed relationships:
for example, cooperative wolf hunting targets moose without changing the
generic prey rule for every medium predator.

Traits may create a directed exception against `ExactSpecies(id)` or, later,
`DescendantsOf(ancestorId)`. A specialized supplemental food does not change the
consumer's niche. If an exact target goes extinct the adaptation simply provides
no benefit. The same edge mechanism can later represent host-specific parasites,
pollination, seed dispersal, and commensal relationships.

## Population dynamics

### State

Active biomass performs ecological work. Reserve energy buffers a fraction of
one season's unmet demand and pays for migration and reproduction. Dormant
biomass consumes almost nothing, performs no feeding or photosynthesis, and
suffers slow attrition.

When active conditions are poor, an organism with an appropriate dormancy trait
may transfer biomass into the dormant pool. It can return when conditions cross
a reactivation threshold. Propagule dormancy, burrowing eggs, seasonal torpor,
and whole-body desiccation use this same state with different transition rates,
survival, and eligibility checks. Complete self-desiccation is unavailable while
immersed.

Stage structure is deferred. Dormant biomass is a persistence state, not a full
egg/juvenile/adult model.

### Seasonal update

For each population:

1. Calculate temperature, water, light, habitat, and seasonal-behavior fitness.
2. Combine non-food problems into one bounded environmental stress.
3. Calculate primary production, organic-pool intake, and size-overlap niche competition.
4. Apply precomputed modeled-species feeding interactions.
5. Spend intake on maintenance, then refill reserves, then reproduce.
6. Use reserves against shortfalls before starvation mortality.
7. Apply stress, starvation, predation, and background mortality separately so
   the UI can explain each.
8. Move biomass between active and dormant state.
9. Emit local dispersal or cached-route migration.
10. Remove a local population below the viable individual threshold after
    incoming dispersal.

Rates are bounded and updates use the previous state for every population, which
makes results deterministic and independent of population iteration order.

### Stability and diversity

Stability comes from several soft negative feedback loops:

- production is capped by cell area, habitat, light, water, and fertility;
- intake saturates rather than scaling without bound;
- members of the same and overlapping niches share finite opportunity;
- maintenance grows with biomass;
- reproduction slows near carrying opportunity;
- predators decline after prey declines because reserves are finite;
- small local populations fail the minimum viable population check;
- dormancy preserves lineages but contributes no current ecological output.

A hard species cap is only an emergency assertion. The target steady state is
roughly 20–30 modeled populations per populated cell and a few hundred species
globally.

## Environment and geology boundary

The runtime accepts an ecology-specific seasonal cell record rather than reading
`PlanetTile` directly. A later adapter will provide:

- cell area;
- seasonal temperature, insolation, and precipitation;
- surface-stone fertility, moisture capacity, and acidity;
- adjacency to major river and ocean;
- water depth and whether useful sunlight reaches water;
- canopy and reef cover;
- ocean circulation-region id;
- snow/ice inferred from current and annual temperature plus moisture.

Fertility scales area-based producer capacity. Moisture capacity modifies
seasonal water availability. Stone-adaptation traits should generally target
procedural properties (acidic, infertile, low-retention) and may optionally
target a stable generated stone id for a narrow specialist.

`reefCover` is an aquatic habitat modifier. Reef building spends producer
surplus to raise it slowly; cover decays when builders disappear. Reef nesting,
reef camouflage, and reef boring convert cover into habitat/feeding advantages.
Terrestrial construction is deferred.

Marine snow is produced by aquatic deaths. A later planet adapter can aggregate
it by the existing circular current region and expose the result with a one-turn
lag. Carrion remains local.

## Movement and biogeography

- Neighbor dispersal has no fixed destination and slowly radiates viable
  populations.
- Short, regional, and long-distance migration traits cache seasonal destination
  sets or routes when the species first obtains the trait.
- A fixed migration route can become harmful after climate change; routes are
  not magically re-optimized each turn.
- A species' accessible biogeographic mask, realized range, and current
  propagule reach are separate.
- A `BiotaDistribution` seeds a prototype ancestor into an intentionally broad
  accessible region. Descendants radiate and specialize within it. Some
  ancestors may have a global accessible mask.

Dispersal is applied before local minimum-viable-population removal, so rescue
works only when immigrants make the destination cell viable. Global extinction
is recorded when no viable active or dormant population remains anywhere.
The one-tile prototype threshold is two individuals because a tile covers about
40,000 km2. A later regional pass should treat only a handful of individuals
spread across several tiles as extinct.

## Performance budget

For 20,000 cells and 30 populations per cell:

- 600,000 population updates;
- at most 900 direct pair lookups per cell, or 18 million very small lookups;
- no per-turn hash maps, trait traversal, relationship discovery, or solver
  allocation;
- primitive arrays and stable integer indexes inside the runtime;
- reusable scratch buffers sized to the largest local community.

The initial target is under 300 ms for one warmed seasonal turn on the
development machine. The benchmark reports median and slowest time rather than
asserting a hardware-dependent limit in the normal unit-test task.

## Prototype acceptance checks

- Compiling a trait set is deterministic and rejects invalid foundations.
- Every ordinary trait has both a benefit and a cost.
- Habitat and strategy traits produce sensible derived niches.
- Exact-species diet and host edges affect only their selected target.
- Invariant-only traits are rejected on evolving species.
- Filter-feeding size rules are identical for invariant and evolving prey.
- Medium predators cannot consume tiny invariant insects through a special
  aggregate-guild exception.
- Climate mismatches cause visible stress; reserves delay starvation but do not
  create energy.
- Dry-burrow specialists suffer in saturated land cells while generic burrowers
  do not receive an automatic wetness penalty.
- Dormant biomass survives otherwise lethal temporary seasons but cannot feed.
- Freshwater and coastal niches appear only where their compartments exist.
- Reef users gain only from aquatic reef cover.
- Updates remain finite, non-negative, and independent of iteration order.
- A long unchanged run approaches bounded variation.
- Invasion and climate-change experiments can disrupt and later settle.
- A 20,000-cell, 20–30-population benchmark is measured and documented.

## Deferred until integration

- save migration and UI;
- mutation probabilities and trait inheritance;
- full life-stage structure;
- daily weather and short extreme events;
- explicit atmospheric chemistry, pressure, pH, UV, and salinity;
- terrestrial construction;
- lakes;
- dynamic discovery of new strategy definitions.
