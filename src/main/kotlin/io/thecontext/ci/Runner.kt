package io.thecontext.ci

import io.reactivex.Observable
import io.reactivex.Single
import io.thecontext.ci.context.Context
import io.thecontext.ci.context.InputContext
import io.thecontext.ci.context.OutputContext
import io.thecontext.ci.context.ValidationContext
import io.thecontext.ci.input.InputFilesLocator
import io.thecontext.ci.validation.*
import java.io.File

fun main(args: Array<String>) {
    Runner(Context.Impl()).run(
            inputDirectory = File("/tmp/podcast-input"),
            rssFeedDirectory = File("/tmp/podcast-output"),
            showNotesDirectory = File("/tmp/podcast-output"),
            websiteDirectory = File("/tmp/podcast-website")
    )
}

class Runner(private val context: Context) {

    fun run(inputDirectory: File, showNotesDirectory: File, rssFeedDirectory: File, websiteDirectory: File) {
        val inputContext = InputContext.Impl(context)

        val inputFiles = inputContext.inputFilesLocator.locate(inputDirectory)
                .toObservable()
                .share()

        val input = inputFiles
                .ofType<InputFilesLocator.Result.Success>()
                .switchMapSingle {
                    inputContext.inputReader.read(it.people, it.podcast, it.episodes)
                }
                .share()

        val validation = input
                .switchMapSingle { inputResult ->
                    val context = ValidationContext.Impl(context, inputResult.people)

                    val podcast = context.podcastValidator.validate(inputResult.podcast)
                    val episodes = inputResult.episodes.map { context.episodeValidator.validate(it) }
                    val episodeList = context.episodesValidator.validate(inputResult.episodes)

                    Single.merge(episodes.plus(podcast).plus(episodeList)).toList().map { it.merge() }
                }

        val output = validation
                .ofType<ValidationResult.Success>()
                .withLatestFrom(input) { _, inputResult -> inputResult }
                .switchMapSingle {
                    val context = OutputContext.Impl(context)

                    context.outputWriter.write(
                            showNotesDirectory = showNotesDirectory,
                            rssFeedDirectory = rssFeedDirectory,
                            websiteDirectory = websiteDirectory,
                            people = it.people,
                            podcast = it.podcast,
                            episodes = it.episodes)
                }

        val resultSuccess = output.map { "Done!" }

        val resultError = Observable
                .merge(
                        inputFiles.ofType<InputFilesLocator.Result.Failure>().map { it.message },
                        validation.ofType<ValidationResult.Failure>().map { it.message }
                )

        println(Observable.merge(resultSuccess, resultError).blockingFirst())
    }
}