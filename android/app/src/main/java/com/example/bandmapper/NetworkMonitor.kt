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
            
            if (!allCellInfo.isNullOrEmpty()) {
                for (info in allCellInfo) {
                    if (info is CellInfoNr && info.isRegistered) {
                        // 5G NR tespiti (Standalone)
                        val nr = info.cellIdentity as CellIdentityNr
                        val band = getBandFromArfcn(nr.nrarfcn)
                        val isSA = info.cellConnectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING
                        _currentBand.value = BandInfo.NR(band, isSA)
                        return
                    }
                }
            }

            // Eğer CellInfoNr bulunamadıysa NSA (Non-Standalone) kontrolü yap
            checkNsaStatus()

        } catch (e: SecurityException) {
            // İzin hatası
        }
    }

    private fun checkNsaStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val displayInfo = telephonyManager.telephonyDisplayInfo
                val overrideType = displayInfo.overrideNetworkType
                
                if (overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA || 
                    overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED) {
                    // Cihaz 5G NSA (Non-Standalone) modunda
                    _currentBand.value = BandInfo.NR(78, false) 
                    return
                }
            } catch (e: Exception) {
                // Bazı cihazlarda veya durumlarda hata verebilir
            }
        }
        
        // Hiçbiri değilse LTE veya Bilinmiyor
        _currentBand.value = BandInfo.LTE(0)
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
