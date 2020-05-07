/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.catalog.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tachiyomi.domain.catalog.model.CatalogInstalled
import tachiyomi.domain.catalog.model.CatalogLocal
import tachiyomi.domain.catalog.model.CatalogRemote
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogStore @Inject constructor(
  private val loader: CatalogLoader,
  catalogRemoteRepository: CatalogRemoteRepository,
  installationReceiver: CatalogInstallationReceiver
) : CatalogInstallationReceiver.Listener {

  var catalogs = emptyList<CatalogLocal>()
    private set(value) {
      field = value.withUpdateCheck()
      updatableCatalogs = field.filterIsInstance<CatalogInstalled>().filter { it.hasUpdate }
      catalogsBySource = field.associateBy { it.sourceId }
      catalogsChannel.offer(field)
    }

  private var updatableCatalogs = emptyList<CatalogInstalled>()

  private var remoteCatalogs = emptyList<CatalogRemote>()

  private var catalogsBySource = emptyMap<Long, CatalogLocal>()

  private val catalogsChannel = ConflatedBroadcastChannel(catalogs)

  init {
    catalogs = loader.loadAll()
    installationReceiver.register(this)

    catalogRemoteRepository.getRemoteCatalogsFlow()
      .onEach {
        remoteCatalogs = it
        synchronized(this@CatalogStore) {
          catalogs = catalogs // Force an update check
        }
      }
      .launchIn(GlobalScope)
  }

  fun get(sourceId: Long): CatalogLocal? {
    return catalogsBySource[sourceId]
  }

  fun getCatalogsFlow(): Flow<List<CatalogLocal>> {
    return catalogsChannel.asFlow()
  }

  private fun List<CatalogLocal>.withUpdateCheck(): List<CatalogLocal> {
    val catalogs = toMutableList()
    val remoteCatalogs = remoteCatalogs
    for ((index, installedCatalog) in catalogs.withIndex()) {
      if (installedCatalog !is CatalogInstalled) continue

      val pkgName = installedCatalog.pkgName
      val remoteCatalog = remoteCatalogs.find { it.pkgName == pkgName } ?: continue

      val hasUpdate = remoteCatalog.versionCode > installedCatalog.versionCode
      if (installedCatalog.hasUpdate != hasUpdate) {
        catalogs[index] = installedCatalog.copy(hasUpdate = hasUpdate)
      }
    }
    return catalogs
  }

  override fun onInstalled(pkgName: String) {
    GlobalScope.launch(Dispatchers.Default) {
      val catalog = loader.load(pkgName) as? CatalogInstalled ?: return@launch

      synchronized(this@CatalogStore) {
        val mutInstalledCatalogs = catalogs.toMutableList()
        val oldCatalog = mutInstalledCatalogs.find {
          (it as? CatalogInstalled)?.pkgName == catalog.pkgName
        }
        if (oldCatalog != null) {
          mutInstalledCatalogs -= oldCatalog
        }
        mutInstalledCatalogs += catalog
        catalogs = mutInstalledCatalogs
      }
    }
  }

  override fun onUninstalled(pkgName: String) {
    GlobalScope.launch(Dispatchers.Default) {
      synchronized(this@CatalogStore) {
        val installedCatalog = catalogs.find { (it as? CatalogInstalled)?.pkgName == pkgName }
        if (installedCatalog != null) {
          catalogs = catalogs - installedCatalog
        }
      }
    }
  }

}