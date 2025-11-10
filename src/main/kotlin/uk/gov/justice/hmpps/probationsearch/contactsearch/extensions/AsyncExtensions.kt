package uk.gov.justice.hmpps.probationsearch.contactsearch.extensions

import org.springframework.core.task.SimpleAsyncTaskExecutor
import java.util.concurrent.CompletableFuture

object AsyncExtensions {
  fun SimpleAsyncTaskExecutor.runAsync(fn: () -> Unit): CompletableFuture<Void> = CompletableFuture.runAsync(fn, this)
  fun <T> SimpleAsyncTaskExecutor.supplyAsync(fn: () -> T): CompletableFuture<T> =
    CompletableFuture.supplyAsync(fn, this)
}