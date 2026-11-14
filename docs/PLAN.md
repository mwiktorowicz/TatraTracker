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

## Aktualne zadanie: mapa – przebyte szlaki i moje punkty

**Cel:** dwie nowe warstwy na mapie głównej: szlaki, które Miłosz już przeszedł (punkt 9), oraz jego zapisane miejsca (rozbieżność 1 z poprzedniego zadania). Obie z przełącznikiem w panelu „Warstwy", zapamiętywanym jak pozostałe.

**Stan zastany:** `TrailMatchResult.matchedGeoJson` zawiera geometrię dopasowanych odcinków; wynik udostępnia `data/TrailProgressRepository` (jeden na proces). Mapa nie czyta tabeli `saved_places`. W panelu warstw sekcja „Ukończone szlaki" to nieaktywny wiersz z plakietką „Wkrótce".

**1. Warstwa „Przebyte szlaki"**
- Źródło GeoJSON z `matchedGeoJson`, pobierane z `TrailProgressRepository` przez `MapViewModel`. Warstwa odświeża się, gdy repozytorium poda nowy wynik (np. po synchronizacji).
- Wygląd: szeroka, półprzezroczysta zielona poświata (kolor z motywu, `TatraGreen`) narysowana **pod** kolorowymi liniami szlaków i nad podkładem mapy, tak żeby kolor szlaku pozostał czytelny. Szerokość skalowana z przybliżeniem, ok. 3× szerokości linii szlaku.
- W panelu: wiersz „Przebyte szlaki" z podpisem „Odcinki, które masz już za sobą" i przełącznikiem zamiast plakietki „Wkrótce"; przenieś go do sekcji *Mapa* (pod „Moje ślady GPS") i usuń osobną sekcję „Ukończone szlaki".
- Domyślnie wyłączona. Dopasowanie jest kosztowne, więc mapa prosi repozytorium o wynik dopiero po włączeniu przełącznika (albo gdy przełącznik był zapisany jako włączony). W trakcie liczenia przy wierszu mały wskaźnik postępu; przy błędzie krótki komunikat, przełącznik zostaje włączony.
- Brak dopasowanych odcinków = pusta warstwa, bez komunikatu.

**2. Warstwa „Moje punkty"**
- Źródło GeoJSON z `SavedPlaceDao.getAllPlaces()` (Flow) przez `MapViewModel`/`SavedPlaceRepository`; nowy punkt dodany z mapy pojawia się od razu, usunięty znika.
- Symbol według kategorii: `HUT` – istniejąca ikona schroniska, `MOUNTAIN` – istniejąca ikona szczytu, `OTHER` – nowa prosta ikona pinezki (wektor w `res/drawable`, w stylu pozostałych `ic_map_*`). Ikony zapisanych miejsc mają się odróżniać od zwykłych POI mapy (np. kolor akcentu lub obwódka). Pod symbolem nazwa.
- W panelu: przełącznik „Moje punkty" z podpisem „Zapisane miejsca i własne punkty" w sekcji *Punkty*. Domyślnie włączona.
- Dotknięcie punktu: bez nowej obsługi (poza zakresem).

**3. Ustawienia** – dwa nowe pola w `MapLayerSettings` i klucze w `MapLayerSettingsStore` (domyślnie: przebyte szlaki wyłączone, moje punkty włączone); starsze zapisane ustawienia muszą się wczytywać bez błędu. Uzupełnij istniejące testy mapowania ustawień.

**Poza zakresem:** zapis wyniku dopasowania w bazie (punkt 38), szczegóły szlaku po dotknięciu (22), okno szczegółów zapisanego miejsca po dotknięciu, zmiany w ekranie „Ukończone szlaki".

**Test na telefonie (Miłosz):** Warstwy → włącz „Przebyte szlaki": po chwili odcinki z Twoich wędrówek są podświetlone na zielono, a kolory szlaków nadal widać; po wyłączeniu poświata znika; ustawienie zostaje po restarcie aplikacji. „Moje punkty": punkt dodany przez Warstwy → „Dodaj własny punkt" pojawia się na mapie od razu; po usunięciu go w Więcej → Schroniska i punkty znika z mapy; przełącznik go ukrywa.

## Zakończone: koniec martwych przycisków (część 1)

**Decyzje po rozbieżnościach (2026-11-14):** (1) mapa nie pokazywała zapisanych miejsc – warstwa „Moje punkty" wchodzi do zadania powyżej; (2) brak wysokości zapisany jako 0.0 – w eksporcie GPX wysokość równa dokładnie 0.0 jest traktowana jako brak (pomijany `ele`); zmiana modelu na `ele: Double?` dopisana do listy jako punkt 55.

Opis zadania:

**Cel:** przyciski, które dziś otwierają okienko „podepniemy później", zaczynają działać, a opisy w aplikacji przestają mówić o rzeczach już zrobionych. Punkty 6, 7, 8, 10 z listy. Wykonuj podpunkty w podanej kolejności; po każdym projekt ma się budować.

**1. Edycja aktywności (punkt 6)** – `ui/ActivityDetailsScreen.kt`, przycisk „Edytuj aktywność".
- Edytowalne: nazwa i notatka. Daty i danych z GPS nie edytujemy.
- Notatka to nowa kolumna `note` (TEXT, nullable) w `ActivityEntity`; migracja Room 9 → 10 samym `ALTER TABLE`, schemat `10.json`, bez `fallbackToDestructiveMigration`.
- Okno edycji: pole nazwy (niepuste, przycinane), pole notatki (wielowierszowe, opcjonalne), „Zapisz" i „Anuluj". Zapis przez ViewModel, nie z poziomu composable.
- Notatka widoczna w szczegółach aktywności, gdy jest niepusta.
- Synchronizacja z Samsung Health nie może nadpisywać nazwy ani notatki (dziś pomija istniejące aktywności – upewnij się, że tak zostaje).

**2. Eksport GPX pojedynczej aktywności (punkt 7)** – przycisk „Eksportuj GPX" w szczegółach aktywności.
- GPX 1.1 z jednym śladem (`trk`/`trkseg`/`trkpt`): pozycja, `ele` gdy jest, `time` (UTC, ISO 8601) gdy jest, tętno w rozszerzeniu `gpxtpx:TrackPointExtension` (`gpxtpx:hr`) gdy jest. Nazwa aktywności w `metadata` i `trk`.
- Wybór miejsca zapisu systemowym oknem „Utwórz dokument", tak jak robi to eksport zaplanowanej trasy w `ui/PlannedRouteDetailsScreen.kt`; proponowana nazwa pliku z nazwy i daty aktywności.
- Budowanie XML jako czysta funkcja (np. `export/ActivityGpxExporter`), ze wspólnym z `PlannedRouteGpxExporter` kodem do escapowania i nagłówka zamiast kopii. Testy JUnit: znaki specjalne w nazwie, punkty bez wysokości/czasu/tętna, pusty ślad (czytelny błąd zamiast pustego pliku).
- Komunikat po zapisie i przy błędzie.

**3. Zapis własnego punktu z mapy (punkt 8)** – `AddOwnPlaceDialog` w `ui/MapScreen.kt` kończy się dziś komunikatem „gotowy do podłączenia do warstwy danych".
- Zapisz punkt w tabeli `saved_places` (kategoria `OTHER`) w środku aktualnego widoku mapy, używając tej samej logiki co `SavedPlacesViewModel.savePlace` (wydziel wspólny kod, nie kopiuj).
- Po zapisie komunikat „„nazwa" zapisano w Schroniska i punkty".
- Popraw tekst w oknie dialogowym, który mówi „Docelowo punkt zostanie zapisany…", na opis stanu faktycznego.
- **Sprawdź, czy mapa główna pokazuje zapisane miejsca.** Jeśli nie, nie dobudowuj warstwy samodzielnie: zgłoś to jako rozbieżność (zasada 5) z krótką propozycją, a pozostałe podpunkty rób dalej.

**4. Realny postęp w Statystykach i opisy (punkt 10)**
- `ui/StatisticsScreen.kt`, karta „Postęp w Tatrach": zamiast tekstu „Procent pojawi się po spięciu track matchingu…" pokaż procent przebytej sieci szlaków i liczbę ukończonych szlaków – te same wartości co ekran „Ukończone szlaki".
- Dopasowanie nie może liczyć się dwa razy: wydziel wspólne źródło wyniku (np. repozytorium z wynikiem trzymanym w pamięci procesu), z którego korzystają `CompletedTrailsViewModel` i Statystyki. W trakcie liczenia Statystyki pokazują „Liczę postęp…", przy błędzie – krótki komunikat; reszta ekranu działa niezależnie. Zapis wyniku w bazie to punkt 38, poza zakresem.
- `ui/MoreScreen.kt`: usuń plakietki „Gotowy ekran" i „Matching później"; „Backend później" zamień na „Wkrótce"; „Podgląd" przy Ustawieniach zostaje.
- Przejrzyj pozostałe teksty widoczne dla użytkownika pod kątem słów „później", „docelowo", „podepniemy", „etap" i popraw te, które opisują rzeczy już działające. Ekranów Kopia zapasowa, Eksport danych, Mapy offline i Ustawienia nie zmieniaj – to osobne zadania.

**Poza zakresem:** warstwa ukończonych szlaków na mapie (punkt 9 – następne zadanie), ustawienia i jednostki (1, 2), import aktywności z GPX (14), przycisk „Rozpocznij" (11), zmiany wyglądu.

**Test na telefonie (Miłosz):**
- aplikacja uruchamia się po aktualizacji i stare aktywności są na miejscu (migracja);
- zmiana nazwy i dodanie notatki do aktywności zostają po wyjściu i powrocie oraz po ponownej synchronizacji z Samsung Health;
- eksport GPX aktywności tworzy plik, który da się z powrotem otworzyć (np. import w Stravie albo podgląd w innej aplikacji);
- własny punkt dodany na mapie pojawia się w Więcej → Schroniska i punkty;
- Statystyki pokazują ten sam procent co ekran „Ukończone szlaki";
- w zakładce Więcej nie ma plakietek „Gotowy ekran" i „Matching później".

## Zakończone: porządki na mapie

**Cel:** jeden przycisk i jeden panel warstw zamiast trzech dublujących się, ikony zgodne z działaniem, zapamiętywanie ustawień warstw. Przy okazji usunięcie ekranu testowego Samsung Health. Punkty 45–48 z listy.

**Stan zastany:** w `ui/MapScreen.kt` w prawym górnym rogu są trzy przyciski: „Punkty" (`MapPoiToolsPanel`), „Warstwy" (`MapLayersPanel`), „Szlaki" (`MapTrailToolsPanel`). „Warstwy" zawiera przełączniki obu pozostałych. Ustawienia (`MapLayerSettings`) są w `remember` i giną po wyjściu z mapy. Akcja „Importuj GPX" w panelu „Szlaki" niczego nie importuje – pokazuje tylko okienko „Import GPX podepniemy w kolejnym etapie."; import zaplanowanej trasy z GPX działa wyłącznie w „Moich trasach" (`MyRoutesScreen`, `GpxPlannedRouteImporter`). (Ustalone przez wykonawcę 2026-11-14.)

**Do zrobienia:**

1. **Jeden przycisk „Warstwy"** (ikona `Layers`) w prawym górnym rogu. Usuń przyciski „Punkty" i „Szlaki" oraz stany `showPoiPanel` i `showTrailToolsPanel`.
2. **Jeden panel** (`MapLayersPanel`) z sekcjami w tej kolejności:
   - *Mapa:* Szlaki, Poziomice, Moje ślady GPS;
   - *Punkty:* Szczyty, Schroniska, Jaskinie, Jeziora i stawy;
   - *Ukończone szlaki:* wiersz z plakietką „Wkrótce", nieaktywny (do punktu 9);
   - *Akcje:* „Dodaj własny punkt" (otwiera istniejący `AddOwnPlaceDialog`) oraz „Importuj trasę GPX" z podpisem „Dodaj zaplanowaną trasę z pliku". Decyzja Miłosza (2026-11-14): akcja importuje naprawdę, tym samym kodem co „Moje trasy" – logika wydzielona do wspólnego miejsca, bez kopiowania; po udanym imporcie krótki komunikat z nazwą zapisanej trasy, przy błędzie czytelny komunikat; bez automatycznego pokazywania trasy na mapie (osobne zadanie).
   - Każdy przełącznik występuje dokładnie raz. Obecna etykieta „Moje trasy" przy przełączniku `gpsTracks` zmienia się na „Moje ślady GPS".
   - Panel przewijany, jeśli nie mieści się na ekranie.
3. Usuń `MapPoiToolsPanel` i `MapTrailToolsPanel` (pliki `MapPoiTools.kt`, `MapTrailTools.kt`), zachowując `AddOwnPlaceDialog` (przenieś do osobnego pliku).
4. **Ikony:**
   - przycisk centrujący na Tatrach: zamiast `MyLocation` ikona `Landscape`, opis „Pokaż całe Tatry";
   - przycisk planowania trasy (lewy górny róg): zamiast `Menu` ikona `Route`, opis „Planowanie trasy".
5. **Zapamiętywanie warstw:** dodaj Jetpack DataStore (Preferences) i małą klasę, np. `data/MapLayerSettingsStore`, która zapisuje i odczytuje `MapLayerSettings`. Mapa startuje z zapisanymi ustawieniami; każda zmiana przełącznika jest zapisywana. Wersję biblioteki dodaj w `libs.versions.toml`. Nie podpinaj jeszcze ekranu Ustawień – to osobne zadanie (punkt 1).
6. **Usuń ekran testowy Samsung Health:** `ui/SamsungHealthTestScreen.kt`, jego trasę w `navigation/Screen.kt`, wpis w `MainActivity.kt` (`secondaryRoutes` i `NavHost`) oraz sekcję „Eksperymenty" w `ui/MoreScreen.kt`. Synchronizacja na `ImportScreen` zostaje bez zmian.
7. Testy JUnit dla zapisu i odczytu ustawień warstw, jeśli da się to przetestować bez urządzenia (czysta funkcja mapująca ustawienia ↔ klucze); bez testów interfejsu.

**Poza zakresem:** podział `MapScreen.kt` na mniejsze pliki, pozycja użytkownika na mapie (punkt 13), warstwa ukończonych szlaków (punkt 9), zmiany wyglądu panelu poza opisanym układem.

**Test na telefonie (Miłosz):** w prawym górnym rogu jest jeden przycisk; każdy przełącznik w panelu włącza i wyłącza swoją warstwę; po wyłączeniu np. poziomic, wyjściu z mapy i powrocie (oraz po zamknięciu i ponownym otwarciu aplikacji) poziomice nadal są wyłączone; „Dodaj własny punkt" działa jak wcześniej; „Importuj trasę GPX" na mapie zapisuje trasę z pliku (komunikat z nazwą, trasa widoczna w „Moich trasach"), a zły plik daje czytelny błąd; import w „Moich trasach" działa jak wcześniej; w zakładce Więcej nie ma już „Test Samsung Health".

## Zakończone: synchronizacja z Samsung Health

**Cel:** treningi nagrane opaską Galaxy Fit3 trafiają do TatraTracker bez pobierania plików ze Stravy. To już nie eksperyment: kod docelowy, z testami jednostkowymi logiki.

**Wynik testu (2026-11-14, telefon Miłosza):** wpisy z aplikacji `com.sec.android.app.shealth` (urządzenie Galaxy Fit3) są kompletne – ok. 29 tys. punktów trasy na 8 h (mniej więcej 1 punkt/s), każdy z wysokością; log ok. 1 wpis/min z tętnem i prędkością; średnie i maks. tętno w sesji. `altitudeGain` jest puste. Każda wędrówka ma też drugi wpis z aplikacji `com.strava` (urządzenie: telefon) – 0 punktów, pusty log – to duplikat do pominięcia.

**Decyzje (zatwierdzone przez Miłosza):**

1. **Dwa czasy.**
   - *Czas całkowity* = `endTime − startTime` sesji (od startu do mety, z postojami). To główny czas wędrówki: karty w historii, nagłówek szczegółów, rekord „Najdłuższa aktywność", „Łącznie" w statystykach. Zostaje w istniejącej kolumnie `durationMillis`.
   - *Czas w ruchu* = `duration` sesji (bez pauz). Nowa kolumna `movingDurationMillis` w `ActivityEntity`. Używany do średniej prędkości i tempa oraz do pozycji „W ruchu" w statystykach. W szczegółach aktywności pokazać oba: „Czas całkowity" i „W ruchu"; jeśli różnica jest dodatnia, dodatkowo „Postoje".
   - Stan zastany (ustalony przez wykonawcę 2026-11-14): `durationMillis` przechowuje dziś czas w ruchu (FIT: `total_timer_time`, TCX: suma `TotalTimeSeconds`).
   - Migracja Room 8 → 9: dodać kolumnę `movingDurationMillis`; dla istniejących wierszy najpierw `movingDurationMillis = durationMillis`, potem `durationMillis` = różnica między ostatnim a pierwszym znacznikiem czasu punktów tej aktywności – tylko gdy punkty mają znaczniki, wynik jest większy od obecnej wartości i nie przekracza 24 h. W pozostałych przypadkach oba czasy zostają równe. Zapisanych prędkości i tempa nie przeliczać. Bez `fallbackToDestructiveMigration`. Wyeksportować schemat `9.json`.
   - Import plików od teraz: FIT `total_elapsed_time` → całkowity, `total_timer_time` → w ruchu; TCX różnica czasów pierwszego i ostatniego punktu → całkowity, suma `TotalTimeSeconds` → w ruchu. Jeśli któregoś brak, oba równe. Ten sam limit 24 h dla czasu liczonego z punktów.
   - Jeden punkt danych może mieć kilka sesji: pierwsza dostaje `samsung-health:<uid>`, kolejne `samsung-health:<uid>:<n>`. Typ treningu brany z sesji, a gdy pusty – z punktu danych.
2. **Typy treningów:** tylko HIKING (`PredefinedExerciseType.HIKING`). Wszystkie inne typy, także WALKING, są pomijane. Trzymaj listę dozwolonych typów w jednym miejscu (stała), żeby dało się ją łatwo rozszerzyć.
3. **Filtr źródła:** tylko punkty danych z aplikacji `com.sec.android.app.shealth`, których sesja ma niepustą trasę (`route`). Wpisy innych aplikacji (np. `com.strava`) i treningi bez trasy pomijać.
4. **Uruchamianie:**
   - przycisk „Synchronizuj z Samsung Health" na `ImportScreen` (zakres: ostatnie 90 dni) oraz akcja „Pobierz całą historię";
   - automatycznie raz na uruchomienie aplikacji, po cichu, tylko jeśli uprawnienia są już nadane (nigdy nie otwieraj okna zgód automatycznie); po zakończeniu krótki komunikat „Zaimportowano N aktywności", a gdy N = 0 – nic.
   - Bez pracy w tle przy zamkniętej aplikacji.
5. **Identyfikacja i duplikaty:**
   - `sourceFileName = "samsung-health:<uid punktu danych>"` – ponowna synchronizacja nie dodaje tego samego treningu (istniejące `activityExists`);
   - dodatkowo pomiń trening, jeśli w bazie jest aktywność z pliku o starcie w odległości do 5 minut i dystansie różniącym się o mniej niż 5%. (Sprawdź najpierw, co oznacza `ActivityEntity.timestamp` – ma to być czas startu.)
6. **Mapowanie na istniejący model:**
   - `ExerciseLocation` → `TrackPointEntity` (pozycja, `altitude`, czas);
   - tętno i prędkość z `ExerciseLog` przypisz do punktów trasy: każdy punkt dostaje wartość z najbliższego czasowo wpisu logu, o ile jest nie dalej niż 90 s; inaczej `null`;
   - podsumowanie (`ActivityEntity`): dystans, kalorie, średnie i maks. tętno, min/maks. wysokość z sesji, a gdy pole jest puste – policzone z punktów; średnia prędkość i tempo liczone z czasu w ruchu;
   - nazwa: `customTitle`, a gdy pusta – typ + data (np. „Wędrówka 06.11.2026").
7. **Dalsza obróbka:** po zapisie każda aktywność przechodzi tę samą ścieżkę co import pliku (wykrywanie POI przez `ActivityPoiDetector`). Wydziel wspólny kod zamiast go kopiować.
8. **Architektura:** logika w osobnych klasach (np. pakiet `health/`): klient SDK, czysty mapper sesja → model, serwis synchronizacji. Stan ekranu w ViewModelu, nie w `remember`. Mapper i reguła duplikatów jako czyste funkcje z testami JUnit (przypisanie tętna do punktów, puste pola, duplikat/nie-duplikat, dwa czasy).
9. **Błędy:** brak Samsung Health lub stara wersja → `ResolvablePlatformException.resolve(activity)`; brak uprawnień → czytelny komunikat z przyciskiem ponowienia; błąd jednej aktywności nie przerywa pozostałych – na końcu podsumowanie „zaimportowano X, pominięto Y, błędy Z".
10. **Ekran testowy** (`SamsungHealthTestScreen`) zostaje do czasu potwierdzenia, że synchronizacja działa na telefonie; usunięcie będzie osobnym zadaniem.

**Poza zakresem tego zadania:** suma podejść zapisywana w bazie (punkt 39), praca w tle, Health Connect, zmiany wyglądu.

**Test na telefonie (Miłosz):** synchronizacja → w historii pojawiają się wędrówki z 28.10 i 06.11 po jednym razie; szczegóły pokazują trasę na mapie, profil wysokości, tętno, oba czasy; druga synchronizacja nic nie dodaje; statystyki i „Ukończone szlaki" uwzględniają nowe aktywności.

**API SDK:** patrz sekcja „Zakończone: test Samsung Health" poniżej oraz działający kod w `ui/SamsungHealthTestScreen.kt` (stronicowanie, `getDeviceManager()`, obsługa wyjątków).

## Zakończone: test Samsung Health (eksperyment)

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
54. „Importuj GPX" na mapie był zaślepką (okienko „podepniemy w kolejnym etapie"), a opis mówił „ślad lub trasę"; prawdziwy import zaplanowanej trasy był tylko w „Moich trasach". Zrobione w zadaniu „porządki na mapie": „Importuj trasę GPX" na mapie importuje zaplanowaną trasę wspólnym kodem z „Moimi trasami".

55. `TrackPoint.ele` jako `Double?`: brak wysokości zapisywany jako `null` zamiast 0.0 (import FIT/TCX, Samsung Health, wykresy, statystyki). Stare wiersze z 0.0 wymagają osobnej decyzji.

## Proponowana kolejność

1. Test Samsung Health i synchronizacja z Samsung Health (zrobione, 24).
2. Porządki na mapie: 45–48 (zrobione).
3. Etap „bez martwych przycisków": część 1 – 6, 7, 8, 10 (zrobione); 9 + warstwa „Moje punkty" (trwa); potem 1, 2, 14.
4. Makiety wyglądu (34), potem 23, 27, 29, 30.
5. Kopia, eksport, mapy offline: 3–5.
6. Nagrywanie na żywo: 12, 13, 16.
7. Równolegle fundamenty: 35–40.

## Dziennik

- 2026-11-13 – przegląd kodu, powstanie planu. Kod bez zmian.
- 2026-11-13 – plik SDK w `app/libs/`, tryb deweloperski Data SDK włączony na telefonie, nazwy klas potwierdzone w `.aar`. Zadanie gotowe do wykonania w terminalu.
- 2026-11-13 – test Samsung Health: nowy ekran `ui/SamsungHealthTestScreen.kt` (Więcej → Eksperymenty → „Test Samsung Health”), trasa w `Screen.kt` i `MainActivity.kt`, Gradle: SDK z `app/libs`, Gson 2.13.2, wtyczka parcelize (przez `libs.versions.toml`), Java 17. `assembleDebug` przechodzi. Rozbieżności z planem: (1) przy wbudowanym Kotlinie AGP 9 wtyczka parcelize nie dodaje `kotlin-parcelize-runtime`, a SDK używa `kotlinx.parcelize.Parceler` w runtime – bez tego klasy nie było w APK; dodana jawnie w wersji 2.2.10 (Kotlin z AGP). (2) `HealthDataStore.getDeviceManager()` jest w Kotlinie funkcją, nie właściwością. Odczyt pobiera kolejne strony, jeśli SDK zwróci `pageToken` (limit 50). Nazwa urządzenia z `getDeviceManager().getDevice(deviceId)`. Nie testowane na telefonie. Do sprawdzenia: okno zgód (ćwiczenia + lokalizacja ćwiczeń), trasa, wysokość w punktach, `altitudeGain`, tętno i prędkość w logu, urządzenie źródłowe (Fit3 czy telefon). Pełny stos błędu w logcat, tag `SamsungHealthTest`.
- 2026-11-14 – test na telefonie udany (dane z Fit3 kompletne, wpisy Stravy to puste duplikaty, `altitudeGain` puste). Dopisano zadanie „synchronizacja z Samsung Health".
- 2026-11-14 – synchronizacja z Samsung Health (kod). Nowe: pakiet `health/` (modele niezależne od SDK, `SamsungHealthClient` – jedyne użycie SDK, czysty `SamsungHealthSessionMapper`, `DuplicateActivityRule`, `SamsungHealthSyncService`), `fit/ActivitySaver.kt` (wspólny zapis aktywności + wykrywanie POI dla pliku i Samsung Health), `fit/ActivityMetrics.kt` (dwa czasy, limit 24 h, tempo), `viewmodel/SamsungHealthSyncViewModel.kt`, `ui/SamsungHealthSyncSection.kt` na `ImportScreen`, komunikat „Zaimportowano N aktywności” w `MainActivity`. Room 9: `movingDurationMillis` i migracja wg decyzji 1 (SQL sprawdzony na próbnej bazie SQLite, w tym granica 24 h), schemat `9.json`. Import FIT/TCX zapisuje oba czasy. Szczegóły aktywności: „Czas całkowity”, „W ruchu”, „Postoje”; Statystyki: karta „Czas” = całkowity, podpis „W ruchu X”. Testy JUnit: 37 nowych (mapper, duplikaty, czasy); `assembleDebug` i `testDebugUnitTest` przechodzą.
  - Ustalenia wykonawcy (do ewentualnej korekty): dystans duplikatu porównywany względem dystansu treningu z Samsung Health (|różnica| / dystans SH < 5%), start ±5 min włącznie; „pominięto” liczy treningi już zaimportowane i duplikaty plików – treningi innych typów, aplikacji i bez trasy są pomijane bez liczenia; tętno i prędkość szukane osobno (najbliższy wpis z niepustą wartością), tętno ≤ 0 = brak; czas całkowity nigdy krótszy niż czas w ruchu; „Postoje” pokazywane od 1 min (krótsze dałyby „0 min”); synchronizacja automatyczna obejmuje 90 dni, raz na proces aplikacji, jest pomijana, gdy trwa ręczna, a jej błędy trafiają tylko do logcat; „cała historia” = od 01.01.2010, limit 500 stron odczytu; dozwolone typy jako nazwy z `PredefinedExerciseType` (`"HIKING"`) w `SamsungHealthSessionMapper.ALLOWED_EXERCISE_TYPES`, żeby mapper dało się testować bez SDK.
  - Nie testowane na telefonie: migracja na prawdziwej bazie (test `MigrationTestHelper` wymagałby testu instrumentalnego – nie powstał), okno zgód, `resolve()` przy braku/starej wersji Samsung Health, komunikat po synchronizacji automatycznej, czas przetwarzania treningu z ok. 29 tys. punktów, wykrywanie POI dla nowych aktywności. Logi: tag `SamsungHealthSync` (oraz `SamsungHealthClient`).
- 2026-11-14 – synchronizacja przetestowana na telefonie przez Miłosza: działa (migracja, zgody, import, brak duplikatów). Dopisano zadanie „porządki na mapie".
- 2026-11-14 – porządki na mapie (kod). Mapa: jeden przycisk „Warstwy" i jeden panel `MapLayersPanel` (sekcje Mapa / Punkty / „Ukończone szlaki" z plakietką „Wkrótce", nieaktywne / Akcje; przewijany), usunięte `MapPoiTools.kt` i `MapTrailTools.kt`, `AddOwnPlaceDialog` w osobnym pliku, ikony `Route` („Planowanie trasy") i `Landscape` („Pokaż całe Tatry"). Zapamiętywanie warstw: DataStore Preferences 1.2.1, `data/MapLayerSettingsStore.kt` (plik `map_layer_settings`), stan w `MapViewModel` (`layerSettings`, `updateLayerSettings`). Import GPX na mapie (decyzja A): `PlannedRoutesViewModel.importGpxRoute()` zwraca `GpxRouteImportOutcome` i jest używany przez mapę oraz „Moje trasy" (treść komunikatów w „Moich trasach" bez zmian); na mapie komunikat „„nazwa" została zapisana w Moich trasach." albo błąd, w trakcie importu wskaźnik ładowania. Usunięty ekran testowy Samsung Health (`SamsungHealthTestScreen.kt`, trasa w `Screen.kt`, wpisy w `MainActivity.kt`, sekcja „Eksperymenty" w `MoreScreen.kt`). Testy JUnit: 6 nowych (`MapLayerSettingsStoreTest`); `assembleDebug` i `testDebugUnitTest` przechodzą (44 testy).
  - Ustalenia wykonawcy (do ewentualnej korekty): DataStore 1.2.1 zamiast najnowszej 1.3.0 – 1.3.0 wymaga `kotlin-stdlib` 2.2.20, nowszej niż Kotlin wbudowany w AGP (2.2.10); 1.2.1 nie podbija żadnej zależności (coroutines 1.9.0 już były w projekcie). Ustawienia wczytywane raz przy otwarciu mapy; jeśli użytkownik przełączy warstwę przed końcem odczytu, wygrywa jego wybór. Zanim przyjdą zapisane ustawienia, przez ułamek sekundy mogą obowiązywać domyślne (w praktyce odczyt DataStore kończy się przed wczytaniem stylu mapy z sieci). `MapLayerSettingsStore` w pakiecie `data` zależy od `ui.map.MapLayerSettings` – przeniesienie klasy poza zakresem. Kliknięcie akcji w panelu zamyka panel. „Dodaj własny punkt" nadal kończy się komunikatem „gotowy do podłączenia" (punkt 8).
  - Nie testowane na telefonie: wszystko z sekcji „Test na telefonie" zadania, w szczególności pamięć warstw po restarcie aplikacji i import GPX z mapy (poprawny i błędny plik).
- 2026-11-14 – porządki na mapie przetestowane na telefonie przez Miłosza: działa; zmiany na `main`. Dopisano zadanie „koniec martwych przycisków (część 1)".
- 2026-11-14 – koniec martwych przycisków, część 1 (kod). (1) Edycja aktywności: kolumna `note` w `ActivityEntity`, Room 10 (`MIGRATION_9_10` – samo `ALTER TABLE … ADD COLUMN note TEXT`), schemat `10.json`, `ActivityDao.updateNameAndNote`, okno `ui/ActivityEditDialog.kt`, zapis w `ActivityDetailsViewModel.saveDetails`; notatka w górnej karcie szczegółów. Synchronizacja z Samsung Health bez zmian – pomija istniejące (`activityExists`), a `ActivitySaver` tylko wstawia, więc nazwy i notatki nie nadpisze. (2) Eksport GPX aktywności: wspólny `export/GpxXml.kt` (nagłówek, escapowanie, formatowanie, nazwa pliku), czysty `export/ActivityGpxExporter.kt`; `PlannedRouteGpxExporter` korzysta z `GpxXml` (plik trasy porównany bajt w bajt przed i po zmianie – identyczny). Okno „Utwórz dokument” jak w szczegółach trasy, plik zapisuje `ActivityDetailsViewModel.exportGpx` poza wątkiem UI; komunikat po zapisie i przy błędzie. (3) Własny punkt z mapy: wspólny `data/SavedPlaceRepository.kt` (logika z `SavedPlacesViewModel.savePlace`, który teraz go woła), `MapViewModel.saveOwnPlace` (kategoria `OTHER`, środek widoku mapy), komunikat „„nazwa" zapisano w Schroniska i punkty", nowy opis w `AddOwnPlaceDialog`. (4) `data/TrailProgressRepository.kt` – jeden wynik dopasowania na proces (przeniesione tu `COMPLETED_TRAIL_THRESHOLD` i stan, teraz `TrailProgressState`); korzystają z niego `CompletedTrailsViewModel` i `StatisticsViewModel`. Karta „Postęp w Tatrach" w Statystykach: procent (ten sam `formatPercent` co „Ukończone szlaki"), pasek, „Ukończono X z Y szlaków", „Liczę postęp…", „Nie udało się policzyć postępu.". Więcej: usunięte „Gotowy ekran" ×2 i „Matching później", „Backend później" → „Wkrótce" ×3. Mapa, okno akcji trasy: „Eksport … podepniemy w kolejnym etapie." → „Eksport do GPX jest dostępny w szczegółach zapisanej trasy (Więcej → Moje trasy)." Inne nieaktualne teksty poza wyłączonymi ekranami – nie znalezione. Testy JUnit: 7 nowych (`ActivityGpxExporterTest`); `assembleDebug` i `testDebugUnitTest` przechodzą (51 testów).
  - **Rozbieżność 1 (zasada 5): mapa główna nie pokazuje zapisanych miejsc.** `MapScreen`/`MapLayers` nie czytają `saved_places`; pinezka jest tylko na podglądzie jednego miejsca (`SavedPlaceFocusMap`). Warstwy nie dobudowano. Propozycja: warstwa GeoJSON z `SavedPlaceDao.getAllPlaces()` przez `MapViewModel`, symbol wg kategorii, przełącznik „Moje punkty" w sekcji *Punkty* panelu warstw (zapamiętywany jak pozostałe); można to zrobić razem z punktem 9 albo jako osobne zadanie.
  - **Rozbieżność 2 (zasada 5): brak wysokości jest w bazie zapisany jako 0.0, nie `null`.** Model `TrackPoint.ele` nie dopuszcza `null`, a FIT, TCX (`ActivityImportManager`: `ele = altitudeMeters ?: 0.0`) i Samsung Health (`SamsungHealthSessionMapper`: `altitude ?: 0.0`) zamieniają brak na 0. Eksporter pomija `ele` tylko przy `null`, więc dla takich punktów zapisze `<ele>0.0</ele>`. Do decyzji: (a) w eksporcie traktować 0.0 jako brak (proste, w Tatrach nie występuje), albo (b) `TrackPoint.ele: Double?` i zapis `null` przy imporcie (szersza zmiana, dotyczy też wykresów i statystyk; starych wierszy z 0.0 to nie naprawi).
  - Ustalenia wykonawcy (do ewentualnej korekty): tętno ≤ 0 pomijane w GPX; w `metadata` oprócz nazwy czas startu aktywności; przestrzeń nazw `gpxtpx` zawsze w nagłówku; proponowana nazwa pliku `<nazwa>_<rrrr-mm-dd>.gpx` (data lokalna startu, np. `wedrowka_06_11_2026_2026-11-06.gpx`); aktywność bez punktów GPS jest blokowana przed otwarciem okna zapisu (to okno od razu tworzy pusty plik); po udanej edycji brak okienka (zmianę widać od razu), okienko tylko przy błędzie; punkt z mapy dostaje wizytę MANUAL, tak jak dodany na ekranie „Schroniska i punkty"; gdy mapa jeszcze się nie wczytała, zapis punktu kończy się komunikatem zamiast zapisu w domyślnym miejscu. Postęp szlaków: liczenie rusza przy pierwszym otwarciu Statystyk albo „Ukończonych szlaków" i od tej chwili, póki żyje proces aplikacji, przelicza się po każdym imporcie lub usunięciu aktywności – także gdy żaden z tych ekranów nie jest otwarty (np. po automatycznej synchronizacji); w trakcie przeliczania Statystyki pokazują poprzedni procent z podpisem „Liczę postęp…".
  - Nie testowane na telefonie: wszystko z sekcji „Test na telefonie" zadania, w szczególności migracja 9 → 10 na prawdziwej bazie, otwarcie wyeksportowanego GPX w innej aplikacji i czas eksportu treningu z ok. 29 tys. punktów.
- 2026-11-14 – część 1 przetestowana na telefonie przez Miłosza: działa. Rozstrzygnięto rozbieżności 1 i 2 (patrz „Zakończone: koniec martwych przycisków"). Dopisano zadanie „mapa – przebyte szlaki i moje punkty" oraz punkt 55.
- 2026-11-14 – poprawka po rozbieżności 2: `ActivityGpxExporter` traktuje wysokość równą dokładnie 0.0 jako brak (bez `ele`; punkt bez innych danych jest samozamykający). Test JUnit `zeroAltitude_isTreatedAsMissing`; `assembleDebug` i `testDebugUnitTest` przechodzą (52 testy). Model i import bez zmian (punkt 55).
- 2026-11-14 – mapa: przebyte szlaki i moje punkty (kod). Decyzje Miłosza przy planie: poświata najniżej z naszych warstw; moje punkty nad śladem GPS, pod planowaną trasą i jej znacznikami (komentarz o kolejności w `addTatraMapLayers` poprawiony); po błędzie ponowne włączenie przełącznika woła `TrailProgressRepository.recalculate()`, przy poprawnym wyniku nie. (1) „Przebyte szlaki”: `ui/map/MapWalkedTrailsLayer.kt` – źródło GeoJSON z `matchedGeoJson`, linia `TatraGreen`, krycie 0,45, lekkie rozmycie, szerokość rosnąca z przybliżeniem (ok. 5 px przy zoomie 10, 6 px przy 13, 11 px przy 16; linia szlaku ma stałe 1,9 px), od zoomu 8,5 jak szlaki. `MapViewModel.walkedTrails` sięga po repozytorium (leniwie) dopiero, gdy warstwa jest włączona. W panelu wiersz w sekcji *Mapa* z podpisem, wskaźnikiem w trakcie liczenia i czerwonym „Nie udało się policzyć przebytych odcinków.” przy błędzie (na mapie zostaje poprzedni wynik, jeśli był); sekcja „Ukończone szlaki” i `ComingSoonRow` usunięte. (2) „Moje punkty”: `ui/map/MapSavedPlacesLayer.kt` – biały krążek z obwódką `TatraGreenDark`, na nim ikona wg kategorii (`HUT` – schronisko, `MOUNTAIN` – szczyt, `OTHER` – nowa `ic_map_pin.xml`), pod nim nazwa; dane z `SavedPlaceRepository.observePlaces()` (`SavedPlaceDao.getAllPlaces()`) przez `MapViewModel.savedPlaces`. Przełącznik na końcu sekcji *Punkty*. (3) `MapLayerSettings.walkedTrails` (domyślnie wyłączone) i `savedPlaces` (domyślnie włączone), klucze `walked_trails` i `saved_places`. Testy JUnit: 2 nowe i uzupełnione istniejące w `MapLayerSettingsStoreTest` (w tym stare ustawienia bez nowych kluczy); `assembleDebug` i `testDebugUnitTest` przechodzą (54 testy).
  - Ustalenia wykonawcy (do ewentualnej korekty): ponowna próba po błędzie rusza też przy otwarciu mapy z warstwą zapisaną jako włączona (to także „włączenie”); przy ponownej próbie stary komunikat błędu może mignąć, zanim pojawi się wskaźnik. Po wyłączeniu warstwy źródło jest czyszczone, a po ponownym włączeniu gotowy wynik wraca od razu; repozytorium raz uruchomione dalej przelicza wynik po imporcie lub usunięciu aktywności (jak dotąd przy Statystykach). Moje punkty są widoczne przy każdym przybliżeniu, ikony i nazwy mogą na siebie nachodzić (tak jak schroniska), żeby ikona nie rozjechała się z krążkiem. Poświata jest widoczna także przy wyłączonej warstwie „Szlaki”.
  - Nie testowane na telefonie: wszystko z sekcji „Test na telefonie” zadania, w szczególności wygląd poświaty (szerokość, krycie, czytelność koloru szlaku) i krążka z ikoną, czas liczenia przy włączeniu oraz zachowanie przy błędzie.
