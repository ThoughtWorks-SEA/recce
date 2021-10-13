package com.thoughtworks.recce.server.config

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@MicronautTest(
    environments = arrayOf("test-integration"),
    propertySources = arrayOf("classpath:config/application-test-dataset.yml"),
)
internal class ReconciliationConfigurationTest {

    @Inject
    lateinit var config: ReconciliationConfiguration

    @Test
    fun `should parse dataset configuration from yaml`() {
        assertThat(config.datasets)
            .hasSize(1)
            .hasEntrySatisfying("test-dataset") {
                assertThat(it.name).isEqualTo("test-dataset")
                assertThat(it.source.dataSourceRef).isEqualTo("reactive-source")
                assertThat(it.source.query).contains("sourcedatacount")
                assertThat(it.source.dbOperations).isNotNull
                assertThat(it.target.dataSourceRef).isEqualTo("reactive-target")
                assertThat(it.target.query).contains("targetdatacount")
                assertThat(it.target.dbOperations).isNotNull
            }
    }
}