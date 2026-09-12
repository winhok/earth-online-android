package xyz.winhok.earthonline.core

import org.junit.Test
import org.junit.Assert.assertEquals

class DeadlineTest {
    @Test fun productionDeadlineScenarios() {
        val count = DeadlineScenarios.run()
        println("EARTH_CONTRACT_SCENARIOS=$count")
        assertEquals("All 45 contract scenarios must execute", 45, count)
    }
}
