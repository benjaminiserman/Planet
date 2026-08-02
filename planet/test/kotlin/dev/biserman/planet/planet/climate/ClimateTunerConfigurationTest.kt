package dev.biserman.planet.planet.climate

import com.fasterxml.jackson.databind.node.ObjectNode
import dev.biserman.planet.utils.Serialization
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class ClimateTunerConfigurationTest {
    @Test
    fun `checked-in climate config fits the tuning space`() {
        val mapper = Serialization.configMapper
        val config = mapper.readTree(File("climate_config.json")) as ObjectNode
        val space = mapper.readValue(File("climate_tuning.json"), ClimateTuningSpace::class.java)

        space.parameters.forEach { parameter ->
            val value = config.get(parameter.name)?.doubleValue()
                ?: error("${parameter.name} is missing from climate_config.json")
            assertTrue(
                value in parameter.min..parameter.max,
                "${parameter.name}=$value is outside ${parameter.min}..${parameter.max}",
            )
        }
    }

    @Test
    fun `unknown command line options are rejected`() {
        val error = assertThrows<IllegalArgumentException> {
            runClimateTuner(arrayOf("--tuning-space", "somewhere.json"))
        }

        assertTrue(error.message.orEmpty().contains("Unknown option: --tuning-space"))
    }
}
