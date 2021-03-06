package org.gotson.komga.domain.service

import mu.KotlinLogging
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.time.temporal.ChronoUnit
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

@Service
class LibraryScanner(
  private val fileSystemScanner: FileSystemScanner,
  private val seriesRepository: SeriesRepository,
  private val bookRepository: BookRepository,
  private val bookLifecycle: BookLifecycle,
  private val mediaRepository: MediaRepository,
  private val seriesLifecycle: SeriesLifecycle
) {

  fun scanRootFolder(library: Library) {
    logger.info { "Updating library: $library" }
    measureTime {
      val scannedSeries =
        fileSystemScanner.scanRootFolder(Paths.get(library.root.toURI()), library.scanForceModifiedTime)
          .map { (series, books) ->
            series.copy(libraryId = library.id) to books.map { it.copy(libraryId = library.id) }
          }.toMap()

      // delete series that don't exist anymore
      if (scannedSeries.isEmpty()) {
        logger.info { "Scan returned no series, deleting all existing series" }
        val seriesIds = seriesRepository.findByLibraryId(library.id).map { it.id }
        seriesLifecycle.deleteMany(seriesIds)
      } else {
        scannedSeries.keys.map { it.url }.let { urls ->
          val series = seriesRepository.findByLibraryIdAndUrlNotIn(library.id, urls)
          if (series.isNotEmpty()) {
            logger.info { "Deleting series not on disk anymore: $series" }
            seriesLifecycle.deleteMany(series.map { it.id })
          }
        }
      }

      scannedSeries.forEach { (newSeries, newBooks) ->
        val existingSeries = seriesRepository.findByLibraryIdAndUrl(library.id, newSeries.url)

        // if series does not exist, save it
        if (existingSeries == null) {
          logger.info { "Adding new series: $newSeries" }
          val createdSeries = seriesLifecycle.createSeries(newSeries)
          seriesLifecycle.addBooks(createdSeries, newBooks)
          seriesLifecycle.sortBooks(createdSeries)
        } else {
          // if series already exists, update it
          logger.debug { "Scanned series already exists. Scanned: $newSeries, Existing: $existingSeries" }
          val seriesChanged = newSeries.fileLastModified.truncatedTo(ChronoUnit.MILLIS) != existingSeries.fileLastModified.truncatedTo(ChronoUnit.MILLIS)
          if (seriesChanged) {
            logger.info { "Series changed on disk, updating: $existingSeries" }
            seriesRepository.update(existingSeries.copy(fileLastModified = newSeries.fileLastModified))
          }
          if (library.scanDeep || seriesChanged) {
            // update list of books with existing entities if they exist
            val existingBooks = bookRepository.findBySeriesId(existingSeries.id)
            logger.debug { "Existing books: $existingBooks" }
            // update existing books
            newBooks.forEach { newBook ->
              logger.debug { "Trying to match scanned book by url: $newBook" }
              existingBooks.find { it.url == newBook.url }?.let { existingBook ->
                logger.debug { "Matched existing book: $existingBook" }
                if (newBook.fileLastModified.truncatedTo(ChronoUnit.MILLIS) != existingBook.fileLastModified.truncatedTo(ChronoUnit.MILLIS)) {
                  logger.info { "Book changed on disk, update and reset media status: $existingBook" }
                  val updatedBook = existingBook.copy(
                    fileLastModified = newBook.fileLastModified,
                    fileSize = newBook.fileSize
                  )
                  mediaRepository.findById(existingBook.id).let {
                    mediaRepository.update(it.copy(status = Media.Status.OUTDATED))
                  }
                  bookRepository.update(updatedBook)
                }
              }
            }

            // remove books not present anymore
            val newBooksUrls = newBooks.map { it.url }
            existingBooks
              .filterNot { existingBook -> newBooksUrls.contains(existingBook.url) }
              .let { books ->
                logger.info { "Deleting books not on disk anymore: $books" }
                bookLifecycle.deleteMany(books.map { it.id })
              }

            // add new books
            val existingBooksUrls = existingBooks.map { it.url }
            val booksToAdd = newBooks.filterNot { newBook -> existingBooksUrls.contains(newBook.url) }
            logger.info { "Adding new books: $booksToAdd" }
            seriesLifecycle.addBooks(existingSeries, booksToAdd)

            // sort all books
            seriesLifecycle.sortBooks(existingSeries)
          }
        }
      }
    }.also { logger.info { "Library updated in $it" } }
  }

}
