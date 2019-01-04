package io.thecontext.ci

import com.greghaskins.spectrum.Spectrum
import com.greghaskins.spectrum.dsl.specification.Specification.*
import io.thecontext.ci.context.Context
import io.thecontext.ci.output.OutputWriter
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime

@RunWith(Spectrum::class)
class IntegrationSpec {
    init {
        val workingDir = TemporaryFolder()

        beforeEach {
            workingDir.create()
        }

        context("run") {

            val actualFeedFileName = "feed.rss.xml"

            val inputDir = File("src/test/resources/integration/input")
            val expectOutputDir = File("src/test/resources/integration/output")
            val actualOutputDir by memoized { workingDir.root }

            beforeEach {
                val context = object : Context by Context.Impl() {
                    override val time = TestTime().apply {
                        currentResult = LocalDateTime.parse("2018-12-31T10:30:50")
                    }
                }

                Runner(
                        context = context,
                        inputDirectory = inputDir,
                        outputFeedFile = File(actualOutputDir, actualFeedFileName),
                        outputWebsiteDirectory = actualOutputDir
                ).run()
            }

            it("creates RSS feed") {
                val actualFile = File(actualOutputDir, actualFeedFileName)
                val expectFile = File(expectOutputDir, "feed.rss.xml")

                assertThat(actualFile)
                        .usingCharset(Charsets.UTF_8)
                        .hasSameContentAs(expectFile)
            }

            it("creates website") {
                File(expectOutputDir, "website")
                        .walkTopDown()
                        .filter { it.extension == "md" }
                        .forEach { expectFile ->
                            val actualFile = File(actualOutputDir, expectFile.name)

                            assertThat(actualFile)
                                    .usingCharset(Charsets.UTF_8)
                                    .hasSameContentAs(expectFile)
                        }
            }
        }

        afterEach {
            workingDir.delete()
        }
    }
}