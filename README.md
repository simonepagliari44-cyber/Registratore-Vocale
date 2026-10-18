<div align="center">

# 🎙️ **Registratore Vocale** 🚀
<img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" alt="Icona Registratore Vocale" width="120" height="120" />

### *App Android per la Registrazione Audio con Interfaccia Moderna*

---

</div>

## 📌 **Panoramica del Progetto**

Il progetto **Registratore Vocale** nasce con l'obiettivo di offrire un'esperienza utente fluida, moderna ed efficiente per la registrazione audio su dispositivi Android.

L'app sfrutta le potenzialità di **Jetpack Compose** per garantire un'interfaccia reattiva e performante, con un'architettura pulita basata su Material Design 3.

---

## ✨ **Caratteristiche Principali**

* 🎤 **Registrazione Audio di Alta Qualità:** Formato AAC a 128 kbps e 44.1 Hz per un eccellente rapporto qualità dimensione.
* 🎨 **Interfaccia Utente Moderna:** Design reattivo, pulito e intuitivo basato su Material Design 3 e Jetpack Compose.
* 🌊 **Visualizzazione Animata dell'Onda:** Animazione in tempo reale che risponde alla voce durante la registrazione.
* 🎵 **Player Integrato:** Bottom sheet con controlli di riproduzione per ascoltare le registrazioni salvate.
* 📋 **Lista Registrazioni:** Elenco completo delle registrazioni con supporto allo swipe per eliminare.
* 💾 **Salvataggio Automatico:** Le registrazioni vengono salvate automaticamente nel MediaStore del dispositivo.
* 🔄 **Gestione Stato Robusta:** Architettura MVVM con ViewModel e StateFlow per una gestione impeccabile dello stato.

---

## 🛠️ **Stack Tecnologico**

| Tecnologia | Descrizione |
|------------|-------------|
| **Linguaggio** | Kotlin 2.2.21 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architettura** | MVVM (Model-View-ViewModel) |
| **Min SDK** | Android 7.0 (API 24) |
| **Target SDK** | Android 14 (API 34) |
| **Build System** | Gradle 8.14.3 (Kotlin DSL) |

---

## 📁 **Struttura del Progetto**

```
app/src/main/java/com/simonecompany/registratorevocale/
├── data/
│   ├── AudioRecorder.kt       # Logica di registrazione audio
│   ├── RecordingRepository.kt # Repository per la gestione delle registrazioni
│   └── StorageHelper.kt       # Helper per la gestione dello storage
├── model/
│   └── Recording.kt           # Data class per le registrazioni
├── ui/
│   ├── ListScreen.kt          # Schermata lista registrazioni
│   ├── PlayerBottomSheet.kt   # Bottom sheet per la riproduzione
│   ├── RecordScreen.kt        # Schermata di registrazione
│   └── theme/                 # Tema Material Design 3
├── MainActivity.kt            # Activity principale
└── MainViewModel.kt           # ViewModel principale
```

---

## 🚀 **Come Compilare**

1. Clona il repository:
   ```bash
   git clone https://github.com/tuo-utente/registratore-vocale.git
   ```

2. Apri il progetto in **Android Studio** (Koala o superiore).

3. Sincronizza il progetto Gradle e avvia la build.

4. Installa l'APK su un dispositivo o emulatore con API 24+.

---

