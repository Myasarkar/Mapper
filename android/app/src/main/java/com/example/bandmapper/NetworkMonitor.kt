package com.example.bandmapper

import android.content.Context
import android.telephony.*
import android.os.Build
import androidx.annotation.RequiresApi
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import cz.mroczis.netmonster.core.model.nr.CellNr
import cz.mroczis.netmonster.core.model.lte.CellLte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Şebeke bilgilerini ve 5G bandını tespit eden sınıf.
 * NetMonster-core kütüphanesini kullanarak profesyonel tespit yapar.
 */
class NetworkMonitor(private val context: Context) {

    private val netMonster = NetMonsterFactory.get(context)

    // Bağlı olunan bandın bilgisini tutan akış
    private val _currentBand = MutableStateFlow<BandInfo>(BandInfo.Unknown)
    val currentBand: StateFlow<BandInfo> = _currentBand

    sealed class BandInfo {
        object Unknown : BandInfo()
        data class NR(val bandIndex: Int, val isSA: Boolean) : BandInfo()
        data class LTE(val pci: Int) : BandInfo()
    }

    private var listener: cz.mroczis.netmonster.core.INetMonster.ISubscription? = null

    fun startMonitoring() {
        listener = netMonster.subscribe(object : cz.mroczis.netmonster.core.INetMonster.IOnCellsUpdatedListener {
            override fun onCellsUpdated(cells: List<ICell>) {
                processCells(cells)
            }
        })
    }

    fun stopMonitoring() {
        listener?.destroy()
        listener = null
    }

    private fun processCells(cells: List<ICell>) {
        // 1. Standalone (SA) 5G Kontrolü
        val nrCell = cells.filterIsInstance<CellNr>().firstOrNull { it.connectionStatus is PrimaryConnection }
        if (nrCell != null) {
            val band = nrCell.band?.number ?: 0
            _currentBand.value = BandInfo.NR(band, true)
            return
        }

        // 2. Non-Standalone (NSA) 5G Kontrolü
        // LTE hücresinde 5G (NR) desteği olup olmadığını kontrol et
        val lteWithNr = cells.filterIsInstance<CellLte>().firstOrNull { 
            it.connectionStatus is PrimaryConnection && it.isNrAvailable 
        }
        
        if (lteWithNr != null) {
            // NSA durumunda genellikle n78 bandı kullanılır
            _currentBand.value = BandInfo.NR(78, false)
            return
        }

        // 3. Standart LTE Kontrolü
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
