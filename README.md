# Impostra

Impostra, siber güvenlik temalı, çok oyunculu bir sosyal çıkarım oyunudur: oyuncular gizli rollerle (iyi sistem kullanıcıları ve kötü Rogue AI takımı) gece–gündüz turlarında oynar; gece Rogue AI bir oyuncuyu sistemden siler, gündüz herkes tartışıp şüpheliye oy verir. Oyun bir sunucu ile JavaFX arayüzlü istemciler arasında KryoNet üzerinden ağ üzerinden oynanır. Bir okul OOP projesi olarak geliştirilmiştir.

## Oyun

- Oyun 2–14 oyuncu ile başlatılabilir; oyuncu sayısına göre roller dağıtılır.
- **Gece:** Rogue AI bir oyuncuyu hedefler, Güvenlik Mühendisi bir oyuncuyu korur (aynı oyuncu seçilirse saldırı engellenir).
- **Gündüz:** Oyuncular tartışır, ardından oylama yapılır; en çok oyu alan oyuncu elenir (beraberlikte kimse elenmez).
- **Kazanma:** Tüm kötüler elenirse iyiler, kötü sayısı iyi sayısına eşit veya fazla olursa kötüler kazanır.

| Rol | Takım |
| --- | --- |
| Rogue AI | Kötü |
| İç Tehdit | Kötü |
| Güvenlik Mühendisi, Siber Analist, Root Yöneticisi, Log Okuyucu, Uyuyan Bot, Senkronize Düğüm, Kullanıcı | İyi |

## Teknolojiler

- Java 25 ve Apache Maven
- JavaFX 21 (istemci arayüzü)
- KryoNet 2.22.0-RC1 (sunucu–istemci ağ iletişimi)
- JUnit 5 (testler)

## Gereksinimler

- JDK 25 (`pom.xml` içinde `maven.compiler.source/target` = 25)
- Apache Maven 3.x
- İstemci arayüzü için grafik arayüz destekleyen bir ortam (JavaFX)

## Kurulum

```bash
git clone https://github.com/barissurkit/impostra.git
cd impostra
mvn compile
```

Bağımlılıklar (KryoNet, JavaFX, JUnit) `pom.xml` üzerinden Maven tarafından indirilir.

## Kullanım

Önce sunucuyu başlatın:

```bash
mvn exec:java -Dexec.mainClass=com.impostra.server.ServerApp
```

Beklenen çıktı (sunucu 54555 TCP / 54777 UDP portlarını dinler):

```text
--- Impostra Ana Sunucusu Başlatılıyor ---
INFO: [kryonet] Server opened.
[BAŞARILI] Sunucu 54555 portundan dinliyor.
```

Ardından her oyuncu için ayrı bir terminalde JavaFX istemcisini açın:

```bash
mvn exec:java -Dexec.mainClass=com.impostra.client.ui.Launcher
```

İstemci, `127.0.0.1:54555` adresindeki sunucuya bağlanır (adres şu an kaynak kodda sabittir: `ImpostraGUI.java`).

## Testler

```bash
mvn test
```

Testler `tests/` dizinindedir (`pom.xml` içinde `testSourceDirectory` olarak ayarlıdır) ve oyun mantığını (`GameManager`: rol dağıtımı, gece saldırısı/koruma, oylama, kazanma koşulları) ile `Player`/rol sınıflarını doğrular.

## Katkı

Katkı rehberi için [CONTRIBUTING.md](CONTRIBUTING.md) dosyasına bakın.

## Lisans

[MIT](LICENSE)
