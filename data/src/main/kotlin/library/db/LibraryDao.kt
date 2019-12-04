/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.data.library.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.LibrarySorting
import tachiyomi.domain.library.model.MangaCategory
import tachiyomi.domain.manga.model.Chapter
import tachiyomi.domain.manga.model.Manga

@Dao
abstract class LibraryDao {

  fun subscribeAll(sort: LibrarySorting): Flow<List<LibraryManga>> {
    return subscribeAll(RawQueries.all(sort))
  }

  fun subscribeUncategorized(sort: LibrarySorting): Flow<List<LibraryManga>> {
    return subscribeCategory(RawQueries.uncategorized(sort))
  }

  fun subscribeCategory(categoryId: Long, sort: LibrarySorting): Flow<List<LibraryManga>> {
    return subscribeCategory(RawQueries.category(categoryId, sort))
  }

  suspend fun findAll(sort: LibrarySorting): List<LibraryManga> {
    return findAll(RawQueries.all(sort))
  }

  suspend fun findUncategorized(sort: LibrarySorting): List<LibraryManga> {
    return findCategory(RawQueries.uncategorized(sort))
  }

  suspend fun findCategory(categoryId: Long, sort: LibrarySorting): List<LibraryManga> {
    return findCategory(RawQueries.category(categoryId, sort))
  }

  @RawQuery(observedEntities = [Manga::class, Chapter::class])
  protected abstract fun subscribeAll(query: SupportSQLiteQuery): Flow<List<LibraryManga>>

  @RawQuery(observedEntities = [Manga::class, Chapter::class, MangaCategory::class])
  protected abstract fun subscribeCategory(query: SupportSQLiteQuery): Flow<List<LibraryManga>>

  @RawQuery(observedEntities = [Manga::class, Chapter::class])
  protected abstract suspend fun findAll(query: SupportSQLiteQuery): List<LibraryManga>

  @RawQuery(observedEntities = [Manga::class, Chapter::class, MangaCategory::class])
  protected abstract suspend fun findCategory(query: SupportSQLiteQuery): List<LibraryManga>

  @Query("SELECT sourceId FROM library GROUP BY sourceId ORDER BY COUNT(sourceId) DESC")
  abstract suspend fun findFavoriteSourceIds(): List<Long>

}

private object RawQueries {

  private const val defaultFields = "library.id, library.sourceId, library.key, library.title, " +
    "library.status, library.cover, library.lastUpdate"

  private const val defaultUnreadField = "COUNT(chapter.id) AS unread"

  private const val totalChaptersFields = "COUNT(chapter.id) AS total, " +
    "SUM(CASE WHEN chapter.read == 0 THEN 1 ELSE 0 END) AS unread"

  private val groupBy = "library.id"

  fun all(sort: LibrarySorting): SupportSQLiteQuery {
    return when (sort.type) {
      LibrarySort.TotalChapters -> defaultQueryWithTotalChapters()
      else -> defaultQuery()
    }
      .groupBy(groupBy)
      .orderBy(orderBy(sort))
      .create()
  }

  fun uncategorized(sort: LibrarySorting): SupportSQLiteQuery {
    return when (sort.type) {
      LibrarySort.TotalChapters -> defaultQueryWithTotalChapters()
      else -> defaultQuery()
    }
      .selection("NOT EXISTS (SELECT mangaCategory.mangaId FROM mangaCategory " +
        "WHERE manga.id = mangaCategory.id)", null)
      .groupBy(groupBy)
      .orderBy(orderBy(sort))
      .create()
  }

  fun category(categoryId: Long, sort: LibrarySorting): SupportSQLiteQuery {
    return when (sort.type) {
      LibrarySort.TotalChapters -> categoryWithTotalChapters()
      else -> categoryQuery()
    }
      .selection(null, arrayOf(categoryId))
      .groupBy(groupBy)
      .orderBy(orderBy(sort))
      .create()
  }

  private fun defaultQuery(): SupportSQLiteQueryBuilder {
    return SupportSQLiteQueryBuilder
      .builder("library LEFT JOIN chapter ON library.id = chapter.mangaId AND chapter.read = 0")
      .columns(arrayOf(defaultFields, defaultUnreadField))
  }

  private fun categoryQuery(): SupportSQLiteQueryBuilder {
    return SupportSQLiteQueryBuilder
      .builder("library JOIN mangaCategory ON library.id = mangaCategory.mangaId AND " +
        "mangaCategory.categoryId = ?1 LEFT JOIN chapter ON library.id = chapter.mangaId " +
        "AND chapter.read = 0")
      .columns(arrayOf(defaultFields, defaultUnreadField))
  }

  private fun defaultQueryWithTotalChapters(): SupportSQLiteQueryBuilder {
    return SupportSQLiteQueryBuilder
      .builder("library LEFT JOIN chapter ON library.id = chapter.mangaId")
      .columns(arrayOf(defaultFields, totalChaptersFields))
  }

  private fun categoryWithTotalChapters(): SupportSQLiteQueryBuilder {
    return SupportSQLiteQueryBuilder
      .builder("library JOIN mangaCategory ON library.id = mangaCategory.mangaId AND " +
        "mangaCategory.categoryId = ?1 LEFT JOIN chapter ON library.id = chapter.mangaId")
      .columns(arrayOf(defaultFields, totalChaptersFields))
  }

  private fun orderBy(sort: LibrarySorting): String {
    return when (sort.type) {
      LibrarySort.Title -> "library.title ${sort.dir}"
      LibrarySort.LastRead -> "" // TODO
      LibrarySort.LastUpdated -> "library.lastUpdate ${sort.dir}"
      LibrarySort.Unread -> "unread ${sort.dir}"
      LibrarySort.TotalChapters -> "total ${sort.dir}"
      LibrarySort.Source -> "library.sourceId ${sort.dir}, library.title ${sort.dir}"
    }
  }

  val LibrarySorting.dir get() = if (isAscending) "ASC" else "DESC"
}
