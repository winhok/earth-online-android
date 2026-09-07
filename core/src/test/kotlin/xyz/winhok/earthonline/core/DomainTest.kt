package xyz.winhok.earthonline.core

import org.junit.Test

class DomainTest {
    @Test fun completeDomainScenarioSuite() { check(ScenarioSuite.run() == 41) }
}
