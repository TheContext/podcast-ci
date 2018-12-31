package io.thecontext.ci.output

import io.reactivex.Scheduler
import io.reactivex.Single
import io.thecontext.ci.toDate
import io.thecontext.ci.toRfc2822
import io.thecontext.ci.value.Episode
import io.thecontext.ci.value.Person
import io.thecontext.ci.value.Podcast
import io.thecontext.ci.value.find
import java.time.LocalDate

interface PodcastXmlFormatter {

    fun format(podcast: Podcast, episodes: List<Episode>, people: List<Person>): Single<String>

    class Impl(
            private val episodeMarkdownFormatter: EpisodeMarkdownFormatter,
            private val markdownRenderer: MarkdownRenderer,
            private val mustacheRenderer: MustacheRenderer,
            private val ioScheduler: Scheduler
    ) : PodcastXmlFormatter {

        companion object {
            private const val TEMPLATE_RESOURCE_NAME = "podcast.xml.mustache"
        }

        override fun format(podcast: Podcast, episodes: List<Episode>, people: List<Person>) = Single
                .concat(episodes.sortedBy { it.date.toDate() }.map { episode ->
                    episodeMarkdownFormatter.format(podcast, episode, people).map { episode to it }
                })
                .toList()
                .map { preparedEpisodes ->
                    val contents = mapOf(
                            "title" to podcast.title,
                            "language" to "${podcast.language.code.toLowerCase()}-${podcast.language.regionCode.toLowerCase()}",
                            "url" to podcast.url,
                            "description" to podcast.description,
                            "artwork_url" to podcast.artworkUrl,
                            "explicit" to if (podcast.explicit) "yes" else "no",
                            "category" to podcast.category,
                            "subcategory" to podcast.subcategory,
                            "owner_name" to people.find(podcast.people.ownerId).name,
                            "owner_email" to people.find(podcast.people.ownerId).email!!,
                            "authors" to podcast.people.authorIds.map { people.find(it).name }.joinToString(),
                            "build_date" to LocalDate.now().toRfc2822(),
                            "episodes" to preparedEpisodes.map { (episode, episodeMarkdown) ->
                                val episodeTitle = if (episode.part == null) {
                                    "Episode ${episode.number}: ${episode.title}"
                                } else {
                                    "Episode ${episode.number}, Part ${episode.part}: ${episode.title}"
                                }

                                mapOf(
                                        "id" to episode.id,
                                        "title" to episodeTitle,
                                        "description" to episode.description,
                                        "date" to episode.date.toDate().toRfc2822(),
                                        "file_url" to episode.file.url,
                                        "file_length" to episode.file.length,
                                        "url" to episode.url,
                                        "discussion_url" to episode.discussionUrl,
                                        "duration" to episode.duration,
                                        "summary" to markdownRenderer.renderHtml(episodeMarkdown).trim().prependIndent(" ".repeat(10))
                                )
                            }
                    )

                    mustacheRenderer.render(TEMPLATE_RESOURCE_NAME, contents)
                }
                .subscribeOn(ioScheduler)
    }
}
