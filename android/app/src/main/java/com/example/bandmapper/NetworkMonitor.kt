package com.example.bandmapper

import android.content.Context
import android.telephony.*
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Şebeke bilgilerini ve 5G bandını tespit eden sınıf.
 */
class NetworkMonitor(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    // Bağlı olunan bandın bilgisini tutan akış
    private val _currentBand = MutableStateFlow<BandInfo>(BandInfo.Unknown)
    val currentBand: StateFlow<BandInfo> = _currentBand

    sealed class BandInfo {
        object Unknown : BandInfo()
        data class NR(val bandIndex: Int, val isSA: Boolean) : BandInfo()
        data class LTE(val pci: Int) : BandInfo()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun updateNetworkInfo() {
        try {
            // Tüm hücre bilgilerini al
            val allCellInfo = telephonyManager.allCellInfo
            if (allCellInfo.isNullOrEmpty()) return

            for (info in allCellInfo) {
                if (info is CellInfoNr && info.isRegistered) {
                    // 5G NR tespiti
                    val nr = info.cellIdentity as CellIdentityNr
                    val band = getBandFromArfcn(nr.nrarfcn)
                    
                    // SA (Standalone) kontrolü
                    val isSA = info.cellConnectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING
                    
                    _currentBand.value = BandInfo.NR(band, isSA)
                    return
                }
            }

            // Eğer 5G bulunamadıysa LTE kontrolü (NSA durumu için)
            // Not: NSA durumunda ServiceState üzerinden NR durumu kontrol edilmelidir.
            checkNsaStatus()

        } catch (e: SecurityException) {
            // İzin hatası
        }
    }

    private fun checkNsaStatus() {
        // NSA durumunda cihaz LTE'ye bağlıdır ancak NR (5G) sinyali de alır.
        // Bu detaylı tespit için ServiceState.getNrState() kullanılır.
    }

    /**
     * NR-ARFCN değerinden band numarasını hesaplayan yardımcı fonksiyon.
     * n78: 620000 - 653333 arası (yaklaşık)
     */
    private fun getBandFromArfcn(arfcn: Int): Int {
        return when {
            arfcn in 620000..653333 -> 78
            arfcn in 151600..160600 -> 28
            else -> 0 // Diğer bantlar için genişletilebilir
        }
    }
}
