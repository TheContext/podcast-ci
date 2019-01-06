package io.thecontext.podcaster

import io.thecontext.podcaster.value.Episode
import io.thecontext.podcaster.value.Person
import io.thecontext.podcaster.value.Podcast

val testPerson = Person(
        id = "Person ID",
        name = "Person Name",
        email = "person@mail.com",
        twitter = "Twitter ID",
        github = "GitHub ID",
        links = listOf(Person.Link(
                name = "blog",
                url = "https://localhost"
        ))
)

val testPodcast = Podcast(
        title = "Podcast Title",
        description = "Podcast Description",
        people = Podcast.People(
                ownerId = testPerson.id,
                authorIds = listOf(testPerson.id, testPerson.id)
        ),
        language = "en",
        explicit = false,
        category = "Podcast category",
        subcategory = "Podcast subcategory",
        url = "https://localhost/podcast",
        artworkUrl = "https://localhost/podcast/artwork"
)

val testEpisode = Episode(
        id = "thecontext/episode/42",
        number = 42,
        part = 2,
        title = "Episode Title",
        description = "Episode Description",
        people = Episode.People(
                hostIds = listOf(testPerson.id, testPerson.id),
                guestIds = listOf(testPerson.id, testPerson.id)
        ),
        discussionUrl = "https://localhost/discussion",
        time = "2000-12-30T10:15",
        duration = "100:00",
        file = Episode.File(
                url = "https://localhost/episode/file",
                length = 1_000_000
        ),
        slug = "slug",
        notesMarkdown = "Notes"
)