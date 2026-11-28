# Familien-Multiversum

Euer gemeinsamer Familienkalender – im gleichen Stil und mit dem gleichen Update-Weg wie TCG Multiversum.

## Aufbau
- `familie.html` – die komplette App (HTML/CSS/JS), läuft auch einfach im Browser zum Testen
- `assets/icon.svg` – App-Icon
- `.github/workflows/build-apk.yml` – baut bei jeder Änderung die Android-App (Capacitor),
  signiert sie immer mit dem geheimen Schlüssel aus dem GitHub-Secret `FAMILIE_KEYSTORE_B64`,
  veröffentlicht sie als Release
  und schreibt `app/version.json`
- `app/version.json` – daraus liest die App beim Start, ob es eine neue Version gibt

## Funktionen
- Monatsansicht mit farbigen Punkten pro Person, Wischen zum Monatswechsel
- Termine mit Uhrzeit oder ganztägig, für mich / meine Frau / beide
- Wiederholung: wöchentlich, monatlich, jährlich (z. B. Geburtstage)
- „Demnächst“-Liste und Übersicht oben
- Namen und Farben der Personen einstellbar
- Sicherung kopieren / einfügen
- Automatische Update-Prüfung wie bei TCG Multiversum
- Geburtstage & Jahrestage, „🔒 nur für mich“, Countdowns auf der Startseite
- Erinnerungen, mehrtägige Termine, automatische Sicherung (Dokumente/Familien-Multiversum)
- Feiertage & Schulferien NRW
- Essensplan mit Lieblingsgerichten → Zutaten auf die Einkaufsliste
- Listen-Vorlagen (Umzug, Urlaub packen, Wocheneinkauf, Hund)
- Pinnwand, Hunde-Ecke (Impfung, Wurmkur … mit Erinnerung), Müllabfuhr (Rhythmus oder .ics-Datei)

Die Termine liegen aktuell **nur auf dem jeweiligen Handy** (offline). Gemeinsamer Abgleich
zwischen beiden Handys kommt als nächster Schritt.

## Wichtig für Updates
Die App liest `app/version.json` und lädt die APK aus den Releases. Das funktioniert ohne
Anmeldung nur, wenn das Repo **öffentlich** ist (Settings → General → Danger Zone →
Change visibility). Im Repo liegen keine persönlichen Daten – eure Termine bleiben auf den Handys.
