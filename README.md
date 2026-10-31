# erkGPS

Ücretsiz, reklamsız, açık kaynaklı ve **root gerektirmeyen** bir Android konum simülasyonu / test uygulaması.
Android'in **resmî sahte konum (mock location)** mekanizmasını kullanır; geliştiricilerin ve test yapanların
kendi cihazlarında konuma bağlı uygulamaları denemesi için tasarlanmıştır.

> ⚠️ Yalnızca **kendi cihazınızda, test ve geliştirme amacıyla** kullanın. erkGPS, başka uygulamaların
> sahte konumu algılamasını atlatmaya çalışmaz ve kullanıcıdan habersiz hiçbir işlem yapmaz.
> Simülasyon açıkken bildirim çubuğunda görünür bir bildirim bulunur.

## Özellikler (v0.2.0)

- OpenStreetMap haritası (osmdroid), API anahtarı yok
- Haritaya dokunarak nokta seçme
- Enlem / boylamı gösterme ve elle girme (ondalık ayracı olarak nokta ya da virgül)
- Favori konumları kaydetme, seçme, silme (yalnızca cihazda saklanır)
- Simülasyonu başlatma / durdurma / simüle konumu güncelleme
- Ön plan hizmeti + bildirim ("Durdur" düğmesiyle)
- Türkçe kurulum rehberi (uygulama içinde)
- Modern Material 3 arayüzü: yüzen durum kartı, animasyonlu katlanabilir kontrol paneli, açık / koyu tema

## Gereksinimler

- Android 8.0 (API 26) veya üzeri
- Cihazda **Geliştirici seçenekleri** etkin olmalı
- erkGPS, "Sahte konum uygulamasını seç" ayarında seçili olmalı

## Kurulum ve kullanım

1. **Geliştirici seçeneklerini açın:** Ayarlar > Telefon hakkında > "Yapı numarası"na 7 kez dokunun.
2. Ayarlar > Sistem > **Geliştirici seçenekleri** (menü adı üreticiye göre değişebilir).
3. **"Sahte konum uygulamasını seç"** > **erkGPS**.
4. erkGPS'i açın, istenen **konum** ve **bildirim** izinlerini verin.
5. Haritadan nokta seçin veya enlem/boylam girin.
6. **"Simülasyonu başlat"**a dokunun. Bitince **"Simülasyonu durdur"** ya da bildirimdeki **"Durdur"**.

### İzinler neden isteniyor?

| İzin | Neden |
|------|-------|
| Konum (hassas / yaklaşık) | Android, konum türündeki ön plan hizmetinin çalışması için bu izni şart koşar |
| Bildirim (Android 13+) | Simülasyonun açık olduğunu görünür kılmak için |
| İnternet | Harita karolarını OpenStreetMap sunucusundan indirmek için |
| Ön plan hizmeti (konum) | Simülasyon uygulama arka plandayken de sürsün diye |
| ACCESS_MOCK_LOCATION | Sahte konum sağlayıcısı kaydı için bildirilir; asıl yetki geliştirici seçeneklerindeki seçimdir |

Bildirim izni verilmezse simülasyon yine çalışır, yalnızca bildirim görünmez.

## Sınırlamalar (dürüst notlar)

- Sahte konum sistem tarafından **"sahte" olarak işaretlenir**. Bazı uygulamalar bunu algılayıp reddedebilir;
  erkGPS bunu gizlemeye çalışmaz ve çalışmayacaktır.
- Sonuçlar cihaza, üreticinin Android sürümüne ve konuma bakan uygulamaya göre değişebilir.
- Uygulama henüz gerçek cihazlarda kapsamlı test edilmemiştir.
- Konum, sabit tek bir nokta olarak yayınlanır (rota / hız simülasyonu henüz yok).

## Derleme

### Android Studio
Projeyi açın, Gradle senkronizasyonunu bekleyin, **Run** ile çalıştırın.

### Komut satırı
JDK 17, Android SDK (platform 35 + build-tools 35.0.0) ve Gradle 8.9 gerekir:

```bash
gradle wrapper --gradle-version 8.9   # bir kez, gradlew dosyalarını üretir
./gradlew lintDebug testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (debug anahtarıyla imzalanmıştır, doğrudan kurulabilir).

### GitHub Actions
`.github/workflows/android.yml` her `push`'ta lint + birim testlerini çalıştırır ve debug APK'yı
**Actions > ilgili çalışma > Artifacts > erkgps-debug-apk** altına yükler.
`.github/workflows/release.yml` ise `v*` etiketinde (veya elle) derleyip APK'yı **Releases** bölümüne ekler.

## OpenStreetMap kullanım politikası

- Harita karoları `tile.openstreetmap.org` sunucusundan, tanımlayıcı bir `User-Agent` ile ve önbelleğe alınarak istenir.
- Haritada "© OpenStreetMap contributors" atfı gösterilir.
- Toplu karo indirme yapılmaz. Yoğun kullanım öngörüyorsanız kendi karo sunucunuzu veya
  uygun bir karo sağlayıcısını kullanın: <https://operations.osmfoundation.org/policies/tiles/>
- Çatallayanlar `app/src/main/java/com/ymcgps/geopilot/MainActivity.kt` içindeki `User-Agent` değerini
  kendi uygulama adı / iletişim bilgisiyle güncellemelidir.

## Gizlilik

erkGPS veri toplamaz, analiz / reklam SDK'sı içermez. Favoriler yalnızca cihazda saklanır.
Ağ trafiği yalnızca OpenStreetMap karo istekleridir.

## Proje yapısı

```
app/src/main/java/com/ymcgps/geopilot/
├── MainActivity.kt          Harita, giriş alanları, favoriler, başlat/durdur
├── data/                    Favori modeli ve yerel depolama
├── mock/                    Sahte konum motoru, ön plan hizmeti, durum
├── ui/SetupGuideActivity.kt Türkçe kurulum rehberi
└── util/                    Koordinat ayrıştırma/doğrulama, yardımcılar
```

Not: uygulama kimliği (`com.ymcgps.geopilot`) kurulu sürümlerin güncelleme alabilmesi için korunmuştur;
görünen ad ve marka erkGPS'tir.

## Yol haritası

- Rota ve hız simülasyonu
- Son kullanılan konumlar, favorileri dışa/içe aktarma
- Cihaz testleri (Espresso)

## Güvenlik

Gizli anahtar, token veya imzalama anahtarı depoya **eklenmemelidir** (`.gitignore` bunları dışlar).

## Lisans

MIT — bkz. [LICENSE](LICENSE).
