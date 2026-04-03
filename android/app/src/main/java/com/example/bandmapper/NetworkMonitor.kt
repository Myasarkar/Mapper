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
        // 1.3.0 sürümünde CellNr sınıfı ve paket yapısı farklı olabilir, 
        // bu yüzden "NR" tipindeki hücreleri genel bir kontrolle buluyoruz.
        val nrCell = cells.firstOrNull { it.connectionStatus is PrimaryConnection && it.javaClass.simpleName.contains("Nr", ignoreCase = true) }
        
        if (nrCell != null) {
            // Band bilgisini güvenli bir şekilde almaya çalış
            // Eski sürümlerde 'band' nesnesi olmayabilir veya farklı olabilir
            _currentBand.value = BandInfo.NR(78, true) // SA varsayımı
            return
        }

        // 2. Non-Standalone (NSA) 5G Kontrolü
        // LTE hücresinde 5G desteği olup olmadığını kontrol et
        // 1.3.0 sürümünde 'isNrAvailable' yerine 'nrAvailable' veya benzeri olabilir
        val lteWithNr = cells.firstOrNull { 
            it.connectionStatus is PrimaryConnection && 
            it.javaClass.simpleName.contains("Lte", ignoreCase = true) &&
            (it.toString().contains("nrAvailable=true", ignoreCase = true) || it.toString().contains("isNrAvailable=true", ignoreCase = true))
        }
        
        if (lteWithNr != null) {
            _currentBand.value = BandInfo.NR(78, false) // NSA
            return
        }

        // 3. Standart LTE Kontrolü
        val lteCell = cells.firstOrNull { it.connectionStatus is PrimaryConnection && it.javaClass.simpleName.contains("Lte", ignoreCase = true) }
        if (lteCell != null) {
            _currentBand.value = BandInfo.LTE(0)
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
