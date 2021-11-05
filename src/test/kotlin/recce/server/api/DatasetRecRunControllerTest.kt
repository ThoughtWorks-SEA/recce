package recce.server.api

import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.specification.RequestSpecification
import jakarta.inject.Inject
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import recce.server.dataset.DatasetRecRunner
import recce.server.dataset.DatasetRecService
import recce.server.recrun.ColMeta
import recce.server.recrun.DatasetMeta
import recce.server.recrun.MatchStatus
import recce.server.recrun.RecRun
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val testDataset = "testDataset"
private val testCompletedDuration = Duration.ofMinutes(3).plusNanos(234)
private val testResults = RecRun(
    id = 12,
    datasetId = testDataset,
    createdTime = LocalDateTime.of(2021, 10, 25, 16, 16, 16).toInstant(ZoneOffset.UTC),
).apply {
    completedTime = createdTime?.plusNanos(testCompletedDuration.toNanos())
    updatedTime = completedTime?.plusSeconds(10)
    sourceMeta = DatasetMeta(listOf(ColMeta("test1", "String")))
    targetMeta = DatasetMeta(listOf(ColMeta("test1", "String")))
    summary = MatchStatus(1, 2, 3, 4)
}

internal class DatasetRecRunControllerTest {
    private val service = mock<DatasetRecService> {
        on { runFor(eq(testDataset)) } doReturn Mono.just(testResults)
    }

    private val controller = DatasetRecRunController(service)

    @Test
    fun `controller should delegate to service`() {
        StepVerifier.create(controller.create(DatasetRecRunController.RunCreationParams(eq(testDataset))))
            .assertNext {
                assertThat(it).usingRecursiveComparison().isEqualTo(testResults)
            }
            .verifyComplete()
    }
}

@MicronautTest
internal class DatasetRecRunControllerApiTest {

    @Inject
    lateinit var spec: RequestSpecification

    @Test
    fun `controller should delegate to service`() {
        Given {
            spec(spec)
        } When {
            body(mapOf("datasetId" to testDataset))
            post("/runs")
        } Then {
            statusCode(HttpStatus.SC_OK)
            body("datasetId", equalTo(testDataset))
            body("id", equalTo(testResults.id))
            body("createdTime", equalTo(DateTimeFormatter.ISO_INSTANT.format(testResults.createdTime)))
            body("completedTime", equalTo(DateTimeFormatter.ISO_INSTANT.format(testResults.completedTime)))
            body("completedDurationSeconds", closeTo(testCompletedDuration.toSeconds().toDouble(), 0.00001))
            body("sourceMeta.cols", equalTo(listOf(mapOf("name" to "test1", "javaType" to "String"))))
            body("targetMeta.cols", equalTo(listOf(mapOf("name" to "test1", "javaType" to "String"))))
            body(
                "summary",
                equalTo(
                    mapOf(
                        "sourceOnly" to 1,
                        "targetOnly" to 2,
                        "bothMatched" to 3,
                        "bothMismatched" to 4,
                        "sourceTotal" to 8,
                        "targetTotal" to 9,
                        "total" to 10,
                    )
                )
            )
        }
    }

    @Test
    fun `controller should validate params`() {
        val errors: List<Map<String, String>> =
            Given {
                spec(spec)
            } When {
                body(emptyMap<String, String>())
                post("/runs")
            } Then {
                statusCode(HttpStatus.SC_BAD_REQUEST)
                body("message", equalTo("Bad Request"))
            } Extract {
                path("_embedded.errors")
            }

        assertThat(errors)
            .singleElement()
            .satisfies {
                assertThat(it).hasEntrySatisfying("message") { message ->
                    assertThat(message).contains("Missing required creator property 'datasetId'")
                }
            }
    }

    @MockBean(DatasetRecRunner::class)
    fun reconciliationService(): DatasetRecRunner {
        return mock {
            on { runFor(eq(testDataset)) } doReturn Mono.just(testResults)
        }
    }
}
