package io.thecontext.ci.output

import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.Function3
import io.thecontext.ci.output.feed.FeedEpisodeRenderer
import io.thecontext.ci.output.feed.FeedRenderer
import io.thecontext.ci.output.website.WebsiteRenderer
import io.thecontext.ci.value.Episode
import io.thecontext.ci.value.Person
import io.thecontext.ci.value.Podcast
import java.io.File

interface OutputWriter {

    object FileNames {
        const val FEED = "podcast.rss"
    }

    fun write(showNotesDirectory: File, rssFeedDirectory: File, websiteDirectory: File, podcast: Podcast, episodes: List<Episode>, people: List<Person>): Single<Unit>

    class Impl(
            private val feedRenderer: FeedRenderer,
            private val feedEpisodeRenderer: FeedEpisodeRenderer,
            private val websiteRenderer: WebsiteRenderer,
            private val textWriter: TextWriter,
            private val ioScheduler: Scheduler
    ) : OutputWriter {

        override fun write(showNotesDirectory: File, rssFeedDirectory: File, websiteDirectory: File, podcast: Podcast, episodes: List<Episode>, people: List<Person>): Single<Unit> {
            val notes = Single
                    .merge(episodes.map { episode -> feedEpisodeRenderer.render(podcast, episode, people).map { episode to it } })
                    .toList()
                    .flatMap {
                        val operations = it.map { (episode, episodeMarkdown) ->
                            Single.fromCallable {
                                showNotesDirectory.mkdirs()

                                textWriter.write(File(showNotesDirectory, "${episode.slug}.md"), episodeMarkdown)
                            }
                        }

                        Single.merge(operations).toList()
                    }
                    .map { Unit }

            val feed = feedRenderer.render(podcast, episodes, people)
                    .flatMap { podcastXml ->
                        Single.fromCallable {
                            rssFeedDirectory.mkdirs()

                            textWriter.write(File(rssFeedDirectory, FileNames.FEED), podcastXml)
                        }
                    }
                    .map { Unit }

            val website = Single
                    .merge(episodes.map { episode -> websiteRenderer.render(podcast, episode, people).map { episode to it } })
                    .toList()
                    .flatMap {
                        websiteDirectory.mkdirs()
                        val operations = it.map { (episode, episodeWebsiteMarkdown) ->
                            Single.fromCallable {
                                textWriter.write(File(websiteDirectory, "${episode.date}-${episode.slug}.md"), episodeWebsiteMarkdown)
                            }
                        }

                        Single.merge(operations).toList()
                    }
                    .map { Unit }

            return Single.zip(notes, feed, website, Function3<Unit, Unit, Unit, Unit> { _, _, _ -> Unit }).subscribeOn(ioScheduler)
        }
    }
}