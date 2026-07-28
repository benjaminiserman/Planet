package dev.biserman.planet.planet.ecology.v2

import com.fasterxml.jackson.module.kotlin.readValue
import dev.biserman.planet.utils.Serialization
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class EcologyGlobalsTest {
    @Test
    fun `checked in ecology config matches the reloadable globals`() {
        val checkedIn = Serialization.configMapper.readTree(File("ecology_config.json"))
        val currentGlobals = Serialization.configMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(
            EcologyGlobals,
        )

        assertEquals(currentGlobals, checkedIn)
    }

    @Test
    fun `reloaded globals are captured by newly constructed runtime configs`() {
        val original = EcologyGlobals.backgroundMortality
        val existingSnapshot = EcologyRuntimeConfig()
        try {
            Serialization.configMapper.readValue<EcologyGlobals>(
                """{"backgroundMortality":0.123}""",
            )
            EcologyGlobals.validate()

            assertEquals(original, existingSnapshot.backgroundMortality)
            assertEquals(0.123, EcologyRuntimeConfig().backgroundMortality)
        } finally {
            EcologyGlobals.backgroundMortality = original
        }
    }
}
