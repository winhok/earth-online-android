package xyz.winhok.earthonline.core

import org.junit.Test

class DeadlineTest {
    @Test fun productionDeadlineScenarios() { check(DeadlineScenarios.run() == 40) }
}
