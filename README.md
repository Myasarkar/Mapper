# 5G n78 Band Mapper - Android Projesi

Bu proje, Android cihazlarda **5G n78 (3500MHz)** bandını tespit eden ve bu verileri **OpenStreetMap (Osmdroid)** üzerinde görselleştiren profesyonel bir mobil uygulama altyapısıdır.

## 🚀 Kurulum ve Çalıştırma

### 1. Harita Yapılandırması
Bu uygulama **OpenStreetMap** kullandığı için herhangi bir **Google Maps API anahtarına ihtiyaç duymaz.** Kurulum gerektirmeden doğrudan çalıştırılabilir.

### 2. APK Alma
Projeyi GitHub'a yükleyin. GitHub Actions otomatik olarak APK üretecektir:
1. Kodu GitHub reponuza push edin
2. "Actions" sekmesine gidin
3. Build tamamlandığında "app-debug" artifact'ini indirin

## 🛠️ Proje Özellikleri
- **OpenStreetMap (Osmdroid):** API anahtarı gerektirmeyen, tamamen açık kaynaklı harita altyapısı.
- **Jetpack Compose & Material 3:** Modern ve hızlı kullanıcı arayüzü.
- **5G n78 Tespiti:** n78 bandına bağlıyken ekranda büyük yeşil gösterge çıkar.
- **Harita Takibi:** Her 5 saniyede bir o anki şebeke türüne göre haritaya nokta ekler.
- **Arka Plan Servisi:** Uygulama kapalıyken dahi veri toplamaya devam eder (Foreground Service).

## 📦 APK Üretimi
Bu proje GitHub Actions ile entegre edilmiştir. Projeyi GitHub deponuza yüklediğinizde otomatik olarak bir hata ayıklama (debug) APK'sı üretilir ve "Actions" sekmesinde yayınlanır. Artık SHA-1 koduyla uğraşmanıza gerek yoktur.

---
**Geliştirici:** Mustafa Yaşar Kar
**Teknoloji:** Kotlin, Jetpack Compose, Android Telephony API
