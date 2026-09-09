package xyz.winhok.earthonline.core

import org.junit.Test
import org.junit.Assert.assertEquals

class DeadlineTest {
    @Test fun productionDeadlineScenarios() { assertEquals("All 45 contract scenarios must execute", 45, DeadlineScenarios.run()) }
}
