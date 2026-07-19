package dev.biserman.planet.planet.ecology

import dev.biserman.planet.planet.climate.ClimateDatum
import dev.biserman.planet.planet.climate.ClimateDatumSample

/** Default climate used by the notebook examples: mild, wet oceanic temperate seasons. */
val oceanicTemperateClimate = ClimateDatum(
    tileId = -1,
    months = listOf(
        ClimateDatumSample(6.0, 60.0, 115.0),
        ClimateDatumSample(6.5, 90.0, 100.0),
        ClimateDatumSample(8.0, 130.0, 90.0),
        ClimateDatumSample(10.0, 180.0, 80.0),
        ClimateDatumSample(13.0, 220.0, 75.0),
        ClimateDatumSample(16.0, 250.0, 70.0),
        ClimateDatumSample(18.0, 260.0, 70.0),
        ClimateDatumSample(18.0, 220.0, 75.0),
        ClimateDatumSample(15.5, 160.0, 85.0),
        ClimateDatumSample(12.0, 110.0, 100.0),
        ClimateDatumSample(9.0, 70.0, 115.0),
        ClimateDatumSample(7.0, 50.0, 120.0),
    ),
)

