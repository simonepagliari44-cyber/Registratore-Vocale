<div align="center">

# 📋 **Release Notes** 📦

### *Registro delle Versioni — Registratore Vocale*

---

</div>

## 🚀 **Versione 1.0.0** — *18 Ottobre 2026*

> **Prima release pubblica** — Registratore Vocale v1.0

---

### ✨ **Funzionalità Iniziali**

* 🎤 **Registrazione Audio in Formato M4A/AAC**
  * Bitrate: 128 kbps
  * Frequenza di campionamento: 44.1 Hz
  * Supporto nativo MediaStore per Android 10+

* 🎨 **Interfaccia Material Design 3**
  * Tema chiaro/scuro automatico
  * Animazione onda audio in tempo reale
  * UI reattiva con Jetpack Compose

* 🎵 **Player Integrato**
  * Bottom sheet con controlli di riproduzione
  * Seek bar interattiva
  * Durata totale della registrazione visualizzata

* 📋 **Gestione Registrazioni**
  * Lista cronologica delle registrazioni
  * Swipe-to-delete con conferma
  * Data e ora di creazione per ogni file

* 🗂️ **Architettura MVVM**
  * ViewModel con StateFlow
  * Repository pattern per l'accesso ai dati
  * Separazione netta tra logica e UI

---

### 📦 **Dettagli Tecnici**

| Proprietà | Valore |
|-----------|--------|
| **Version Code** | 1 |
| **Version Name** | 1.0 |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 34 (Android 14) |
| **Kotlin** | 2.2.21 |
| **Compose BOM** | 2024.09.03 |

---

### 🔒 **Permessi Richiesti**

* `RECORD_AUDIO` — Necessario per la registrazione audio dal microfono.

---
