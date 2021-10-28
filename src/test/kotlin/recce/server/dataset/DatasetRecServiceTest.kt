package recce.server.dataset

import io.r2dbc.spi.RowMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import recce.server.config.DataLoadDefinition
import recce.server.config.DatasetConfiguration
import recce.server.config.ReconciliationConfiguration

internal class DatasetRecServiceTest {
    private val testDataset = "test-dataset"
    private val recRun = RecRun(1, testDataset)

    private val runService = mock<RecRunService> {
        on { start(testDataset) } doReturn Mono.just(recRun)
        on { complete(recRun) } doReturn Mono.just(recRun)
    }

    private val emptyDataLoad = mock<DataLoadDefinition> {
        on { runQuery() } doReturn Flux.empty()
    }

    private val mockMeta = mock<RowMetadata>()

    private val singleRowResult = mock<io.r2dbc.spi.Result> {
        on { map<HashedRow>(any()) } doReturn Flux.just(HashedRow("abc", "def", mockMeta))
    }

    private val singleRowDataLoad = mock<DataLoadDefinition> {
        on { runQuery() } doReturn Flux.just(singleRowResult)
    }

    private val testRecordKey = RecRecordKey(1, "abc")
    private val testRecord = RecRecord(testRecordKey)
    private val recordRepository = mock<RecRecordRepository> {
        on { save(any()) } doReturn Mono.just(testRecord)
    }

    @Test
    fun `should throw on missing dataset`() {
        assertThatThrownBy { DatasetRecService(mock(), mock(), mock()).runFor(testDataset) }
            .isExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining(testDataset)
    }


    @Test
    fun `should reconcile empty datasets without error`() {
        val service = DatasetRecService(
            ReconciliationConfiguration(mapOf(testDataset to DatasetConfiguration(emptyDataLoad, emptyDataLoad))),
            runService,
            mock()
        )
        StepVerifier.create(service.runFor(testDataset))
            .assertNext {
                assertThat(it.results).isEqualTo(RecRunResults(DatasetResults(), DatasetResults()))
            }
            .verifyComplete()
    }

    @Test
    fun `should reconcile source with empty target`() {
        val service = DatasetRecService(
            ReconciliationConfiguration(mapOf(testDataset to DatasetConfiguration(singleRowDataLoad, emptyDataLoad))),
            runService,
            recordRepository
        )

        StepVerifier.create(service.runFor(testDataset))
            .assertNext {
                assertThat(it.results).isEqualTo(RecRunResults(DatasetResults(), DatasetResults()))
            }
            .verifyComplete()

        verify(recordRepository).save(RecRecord(testRecordKey, sourceData = "def"))
        verify(runService).complete(recRun)
    }

    @Test
    fun `should reconcile empty source with target`() {
        `when`(recordRepository.findById(testRecordKey)).doReturn(Mono.empty())

        val service = DatasetRecService(
            ReconciliationConfiguration(mapOf(testDataset to DatasetConfiguration(emptyDataLoad, singleRowDataLoad))),
            runService,
            recordRepository
        )

        StepVerifier.create(service.runFor(testDataset))
            .assertNext {
                assertThat(it.results).isEqualTo(RecRunResults(DatasetResults(), DatasetResults()))
            }
            .verifyComplete()

        verify(recordRepository).save(RecRecord(testRecordKey, targetData = "def"))
        verify(runService).complete(recRun)
    }

    @Test
    fun `should reconcile source with target`() {
        `when`(recordRepository.findById(testRecordKey)).doReturn(Mono.empty())

        val service = DatasetRecService(
            ReconciliationConfiguration(
                mapOf(
                    testDataset to DatasetConfiguration(
                        singleRowDataLoad,
                        singleRowDataLoad
                    )
                )
            ),
            runService,
            recordRepository
        )

        StepVerifier.create(service.runFor(testDataset))
            .assertNext {
                assertThat(it.results).isEqualTo(RecRunResults(DatasetResults(), DatasetResults()))
            }
            .verifyComplete()

        verify(recordRepository).save(RecRecord(testRecordKey, sourceData = "def"))
        verify(recordRepository).save(RecRecord(testRecordKey, targetData = "def"))
        verify(runService).complete(recRun)
    }

}
