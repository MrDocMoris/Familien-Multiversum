# Familien-Multiversum

Gemeinsamer Kalender für zwei (oder mehr) Personen – Android, Kotlin, Jetpack Compose, Firebase.

**Funktionen (v0.1)**
- Registrieren / Anmelden per E-Mail
- Familie anlegen, Partner tritt per 6-stelligem Einladungscode bei
- Monatsansicht mit Punkten an Tagen mit Terminen
- Termine anlegen, bearbeiten, löschen (Titel, Datum, Uhrzeit oder ganztägig, Notiz)
- Änderungen erscheinen live auf beiden Handys

## Einrichtung (ca. 15 Minuten)

### 1. Firebase-Projekt anlegen
1. <https://console.firebase.google.com> → **Projekt hinzufügen** (Google Analytics kannst du abschalten).
2. **Android-App hinzufügen**, Paketname: `de.mrdocmoris.familienkalender`
3. `google-services.json` herunterladen und in den Ordner **`app/`** legen.
   (Die Datei steht in `.gitignore` und kommt nicht ins Repo.)

### 2. Login aktivieren
Firebase-Konsole → **Authentication** → Jetzt starten → Anmeldemethode **E-Mail/Passwort** aktivieren.

### 3. Datenbank anlegen
1. **Firestore Database** → Datenbank erstellen → Standort `eur3 (europe-west)` → Produktionsmodus.
2. Reiter **Regeln**: Inhalt von `firestore.rules` einfügen → **Veröffentlichen**.

### 4. App starten
1. Ordner in **Android Studio** öffnen (Gradle-Sync läuft automatisch).
2. Handy per USB verbinden (USB-Debugging an) → ▶ Run.
3. Für deine Frau: *Build → Build APK(s)* und die APK auf ihr Handy schicken,
   oder ihr Handy ebenfalls per USB anschließen.

### 5. Benutzen
1. Du registrierst dich → **Familie anlegen**.
2. Menü (⋮) → **Einladungscode anzeigen**.
3. Deine Frau registriert sich → gibt den Code bei **Beitreten** ein. Fertig.

## Projektstruktur
```
app/src/main/java/de/mrdocmoris/familienkalender/
├── MainActivity.kt          Einstieg, Theme
├── data/
│   ├── Models.kt            UserProfile, Family, CalendarEvent
│   └── Repository.kt        alle Firebase-Zugriffe
└── ui/
    ├── MainViewModel.kt     Zustand + Aktionen
    ├── App.kt               Navigation je nach Zustand
    ├── AuthScreen.kt        Login / Registrierung
    ├── FamilyScreen.kt      Familie anlegen / beitreten
    ├── CalendarScreen.kt    Monatsansicht + Tagesliste
    └── EventDialog.kt       Termin anlegen / bearbeiten
```

## Ideen für später
- Mehrtägige Termine und Wiederholungen (wöchentlich, jährlich für Geburtstage)
- Farben pro Person
- Erinnerungen / Push-Benachrichtigungen (Firebase Cloud Messaging)
- Wochenansicht, Widget für den Startbildschirm
- Einkaufsliste und To-dos als weitere Tabs
