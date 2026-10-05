# Katkı Rehberi

Katkıda bulunmak istediğiniz için teşekkürler!

## Kurulum

1. Repository'yi fork'layıp klonlayın.
2. JDK 25 ve Apache Maven kurulu olmalıdır.
3. Projeyi derleyin: `mvn compile`

## Testleri çalıştırma

```bash
mvn test
```

Testler `tests/` dizinindedir. Yeni bir davranış eklerseniz veya mevcut bir davranışı değiştirirseniz ilgili testi de ekleyin/güncelleyin.

## Pull request beklentileri

- `main` dalına doğrudan push yapmayın; ayrı bir dal açıp pull request gönderin.
- Pull request'i tek bir konuya odaklı tutun ve ne değiştiğini kısaca açıklayın.
- `mvn test` komutu yerelde geçmeli; GitHub Actions iş akışı (CI) yeşil olmalıdır.
- Gizli bilgi (anahtar, parola vb.) eklemeyin.
