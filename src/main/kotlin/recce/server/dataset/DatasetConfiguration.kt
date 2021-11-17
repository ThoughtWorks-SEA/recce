package recce.server.dataset

import io.micronaut.context.BeanLocator
import recce.server.PostConstructable
import javax.validation.constraints.NotNull

class DatasetConfiguration(@NotNull val source: DataLoadDefinition, @NotNull val target: DataLoadDefinition) :
    PostConstructable {
    lateinit var name: String
    override fun populate(locator: BeanLocator) {
        source.role = DataLoadRole.source
        source.populate(locator)
        target.role = DataLoadRole.target
        target.populate(locator)
    }

    val datasourceDescriptor: String
        get() = "(${source.dataSourceRef} -> ${target.dataSourceRef})"

    override fun toString(): String {
        return name
    }
}
