package com.example.alertapp.config.migration

import kotlin.test.*

class ConfigVersionTest {
    @Test
    fun testVersionComparison() {
        val v100 = ConfigVersion(1, 0, 0)
        val v110 = ConfigVersion(1, 1, 0)
        val v111 = ConfigVersion(1, 1, 1)
        val v200 = ConfigVersion(2, 0, 0)

        assertTrue(v100 < v110)
        assertTrue(v110 < v111)
        assertTrue(v111 < v200)
        assertTrue(v100 < v200)

        assertFalse(v110 < v100)
        assertFalse(v111 < v110)
        assertFalse(v200 < v111)
    }

    @Test
    fun testVersionFromString() {
        val version = ConfigVersion.fromString("1.2.3")
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }

    @Test
    fun testInvalidVersionString() {
        assertFailsWith<IllegalArgumentException> {
            ConfigVersion.fromString("1.2")
        }
        assertFailsWith<IllegalArgumentException> {
            ConfigVersion.fromString("1.2.3.4")
        }
        assertFailsWith<NumberFormatException> {
            ConfigVersion.fromString("1.a.3")
        }
    }

    @Test
    fun testVersionToString() {
        val version = ConfigVersion(1, 2, 3)
        assertEquals("1.2.3", version.toString())
    }
}
