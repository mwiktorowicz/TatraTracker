# TatraTracker – plan rozwoju i przekazanie kontekstu

Stan na 2026-11-13. Dokument powstał po przeglądzie całego kodu (ok. 34 tys. linii Kotlina, 80 plików).
Jest źródłem prawdy dla pracy w terminalu: zanim zaczniesz zadanie, przeczytaj sekcję „Zasady pracy" i sekcję danego zadania.

## Zasady pracy

Role:

- **Claude w aplikacji Claude (czat)** – planuje, ustala kolejność, robi przegląd zmian, aktualizuje ten plik.
- **Claude w terminalu Android Studio** – wykonawca: pisze kod, buduje projekt, poprawia błędy kompilacji.
- **Miłosz** – łącznik między oboma, tester na telefonie, współdecydujący. Sam robi commity, gałęzie i push.

Reguły dla wykonawcy:

1. **Nie rób commitów, pushów ani zmian gałęzi.** Po skończeniu zadania wypisz zmienione pliki i zaproponuj treść commita; commit robi Miłosz.
2. Rób jedno zadanie z tego pliku naraz. Nie dokładaj zmian spoza zadania (żadnych „przy okazji" refaktorów).
3. Po każdej zmianie zbuduj projekt (`./gradlew :app:assembleDebug`) i napraw błędy, zanim zgłosisz koniec.
4. Na końcu zadania dopisz krótki wpis w sekcji „Dziennik" na dole tego pliku: co zrobiono, co nie działa, co wymaga testu na telefonie.
5. Jeśli coś w tym pliku nie zgadza się z kodem albo z rzeczywistym API biblioteki, nie zgaduj: zatrzymaj się i opisz rozbieżność, Miłosz przekaże ją do czatu.
6. Nowy kod pisz w normalnym formatowaniu Kotlina (nie po jednym tokenie na linię, jak w istniejących plikach). Istniejących plików nie przeformatowuj bez osobnego zadania.
7. Klucz `MAPTILER_API_KEY` jest w `local.properties` – nie wypisuj go i nie przenoś do repozytorium.

## Aktualne zadanie: test Samsung Health (eksperyment)

**Cel:** sprawdzić, czy treningi nagrane opaską Galaxy Fit3 da się pobrać z Samsung Health automatycznie, w zakresie odpowiadającym plikowi FIT/TCX ze Stravy (trasa GPS, czas, wysokość, tętno, prędkość). To spike: kod testowy, bez testów jednostkowych, do wyrzucenia albo przepisania po uzyskaniu odpowiedzi.

**Sprzęt:** telefon Samsung Galaxy A52s 5G (SM-A528B/DS), opaska Galaxy Fit3. Fit3 nie ma GPS – trasę nagrywa telefon przez Samsung Health, z opaski pochodzi tętno i barometr.

**Warunki wstępne (robi Miłosz):**

- Pobrać Samsung Health Data SDK v1.1.0 z https://developer.samsung.com/health/data/overview.html i wrzucić plik `.aar` do `app/libs/`.
- Samsung Health w wersji 6.30.2 lub nowszej.
- Tryb deweloperski: Samsung Health → menu (⋮) → Ustawienia → Samsung Health – informacje → stuknąć wiersz wersji co najmniej 10 razy → „Developer mode (Samsung Health Data SDK)" → zaakceptować → włączyć „Developer Mode for Data Read".
- Zalecane: osobna gałąź, np. `samsung-health-test`.

**Do zrobienia (wykonawca):**

1. Zajrzyj do pliku `.aar` w `app/libs/` (rozpakuj `classes.jar`, wylistuj klasy) i potwierdź nazwy oraz pakiety klas wymienionych niżej. Dopiero potem pisz kod.
2. `app/build.gradle.kts`:
   - `implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))`
   - `implementation("com.google.code.gson:gson:2.13.2")`
   - wtyczka `kotlin-parcelize` (projekt używa wbudowanego Kotlina z AGP – dobierz sposób dodania tak, żeby build przeszedł)
   - Java 17 zamiast 11 (`sourceCompatibility`, `targetCompatibility`) – SDK wymaga Javy 17+.
3. Nowy ekran `SamsungHealthTestScreen` + trasa w `navigation/Screen.kt` + wpis w `MainActivity.kt` (lista `secondaryRoutes` i `NavHost`) + wiersz w `ui/MoreScreen.kt` („Test Samsung Health").
4. Ekran:
   - przycisk „Połącz i pobierz treningi",
   - prośba o uprawnienia do odczytu: ćwiczenia oraz lokalizacja ćwiczeń,
   - odczyt treningów z ostatnich 90 dni, od najnowszych,
   - dla każdej sesji wiersz: data i godzina startu, typ ćwiczenia, czas, dystans, liczba punktów trasy, ile punktów ma wysokość, min/max wysokości z punktów, `altitudeGain`, liczba wpisów logu, ile wpisów ma tętno, ile ma prędkość, średnie i maks. tętno, nazwa/id urządzenia źródłowego,
   - czytelny komunikat błędu (z nazwą klasy wyjątku i kodem), bo to jest główny wynik testu, jeśli coś nie zadziała.
5. Zbuduj projekt. Emulator nie jest obsługiwany przez SDK – test tylko na fizycznym telefonie.

**API SDK – potwierdzone w dokumentacji Samsunga:**

- `HealthDataService.getStore(context)` zwraca `HealthDataStore` (import `com.samsung.android.sdk.health.data.HealthDataStore`).
- Uprawnienia: `Permission.of(DataTypes.EXERCISE, AccessType.READ)`; `healthDataStore.getGrantedPermissions(set)`; `healthDataStore.requestPermissions(set, activity)` – wymaga `Activity`, wywoływane z funkcji `suspend`.
- Odczyt: `DataTypes.EXERCISE.readDataRequestBuilder.setLocalTimeFilter(LocalTimeFilter.of(startLocalDateTime, endLocalDateTime)).setOrdering(Ordering.DESC).build()`, potem `healthDataStore.readData(request).dataList` (z korutyny).
- Punkt danych: `HealthDataPoint`, m.in. `dataSource?.deviceId`. Sesje są pod polem `DataType.ExerciseType.SESSIONS`, typ pod `DataType.ExerciseType.EXERCISE_TYPE`.
- `ExerciseSession` (pakiet `com.samsung.android.sdk.health.data.data.entries`): `startTime: Instant`, `endTime: Instant`, `duration: Duration`, `exerciseType`, `distance: Float?`, `calories: Float`, `altitudeGain: Float?`, `altitudeLoss: Float?`, `minAltitude: Float?`, `maxAltitude: Float?`, `meanHeartRate: Float?`, `maxHeartRate: Float?`, `minHeartRate: Float?`, `meanSpeed: Float?`, `maxSpeed: Float?`, `route: List<ExerciseLocation>?`, `log: List<ExerciseLog>?`, `customTitle: String?`.
- `ExerciseLocation`: `timestamp`, `latitude`, `longitude`, `altitude: Float?`, `accuracy: Float?` (WGS84).
- `ExerciseLog`: `timestamp`, `heartRate`, `speed`, `cadence`, `count`, `power`.
- Błędy: pakiet `com.samsung.android.sdk.health.data.error`; `HealthDataException` i podklasy `AuthorizationException`, `InvalidRequestException`, `PlatformInternalException`, `ResolvablePlatformException`. Dla tej ostatniej: `if (e.hasResolution) e.resolve(activity)` (np. stara wersja Samsung Health).

**API SDK – potwierdzone w pliku `samsung-health-data-api-1.1.0.aar` (2026-11-13):**

- Typ uprawnienia do trasy: `DataTypes.EXERCISE_LOCATION` (istnieje). Prosić o `Permission.of(DataTypes.EXERCISE, AccessType.READ)` i `Permission.of(DataTypes.EXERCISE_LOCATION, AccessType.READ)`.
- Odczyt pola z punktu: `healthDataPoint.getValue(DataType.ExerciseType.SESSIONS)` (jest też `getValueOrDefault`). Pola `DataType.ExerciseType`: `EXERCISE_TYPE`, `SESSIONS`, `CUSTOM_TITLE`, `TOTAL_CALORIES`, `TOTAL_DURATION`.
- Pakiety: `com.samsung.android.sdk.health.data.HealthDataService`, `...data.HealthDataStore`, `...data.permission.Permission`, `...data.permission.AccessType`, `...data.request.DataTypes`, `...data.request.DataType`, `...data.request.LocalTimeFilter`, `...data.request.Ordering`, `...data.data.HealthDataPoint`, `...data.data.entries.ExerciseSession` / `ExerciseLocation` / `ExerciseLog`, `...data.error.*`.
- Manifest biblioteki sam deklaruje `<queries>` dla Samsung Health i `minSdk 29` – w manifeście aplikacji nie trzeba nic dodawać.
- Krok 1 z listy „Do zrobienia" jest więc wykonany; wystarczy szybkie sprawdzenie sygnatur przy pierwszej kompilacji.

**Znane ryzyka:**

- Wysokość może być pusta (zgłaszano, że przewyższenie z barometru Fit3 nie trafia do Samsung Health). Zapas: uzupełnianie wysokości z Open-Meteo, jak w `ui/map/RouteElevationRepository.kt`.
- Treningi z GPS startowane z opaski bywały niewidoczne w telefonie (zgłoszenie z 2024 r.).
- Tryb deweloperski służy tylko do testów na własnym urządzeniu. Publikacja w sklepie wymaga zatwierdzonego partnerstwa Samsunga. Alternatywa na później: Health Connect (trasa wymaga osobnej zgody, brak odczytu cudzych tras w tle, domyślnie 30 dni historii).

**Wynik testu:** Miłosz uruchamia ekran na telefonie i przekazuje zrzut ekranu do czatu. Na jego podstawie zapada decyzja, czy budować pełną synchronizację (nowe treningi → istniejący `ActivityImportManager` → wykrywanie POI i szlaków).

## Lista usprawnień

### A. Dokończenie atrap

1. Ustawienia bez zapisu – brak DataStore; podpiąć pod mapę, routing, próbkowanie wysokości.
2. Jednostki imperialne – przełącznik nic nie zmienia; wspólny formatter dystansu, wysokości, prędkości.
3. Kopia zapasowa – `BackupScreen` to zaślepka; eksport bazy do ZIP/JSON i przywracanie z walidacją wersji.
4. Eksport danych – GPX, GeoJSON, CSV tylko w interfejsie; rozszerzyć `PlannedRouteGpxExporter` na aktywności.
5. Mapy offline – `OfflineMapsScreen` to zaślepka; regiony offline MapLibre z postępem, rozmiarem, usuwaniem.
6. Edycja aktywności (nazwa, data, notatka) – zaślepka w `ActivityDetailsScreen`.
7. Eksport GPX pojedynczej aktywności – zaślepka w `ActivityDetailsScreen`.
8. Zapis punktu z mapy – komunikat „gotowy do podłączenia"; tabela zapisanych miejsc już istnieje.
9. Warstwa „Ukończone szlaki" na mapie – plakietka „Wkrótce" w `MapTrailTools`; `TrailMatcher` już liczy odcinki.
10. Nieaktualne opisy – Statystyki („procent pojawi się po spięciu track matchingu"), Więcej („Matching później", „Backend później").
11. Przycisk „Rozpocznij" na ekranie głównym tylko otwiera mapę – zdecydować, co ma robić.

### B. Nowe funkcje

12. Nagrywanie trasy na żywo (brak uprawnień lokalizacji i kodu GPS): usługa pierwszoplanowa, pauza, zapis do tych samych tabel.
13. Pozycja użytkownika na mapie i „wycentruj na mnie".
14. Import aktywności z GPX (dziś tylko FIT i TCX).
15. Import wielu plików naraz i „Otwórz w TatraTracker" z innych aplikacji.
16. Nawigacja po zaplanowanej trasie: pozostały dystans i podejście, ostrzeżenie o zejściu ze szlaku.
17. Szacowany czas przejścia z uwzględnieniem przewyższeń; docelowo czasy szlakowe z OSM.
18. Pogoda i warunki dla trasy lub szczytu, wschód i zachód słońca (Open-Meteo).
19. Odznaki i cele: Korona Tatr, zdobyte szczyty i schroniska, cele roczne.
20. Zdjęcia i notatki przy aktywności.
21. Statystyki: rok do roku, mapa cieplna śladów, strefy tętna, miesiące.
22. Szczegóły szlaku po kliknięciu na mapie (kolor, nazwa, długość, procent przejścia).
23. Ekran główny z treścią: ostatnia aktywność, postęp, skrót tygodnia.
24. Synchronizacja z Samsung Health (zależy od wyniku testu powyżej).
25. Widok 3D: MapLibre Native na Androida nie obsługuje terenu 3D. Opcje: osobny ekran WebView z MapLibre GL JS + kafle terrain-rgb MapTiler (trasa na terenie, przelot kamerą) albo tańsze cieniowanie rzeźby (hillshade) + pochylenie kamery na głównej mapie.
26. „Live": kamery internetowe w Tatrach i aktualne warunki – najpierw sprawdzić, które kamery pozwalają na osadzanie.

### C. Jakość i wygląd

27. Tryb ciemny (motyw ma tylko `lightColorScheme`).
28. Teksty do `strings.xml` (dziś wpisane w kodzie) – umożliwia wersję EN/SK.
29. Tło ekranu głównego `tatra_home_bg.png` waży 7,5 MB – zamienić na WebP.
30. Własna ikona aplikacji (dziś domyślna; `ic_tatra_logo.xml` już jest).
31. Onboarding i stany pustych ekranów.
32. Obsługa braku internetu: komunikaty, ponawianie, cache (Valhalla, Overpass, Open-Meteo, MapTiler).
33. Dostępność: opisy ikon, pola dotyku, kontrast.
34. Kierunki wyglądu do makiet: kolory szlaków jako język aplikacji, karty aktywności z miniaturą śladu, pierścień postępu, animacje rysowania śladu, odznaki. Makiety przed kodem.

### D. Fundamenty techniczne

35. Testy – jest tylko przykładowy; zacząć od parserów FIT/TCX/GPX, `TrailMatcher`, `ActivityPoiDetector`.
36. Warstwa repozytoriów i DI – `AppDatabase.getDatabase` wołane bezpośrednio w 12 miejscach; logika sieciowa leży w `ui/map`.
37. Podział dużych plików: `MapScreen.kt` (2151 linii), `TrailMatcher.kt` (1964), `RoutePlanningPanel.kt` (1513).
38. Cache postępu szlaków w bazie; przeliczać tylko nowe aktywności.
39. Podejście i zejście zapisywane przy imporcie (z wygładzaniem), zamiast liczenia w locie.
40. ktlint/detekt i przeformatowanie kodu.
41. Aktualizacja bibliotek (Navigation 2.7.7, Lifecycle 2.7.0, Coroutines 1.8.0) i przeniesienie wersji do `libs.versions.toml`.
42. Wydanie: zmiana `com.example.tatratracker`, R8, podpisywanie; debugowy APK ma 87 MB.
43. Klucz MapTiler ograniczyć w panelu MapTiler do identyfikatora aplikacji.
44. CI (GitHub Actions) i README.

### E. Słabe punkty znalezione w interfejsie

45. Mapa, prawy górny róg: przyciski „Punkty", „Warstwy", „Szlaki" – „Warstwy" dubluje dwa pozostałe. Jeden panel „Warstwy"; „Dodaj własny punkt" i „Importuj GPX" jako akcje w nim.
46. Przycisk z ikoną `MyLocation` centruje na Tatrach, nie na użytkowniku – zmienić ikonę (do czasu punktu 13).
47. Ikona `Menu` w lewym górnym rogu otwiera planowanie trasy – zmienić ikonę.
48. Ustawienia warstw (`MapLayerSettings`) trzymane w `remember` – resetują się po wyjściu z mapy.
49. Zaplanowane trasy w dwóch miejscach: zakładka w `HistoryScreen` i `MyRoutesScreen`.
50. „Ukończone szlaki" dostępne ze Statystyk i z Więcej; Statystyki nie pokazują realnego procentu.
51. Trzy implementacje wykresu wysokości (`ElevationProfileChart`, `ActivityElevationSection`, `PlannedRouteElevationProfile`) i cztery osobne `MapView` – ujednolicić.
52. `formatDuration` w 4 plikach, `formatDistance` w 3 – różne formaty tego samego dystansu.
53. `ImportScreen` bez ViewModelu – obrót ekranu gubi stan importu.
54. „Importuj GPX" na mapie importuje zaplanowaną trasę, a opis mówi „ślad lub trasę".

## Proponowana kolejność

1. Test Samsung Health (trwa).
2. Porządki na mapie: 45–48.
3. Etap „bez martwych przycisków": 1, 2, 6–10, 14.
4. Makiety wyglądu (34), potem 23, 27, 29, 30.
5. Kopia, eksport, mapy offline: 3–5.
6. Nagrywanie na żywo: 12, 13, 16.
7. Równolegle fundamenty: 35–40.

## Dziennik

- 2026-11-13 – przegląd kodu, powstanie planu. Kod bez zmian.
- 2026-11-13 – plik SDK w `app/libs/`, tryb deweloperski Data SDK włączony na telefonie, nazwy klas potwierdzone w `.aar`. Zadanie gotowe do wykonania w terminalu.
