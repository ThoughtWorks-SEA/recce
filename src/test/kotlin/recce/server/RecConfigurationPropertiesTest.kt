package recce.server

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.context.exceptions.ConfigurationException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import recce.server.dataset.DataLoadRole

// Faster tests that do not load the full configuration and are quicker to iterate on when testing
// configuration binding
internal class RecConfigurationPropertiesTest {

    private val items = HashMap<String, Any>()

    @BeforeEach
    fun setUp() {
        items["flyway.datasources.default.enabled"] = false
        items["r2dbc.datasources.source.url"] =
            "r2dbc:h2:mem:///sourceDb;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        items["r2dbc.datasources.target.url"] =
            "r2dbc:h2:mem:///targetDb;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        items["reconciliation.datasets.test-dataset.source.dataSourceRef"] = "source"
        items["reconciliation.datasets.test-dataset.source.query"] = "select count(*) as sourcedatacount from testdata"
        items["reconciliation.datasets.test-dataset.target.dataSourceRef"] = "target"
        items["reconciliation.datasets.test-dataset.target.query"] = "select count(*) as targetdatacount from testdata"
    }

    @Test
    fun `should parse from properties`() {
        val ctx = ApplicationContext.run(items)

        Assertions.assertThat(ctx.getBean(RecConfiguration::class.java).datasets.values)
            .hasSize(1)
            .first().satisfies {
                Assertions.assertThat(it.name).isEqualTo("test-dataset")
                Assertions.assertThat(it.source.role).isEqualTo(DataLoadRole.source)
                Assertions.assertThat(it.source.dataSourceRef).isEqualTo("source")
                Assertions.assertThat(it.source.query).contains("sourcedatacount")
                Assertions.assertThat(it.source.dbOperations).isNotNull
                Assertions.assertThat(it.target.role).isEqualTo(DataLoadRole.target)
                Assertions.assertThat(it.target.dataSourceRef).isEqualTo("target")
                Assertions.assertThat(it.target.query).contains("targetdatacount")
                Assertions.assertThat(it.target.dbOperations).isNotNull
            }
    }

    @Test
    fun `should fail load on incorrect dataSourceRef`() {
        items["reconciliation.datasets.test-dataset.source.dataSourceRef"] = "non-existent-datasource"

        Assertions.assertThatThrownBy { ApplicationContext.run(items) }
            .isExactlyInstanceOf(BeanInstantiationException::class.java)
            .hasMessageContaining("non-existent-datasource")
            .hasRootCauseExactlyInstanceOf(ConfigurationException::class.java)
            .hasMessageContaining("non-existent-datasource")
    }
}
