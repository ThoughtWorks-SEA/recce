package com.thoughtworks.recce.server.dataset

import com.thoughtworks.recce.server.config.DataLoadDefinition.Companion.migrationKeyColumnName
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDateTime

internal class HashedRowTest {

    val meta = mock<RowMetadata> {
        on { columnNames } doReturn arrayListOf(migrationKeyColumnName, "test")
    }

    @Test
    fun `should throw on no migration key`() {
        val row = mock<Row> {
            on { get(migrationKeyColumnName, String::class.java) } doReturn null
        }
        assertThatThrownBy { toHashedRow(row, meta) }
            .isExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Result rows do not have String column called")
    }

    @Test
    fun `should throw on unrecognized type`() {
        val row = mockSingleColumnRowReturning(LocalDateTime.now())
        assertThatThrownBy { toHashedRow(row, meta) }
            .isExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("LocalDateTime")
            .hasMessageContaining("test")
    }

    @ParameterizedTest
    @MethodSource("types")
    fun `should hash all column types`(type: Class<Any>, input: Any, expectedHash: String) {
        val row = mockSingleColumnRowReturning(input)
        assertThat(toHashedRow(row, meta)).isEqualTo(HashedRow("key", expectedHash))
    }

    private fun mockSingleColumnRowReturning(input: Any): Row {
        val row = mock<Row> {
            on { get(migrationKeyColumnName, String::class.java) } doReturn "key"
            on { get("test") } doReturn input
        }
        return row
    }

    companion object {
        @JvmStatic
        fun types() = listOf(
            Arguments.of(Boolean::class.java, true, "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a"),
            Arguments.of(
                BigDecimal::class.java,
                BigDecimal.TEN,
                "596c0ad17b38f4bf6c899f6b02c82f9ee326cfb7ac2d9775f49a88163364882b"
            ),
            Arguments.of(
                Short::class.java,
                Integer.valueOf(10).toShort(),
                "102b51b9765a56a3e899f7cf0ee38e5251f9c503b357b330a49183eb7b155604"
            ),
            Arguments.of(
                Integer::class.java,
                Integer.valueOf(10),
                "075de2b906dbd7066da008cab735bee896370154603579a50122f9b88545bd45"
            ),
            Arguments.of(
                Long::class.java,
                Integer.valueOf(10).toLong(),
                "a111f275cc2e7588000001d300a31e76336d15b9d314cd1a1d8f3d3556975eed"
            ),
            Arguments.of(
                Float::class.java,
                Integer.valueOf(10).toFloat(),
                "80c8a717ccd70c8809eb78e6a9591c003e11c721fe0ccaf62fd592abda1a5593"
            ),
            Arguments.of(
                Double::class.java,
                Integer.valueOf(10).toDouble(),
                "24b1f4ef66b650ff816e519b01742ff1753733d36e1b4c3e3b52743168915b1f"
            ),
            Arguments.of(String::class.java, "10", "4a44dc15364204a80fe80e9039455cc1608281820fe2b24f1e5233ade6af1dd5"),
            Arguments.of(
                ByteBuffer::class.java,
                ByteBuffer.wrap("10".toByteArray(Charsets.UTF_8)),
                "4a44dc15364204a80fe80e9039455cc1608281820fe2b24f1e5233ade6af1dd5"
            ),
        )
    }
}