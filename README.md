# HydroHomie

Erinnerungs-App, die dich ans Trinken erinnert. Du stellst ein Tagesziel, eine
Glasgröße und ein Trinkzeitfenster ein – die App erinnert dich in diesem Zeitfenster an
den Rest und zählt mit, was du getrunken hast.

## Funktionen

- **Tagesziel** (in ml) und **Standard-Glasgröße** (in ml) einstellbar
- **Trinkzeitfenster** (Beginn/Ende) – die Erinnerungen werden gleichmäßig verteilt
- **Fortschrittsanzeige** als Ring auf dem Hauptbildschirm
- **Trinken** und **Rückgängig** (nimmt wirklich das letzte Glas weg)
- **Erinnerungen** als Benachrichtigung mit Ton; ein Klick zählt ein Glas – **ohne dass
  sich die App öffnet**
- **Automatische Neuberechnung** der Erinnerungen: nach jedem Glas, nach „Rückgängig“
  und nach jeder Änderung der Einstellungen
- Erinnerungen werden nach einem **Neustart des Geräts** oder einem **Update** neu geplant
- **Deutsch/Englisch** umschaltbar (die Oberfläche wird komplett übersetzt)

## Architektur

```
com.example.hydrohomie
├── domain/          reines Kotlin, ohne Android-Abhängigkeiten
│   ├── DrinkSettings         die Einstellungen
│   ├── SettingsValidator     Validierung der Eingaben
│   ├── DrinkHistory          die einzelnen Gläser (mit Rückgängig)
│   ├── DrinkProgress         Fortschritt zum Tagesziel
│   └── NotificationPlanner   berechnet, wann erinnert wird
├── data/            Persistenz
│   ├── JsonFile              atomisches Schreiben von JSON
│   ├── SettingsStore         settings.json (mit Migration der alten Datei)
│   └── HistoryStore          history.json (mit Migration der alten Datei)
├── notifications/
│   ├── ReminderCoordinator    plant und plant neu
│   ├── NotificationFactory   baut die Benachrichtigungen
│   ├── AlarmScheduler        AlarmManager
│   ├── ReminderReceiver      zeigt die Benachrichtigung, zählt beim Klick
│   └── BootReceiver          plant nach Neustart/Update neu
├── ui/              ViewModels mit StateFlow
└── MainActivity, SettingsActivity, WaterProgressView
```

Die Logik liegt bewusst in reinen Klassen, damit sie ohne Android getestet werden kann.
Die alten Dateien `settings.json` und `history.json` werden automatisch weiterverwendet,
die Einstellungen und der Trinkfortschritt gehen beim Update also **nicht** verloren.

## Bauen und Testen

```bash
./gradlew :app:assembleDebug        # App bauen
./gradlew :app:testDebugUnitTest    # Unit-Tests (JVM + Robolectric)
```

Die Tests decken die Planung der Erinnerungen, die Validierung, die Speicherung inkl.
Migration der alten Dateien, die ViewModels und die Benachrichtigungen ab.
