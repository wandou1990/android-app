/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.font
import androidx.compose.ui.text.font.fontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.ui.R
import tachiyomi.ui.Route
import tachiyomi.ui.core.coil.MangaCover
import tachiyomi.ui.core.components.AutofitGrid
import tachiyomi.ui.core.components.Toolbar
import tachiyomi.ui.core.components.manga.MangaGridItem
import tachiyomi.ui.core.viewmodel.viewModel

val ptSansFont = fontFamily(font(R.font.ptsans_bold))

@Composable
fun LibraryScreen(navController: NavController) {
  val vm = viewModel<LibraryViewModel>()

  Column {
    Toolbar(title = { Text(stringResource(R.string.library_label)) })
    Box(Modifier.padding(2.dp)) {
      LibraryTable(
        vm.library,
        onClickManga = { navController.navigate("${Route.LibraryManga.id}/${it.id}") },
      )
    }
  }
}

@Composable
private fun LibraryTable(
  library: List<LibraryManga>,
  onClickManga: (LibraryManga) -> Unit = {}
) {
  AutofitGrid(data = library, defaultColumnWidth = 160.dp) { manga ->
    MangaGridItem(
      title = manga.title,
      cover = MangaCover.from(manga),
      onClick = { onClickManga(manga) }
    )
  }
}
