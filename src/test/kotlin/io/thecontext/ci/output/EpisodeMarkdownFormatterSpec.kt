package io.thecontext.ci.output

import com.greghaskins.spectrum.Spectrum
import com.greghaskins.spectrum.dsl.specification.Specification.context
import com.greghaskins.spectrum.dsl.specification.Specification.it
import io.reactivex.schedulers.Schedulers
import io.thecontext.ci.testEpisode
import io.thecontext.ci.testPerson
import io.thecontext.ci.testPodcast
import org.junit.runner.RunWith

@RunWith(Spectrum::class)
class EpisodeMarkdownFormatterSpec {
    init {
        val formatter = EpisodeMarkdownFormatter.Impl(
                mustacheRenderer = MustacheRenderer.Impl(),
                ioScheduler = Schedulers.trampoline()
        )

        val podcast = testPodcast
        val episode = testEpisode
        val people = listOf(testPerson, testPerson)

        context("regular episode") {

            it("formats") {
                val expected = """
                    # ${episode.title}

                    * [How to listen and subscribe](${podcast.url})
                    * [Discussion after the episode](${episode.discussionUrl})

                    ${episode.notes.descriptionMarkdown}

                    ## Guests

                    * ${people[0].name}: [Twitter](https://twitter.com/${people[0].twitter}), [GitHub](https://github.com/${people[0].github}), [website](${people[0].site})
                    * ${people[1].name}: [Twitter](https://twitter.com/${people[1].twitter}), [GitHub](https://github.com/${people[1].github}), [website](${people[1].site})

                    ## Hosts

                    * ${people[0].name}: [Twitter](https://twitter.com/${people[0].twitter}), [GitHub](https://github.com/${people[0].github}), [website](${people[0].site})
                    * ${people[1].name}: [Twitter](https://twitter.com/${people[1].twitter}), [GitHub](https://github.com/${people[1].github}), [website](${people[1].site})

                    ## Links

                    * [${episode.notes.links.first().title}](${episode.notes.links.first().url})

                    """

                // Note: Mustache inserts EOL in the end. It is simulated here using an empty line.
                formatter.format(podcast, episode, people)
                        .test()
                        .assertResult(expected.trimIndent())
            }
        }
    }
}