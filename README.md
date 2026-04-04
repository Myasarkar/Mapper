# 🛰️ 5G Band Mapper

[![Android Build](https://github.com/Myasarkar/Mapper/actions/workflows/main.yml/badge.svg)](https://github.com/Myasarkar/Mapper/actions/workflows/main.yml)

**5G Band Mapper**, Android cihazlarda şebeke bilgilerini (özellikle 5G n78 bandını) gerçek zamanlı olarak takip eden, harita üzerinde görselleştiren ve verileri yerel bir veritabanında saklayan profesyonel bir araçtır.

## ✨ Özellikler

- **🎯 Gerçek Zamanlı 5G Tespiti:** n78 (3500MHz) ve diğer 5G bantlarını otomatik olarak tespit eder.
- **🗺️ İnteraktif Harita:** OpenStreetMap (Osmdroid) altyapısı ile internet bağlantısı olan her yerde çalışır (Google Maps API anahtarı gerektirmez).
- **📊 Renkli Göstergeler:** Şebeke türüne göre (5G SA, 5G NSA, LTE) harita üzerinde farklı renklerde noktalar bırakır.
- **💾 Yerel Veritabanı:** Room DB kullanarak tüm ölçümlerinizi telefonunuzda saklar ve uygulama açıldığında geçmişi yükler.
- **🚫 Ekran Kilidi Engelleme:** Haritalama aktifken ekranın kapanmasını önler.
- **📏 Akıllı Nokta Yönetimi:** Aynı konumda (20m çapında) mükerrer kayıt oluşturmaz, mevcut veriyi günceller.

## 🛠️ Kullanılan Teknolojiler

- **Dil:** Kotlin
- **UI:** Jetpack Compose & Material 3
- **Harita:** Osmdroid (OpenStreetMap)
- **Şebeke İzleme:** NetMonster Core & Android Telephony API
- **Veritabanı:** Room Persistence Library
- **CI/CD:** GitHub Actions (Otomatik APK Üretimi)

## 📲 Uygulamayı İndir

Uygulamanın en güncel sürümünü aşağıdaki bağlantıdan doğrudan indirebilirsiniz:

[**📥 5G Band Mapper APK İndir**](https://github.com/Myasarkar/Mapper/releases/download/5G-Band-Mapper/5G.Band.Mapper.apk)

## 📸 Ekran Görüntüleri
*(Buraya uygulamanın ekran görüntülerini ekleyebilirsiniz)*

## 📄 Lisans
Bu proje **Apache License 2.0** ile lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakabilirsiniz.

---
**Geliştirici:** [Mustafa Yaşar Kar](https://github.com/Myasarkar)
