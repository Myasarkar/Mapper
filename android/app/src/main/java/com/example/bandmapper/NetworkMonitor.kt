package com.example.bandmapper

import android.content.Context
import android.telephony.*
import android.os.Build
import androidx.annotation.RequiresApi
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import cz.mroczis.netmonster.core.model.nr.CellNr
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

    fun updateNetworkInfo() {
        try {
            // NetMonster ile tüm hücre bilgilerini al
            val cells = netMonster.getCells()
            
            // 1. Standalone (SA) 5G Kontrolü
            val nrCell = cells.filterIsInstance<CellNr>().firstOrNull { it.connectionStatus is PrimaryConnection }
            if (nrCell != null) {
                val band = nrCell.band?.number ?: 0
                _currentBand.value = BandInfo.NR(band, true)
                return
            }

            // 2. Non-Standalone (NSA) 5G Kontrolü
            // NetMonster, NSA durumunu cihazın "Physical Channel Config" veya "Service State" 
            // üzerinden otomatik olarak normalize eder.
            val isNsa = cells.any { it.connectionStatus is PrimaryConnection && it is cz.mroczis.netmonster.core.model.lte.CellLte && it.isNrAvailable }
            
            if (isNsa) {
                // NSA durumunda bandı tam olarak bilemeyebiliriz ama genellikle n78'dir.
                // NetMonster bazen NSA bandını da yakalayabilir.
                _currentBand.value = BandInfo.NR(78, false)
                return
            }

            // 3. LTE Kontrolü
            val lteCell = cells.filterIsInstance<cz.mroczis.netmonster.core.model.lte.CellLte>().firstOrNull { it.connectionStatus is PrimaryConnection }
            if (lteCell != null) {
                _currentBand.value = BandInfo.LTE(lteCell.pci ?: 0)
                return
            }

            _currentBand.value = BandInfo.Unknown

        } catch (e: SecurityException) {
            // İzin hatası
        } catch (e: Exception) {
            _currentBand.value = BandInfo.Unknown
        }
    }
}
