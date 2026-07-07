package dev.biserman.planet.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.deleteIfExists

data class SerializationFixture(val name: String, val values: List<Double>)

class SerializationTest {
    @Test
    fun `value survives a serialization round trip`() {
        val saveFile = Files.createTempFile("planet-round-trip", ".json.gz")
        try {
            val original = SerializationFixture("round trip", listOf(-12.5, 0.0, 42.25))

            Serialization.save(saveFile.toString(), original)
            val restored = Serialization.load(saveFile.toString(), SerializationFixture::class.java)

            assertEquals(original, restored)
        } finally {
            saveFile.deleteIfExists()
        }
    }
}
