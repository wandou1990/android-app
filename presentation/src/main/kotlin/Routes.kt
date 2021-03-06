/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui

sealed class Route(val id: String) {
  object Library : Route("library")
  object LibraryManga : Route("library/manga")

  object Updates : Route("updates")

  object History : Route("history")

  object Browse : Route("browse")
  object BrowseCatalog : Route("browse/catalog")
  object BrowseCatalogManga : Route("browse/catalog/manga")

  object Categories : Route("categories")

  object More : Route("more")

  object Settings : Route("settings")
  object SettingsGeneral : Route("settings/general")
  object SettingsAppearance : Route("settings/appearance")
  object SettingsLibrary : Route("settings/library")
  object SettingsReader : Route("settings/reader")
  object SettingsDownloads : Route("settings/downloads")
  object SettingsTracking : Route("settings/tracking")
  object SettingsBrowse : Route("settings/browse")
  object SettingsBackup : Route("settings/backup")
  object SettingsSecurity : Route("settings/security")
  object SettingsParentalControls : Route("settings/parentalControls")
  object SettingsAdvanced : Route("settings/advanced")
}
