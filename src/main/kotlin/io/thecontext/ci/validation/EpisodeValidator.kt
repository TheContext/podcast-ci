package io.thecontext.ci.validation

import io.reactivex.Single
import io.thecontext.ci.toDate
import io.thecontext.ci.value.Episode
import io.thecontext.ci.value.Person
import java.time.format.DateTimeParseException

class EpisodeValidator(
        private val urlValidator: Validator<String>,
        private val people: List<Person>
) : Validator<Episode> {

    override fun validate(value: Episode): Single<ValidationResult> {
        val urlResults = emptyList<String>()
                .plus(value.url)
                .plus(value.discussionUrl)
                .plus(value.file.url)
                .plus(value.notes.links.map { it.url })
                .map { urlValidator.validate(it) }

        val peopleResults = emptyList<String>()
                .plus(value.people.hostIds)
                .plus(value.people.guestIds)
                .map { personId ->
                    Single.fromCallable {
                        if (people.find { it.id == personId } == null) {
                            ValidationResult.Failure("Person [$personId] is not defined.")
                        } else {
                            ValidationResult.Success
                        }
                    }
                }

        val numberResult = Single.fromCallable {
            if (value.number < 0) {
                ValidationResult.Failure("Episode number is negative.")
            } else {
                ValidationResult.Success
            }
        }

        val dateResult = Single.fromCallable {
            try {
                value.date.toDate()

                ValidationResult.Success
            } catch (e: DateTimeParseException) {
                ValidationResult.Failure("Episode date is in wrong format. Should be YYYY-MM-DD.")
            }
        }

        val fileLengthResult = Single.fromCallable {
            if (value.file.length < 0) {
                ValidationResult.Failure("File length is negative.")
            } else {
                ValidationResult.Success
            }
        }

        return Single
                .merge(urlResults + peopleResults + listOf(numberResult, dateResult, fileLengthResult))
                .toList()
                .map { it.merge() }
    }
}
