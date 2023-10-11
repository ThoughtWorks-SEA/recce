package recce.server

import io.micronaut.runtime.Micronaut.build
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.security.SecuritySchemes
import mu.KotlinLogging
import reactor.tools.agent.ReactorDebugAgent

private const val GITHUB_PROJECT = "https://github.com/thoughtworks-sea/recce"
private val logger = KotlinLogging.logger {}

@OpenAPIDefinition(
    info =
        Info(
            title = "Recce Server",
            description = "Server-based database reconciliation tool for developers",
            contact = Contact(name = "Recce Community", url = "$GITHUB_PROJECT/issues")
        ),
    externalDocs = ExternalDocumentation(description = "Server Docs", url = "$GITHUB_PROJECT/README.md"),
    security = [SecurityRequirement(name = "basicAuth")]
)
@SecuritySchemes(
    SecurityScheme(
        paramName = "Authorization",
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
    )
)
object RecceServer {
    @JvmStatic
    @Suppress("SpreadOperator")
    fun main(args: Array<String>) {
        runCatching { ReactorDebugAgent.init() }
            .onFailure { logger.warn { "ReactorDebugAgent failed to initialise due to $it" } }
        build()
            .banner(false)
            .args(*args)
            .packages("recce.server")
            .eagerInitSingletons(true)
            .start()
    }
}
