package xyz.winhok.earthonline.core

import org.junit.Test

class DomainTest {
    @Test fun completeDomainScenarioSuite() {
        val count = ScenarioSuite.run()
        println("EARTH_DOMAIN_SCENARIOS=$count")
        check(count == 41)
    }
}
