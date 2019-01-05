package io.thecontext.ci.output

import io.reactivex.Scheduler
import io.reactivex.Single
import io.thecontext.ci.value.Episode
import io.thecontext.ci.value.Person
import io.thecontext.ci.value.find

interface MarkdownEpisodeRenderer {

    fun render(template: TemplateRenderer.Template, episode: Episode, people: List<Person>): Single<String>

    class Impl(
            private val templateRenderer: TemplateRenderer,
            private val ioScheduler: Scheduler
    ) : MarkdownEpisodeRenderer {

        override fun render(template: TemplateRenderer.Template, episode: Episode, people: List<Person>) = Single
                .fromCallable {
                    val guests = episode.people.guestIds.orEmpty()
                            .map { people.find(it) }
                            .sortedBy { it.name }
                    val hosts = episode.people.hostIds
                            .map { people.find(it) }
                            .sortedBy { it.name }

                    val contents = mapOf(
                            "number" to episode.number,
                            "part" to (episode.part ?: Int.MIN_VALUE),
                            "part_available" to (episode.part != null),
                            "title" to episode.title,
                            "description" to episode.description,
                            "notes" to episode.notesMarkdown,
                            "guests_available" to guests.isNotEmpty(),
                            "guests" to guests.map { mapOf("guest" to formatPerson(it)) },
                            "hosts_available" to hosts.isNotEmpty(),
                            "hosts" to hosts.map { mapOf("host" to formatPerson(it)) },
                            "discussion_url" to episode.discussionUrl,
                            "file_url" to episode.file.url,
                            "time" to episode.time,
                            "slug" to episode.slug
                    )

                    templateRenderer.render(template, contents)
                }
                .subscribeOn(ioScheduler)

        private fun formatPerson(person: Person): String {
            val twitterLink = person.twitter?.let { Person.Link("Twitter", "https://twitter.com/$it") }
            val githubLink = person.github?.let { Person.Link("GitHub", "https://github.com/$it") }

            val links = listOfNotNull(twitterLink, githubLink) + person.links.orEmpty()

            return if (links.isEmpty()) {
                person.name
            } else {
                "${person.name}: ${links.map { "[${it.name}](${it.url})" }.joinToString(separator = ", ")}"
            }
        }
    }
}