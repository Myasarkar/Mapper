/*
 * Copyright 2026 Mustafa Yasar Kar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bandmapper

import android.content.Context
import android.telephony.*
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.network.NetworkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Şebeke bilgilerini ve 5G bandını tespit eden sınıf.
 * NetMonster-core kütüphanesini kullanarak profesyonel tespit yapar.
 */
class NetworkMonitor(private val context: Context) {

    private val netMonster = NetMonsterFactory.get(context)
    private val subscriptionId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID

    // Bağlı olunan bandın bilgisini tutan akış
    private val _currentBand = MutableStateFlow<BandInfo>(BandInfo.Unknown)
    val currentBand: StateFlow<BandInfo> = _currentBand

    sealed class BandInfo {
        object Unknown : BandInfo()
        data class NR(val bandIndex: Int, val isSA: Boolean) : BandInfo()
        data class LTE(val pci: Int) : BandInfo()
    }

    fun startMonitoring() {
        // Periyodik güncelleme MainActivity'de yapılıyor
    }

    fun stopMonitoring() {
        // Temizlik işlemleri
    }

    private fun processCells(cells: List<ICell>) {
        // 1. Şebeke Tipini Al
        val networkType = netMonster.getNetworkType(subscriptionId)

        // 2. 5G NSA Kontrolü
        if (networkType is NetworkType.Nr.Nsa) {
            _currentBand.value = BandInfo.NR(78, false)
            return
        }

        // 3. 5G SA Kontrolü
        val nrCell = cells.filterIsInstance<CellNr>().firstOrNull { it.connectionStatus is PrimaryConnection }
        if (nrCell != null || networkType is NetworkType.Nr.Sa) {
            val band = (nrCell as? CellNr)?.band?.number ?: 78
            _currentBand.value = BandInfo.NR(band, true)
            return
        }

        // 4. Standart LTE Kontrolü
        val lteCell = cells.filterIsInstance<CellLte>().firstOrNull { it.connectionStatus is PrimaryConnection }
        if (lteCell != null) {
            _currentBand.value = BandInfo.LTE(lteCell.pci ?: 0)
            return
        }

        _currentBand.value = BandInfo.Unknown
    }

    fun updateNetworkInfo() {
        try {
            val cells = netMonster.getCells()
            processCells(cells)
        } catch (e: SecurityException) {
            // İzin hatası
        } catch (e: Exception) {
            _currentBand.value = BandInfo.Unknown
        }
    }
}
