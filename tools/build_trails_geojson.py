"""
Buduje plik tatry_szlaki.geojson ze znakowanych szlaków pieszych z OpenStreetMap.

Pobiera relacje route=hiking z Overpass API i zapisuje każdy odcinek (way)
jeden raz, z listą kolorów i nazw szlaków, które nim biegną.
Format jest zgodny z TrailMatcher (czyta "@id" oraz geometrię LineString).

Przykład:
    py tools/build_trails_geojson.py --area parks --output app/src/main/assets/tatry_szlaki.geojson
"""

import argparse
import json
import math
import os
import sys
import time
import urllib.parse
import urllib.request

OVERPASS_ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.private.coffee/api/interpreter",
]

# Ten sam prostokąt, który obejmował poprzedni plik: (south, west, north, east).
TATRA_BBOX = (49.078, 19.574, 49.340, 20.383)

PARK_NAMES = [
    "Tatrzański Park Narodowy",
    "Tatranský národný park",
]

# Szlaki międzynarodowe (np. E3) nie mają własnego znakowania w terenie,
# biegną po istniejących szlakach i zafałszowałyby ich kolory.
EXCLUDED_NETWORKS = {"iwn"}

COLOR_ORDER = ["red", "blue", "green", "yellow", "black"]


def build_query(area):
    if area == "bbox":
        south, west, north, east = TATRA_BBOX
        bbox = f"({south},{west},{north},{east})"
        return f"""
[out:json][timeout:300];
rel["route"="hiking"]{bbox}->.routes;
way(r.routes){bbox}->.ways;
.routes out body;
.ways out geom;
"""

    parks = "\n".join(
        f'  area["boundary"~"^(national_park|protected_area)$"]["name"="{name}"];'
        for name in PARK_NAMES
    )

    return f"""
[out:json][timeout:300];
(
{parks}
)->.parks;
.parks out tags;
rel["route"="hiking"](area.parks)->.routes;
way(r.routes)(area.parks)->.ways;
.routes out body;
.ways out geom;
"""


def fetch_overpass(query):
    data = urllib.parse.urlencode({"data": query}).encode("utf-8")
    last_error = None

    for endpoint in OVERPASS_ENDPOINTS:
        for attempt in range(2):
            try:
                request = urllib.request.Request(
                    endpoint,
                    data=data,
                    headers={"User-Agent": "TatraTracker-trails-builder/1.0"},
                )
                with urllib.request.urlopen(request, timeout=360) as response:
                    return json.load(response)
            except Exception as error:
                last_error = error
                print(f"  {endpoint} (próba {attempt + 1}): {error}", file=sys.stderr)
                time.sleep(5)

    raise RuntimeError(f"Żaden serwer Overpass nie odpowiedział: {last_error}")


def route_color(tags):
    # osmc:symbol ma postać "kolor_drogi:tło:znak", np. "red:white:red_bar".
    symbol = tags.get("osmc:symbol", "")
    color = symbol.split(":", 1)[0].strip().lower()

    if not color:
        color = tags.get("colour", "").strip().lower()

    return color if color in COLOR_ORDER else "other"


def sort_colors(colors):
    order = COLOR_ORDER + ["other"]
    return sorted(colors, key=order.index)


def build_trails(elements):
    parks = [e.get("tags", {}).get("name", "?") for e in elements if e["type"] == "area"]
    ways = {e["id"]: e for e in elements if e["type"] == "way"}

    used_relations = []
    skipped_relations = []

    for element in elements:
        if element["type"] != "relation":
            continue

        if element.get("tags", {}).get("network") in EXCLUDED_NETWORKS:
            skipped_relations.append(element)
        else:
            used_relations.append(element)

    way_colors = {}
    way_names = {}
    uncolored_names = {}

    for relation in used_relations:
        tags = relation.get("tags", {})
        color = route_color(tags)
        name = tags.get("name") or tags.get("ref") or f"relation/{relation['id']}"

        for member in relation.get("members", []):
            if member["type"] != "way" or member["ref"] not in ways:
                continue

            if color == "other":
                uncolored_names.setdefault(member["ref"], set()).add(name)
            else:
                way_colors.setdefault(member["ref"], set()).add(color)
                way_names.setdefault(member["ref"], set()).add(name)

    # Relacje bez koloru (nadszlaki, ścieżki edukacyjne) biegną po zwykłych
    # szlakach, więc liczą się tylko tam, gdzie nie ma żadnego kolorowego szlaku.
    for way_id, names in uncolored_names.items():
        if way_id not in way_colors:
            way_colors[way_id] = {"other"}
            way_names[way_id] = names

    features = []

    for way_id in sorted(way_colors):
        way = ways[way_id]
        coordinates = [
            [round(point["lon"], 7), round(point["lat"], 7)]
            for point in way.get("geometry", [])
            if point
        ]

        if len(coordinates) < 2:
            continue

        features.append({
            "type": "Feature",
            "id": f"way/{way_id}",
            "properties": {
                "@id": f"way/{way_id}",
                "highway": way.get("tags", {}).get("highway", ""),
                "colors": sort_colors(way_colors[way_id]),
                "names": sorted(way_names[way_id]),
            },
            "geometry": {
                "type": "LineString",
                "coordinates": coordinates,
            },
        })

    return parks, used_relations, skipped_relations, features


def distance_m(a, b):
    lon1, lat1 = map(math.radians, a)
    lon2, lat2 = map(math.radians, b)
    h = (
        math.sin((lat2 - lat1) / 2) ** 2
        + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2
    )
    return 2 * 6_371_000 * math.asin(math.sqrt(h))


def line_length_m(coordinates):
    return sum(distance_m(a, b) for a, b in zip(coordinates, coordinates[1:]))


def print_stats(area, parks, used_relations, skipped_relations, features, output):
    total_km = 0.0
    km_by_color = {}
    km_by_highway = {}
    trail_names = set()

    for feature in features:
        properties = feature["properties"]
        km = line_length_m(feature["geometry"]["coordinates"]) / 1000
        total_km += km
        trail_names.update(properties["names"])

        # Odcinek wspólny dla dwóch kolorów liczy się do obu.
        for color in properties["colors"]:
            km_by_color[color] = km_by_color.get(color, 0.0) + km

        highway = properties["highway"] or "(brak)"
        km_by_highway[highway] = km_by_highway.get(highway, 0.0) + km

    print(f"Wariant: {area}")

    if parks:
        print(f"Parki: {', '.join(parks)}")

    print(f"Relacje szlaków: {len(used_relations)} (pominięte międzynarodowe: {len(skipped_relations)})")

    for relation in skipped_relations:
        print(f"  pominięto: {relation.get('tags', {}).get('name', relation['id'])}")

    print(f"Różnych nazw szlaków: {len(trail_names)}")
    print(f"Odcinki (way): {len(features)}")
    print(f"Długość sieci: {total_km:.1f} km")

    print("Kilometry według koloru (wspólne odcinki liczone w każdym kolorze):")
    for color in sort_colors(km_by_color):
        print(f"  {color:<8} {km_by_color[color]:8.1f} km")

    print("Kilometry według typu drogi:")
    for highway, km in sorted(km_by_highway.items(), key=lambda item: -item[1]):
        print(f"  {highway:<14} {km:8.1f} km")

    print(f"Plik: {output} ({os.path.getsize(output) / 1024 / 1024:.2f} MB)")


def main():
    sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(
        description="Buduje tatry_szlaki.geojson ze znakowanych szlaków pieszych OSM."
    )
    parser.add_argument(
        "--area",
        choices=["bbox", "parks"],
        required=True,
        help="bbox: cały prostokąt Tatr z okolicą, parks: tylko TPN i TANAP",
    )
    parser.add_argument("--output", required=True, help="ścieżka wynikowego pliku GeoJSON")
    args = parser.parse_args()

    print("Pobieranie danych z Overpass API...")
    response = fetch_overpass(build_query(args.area))

    parks, used_relations, skipped_relations, features = build_trails(
        response.get("elements", [])
    )

    if args.area == "parks" and not parks:
        raise SystemExit("Nie znaleziono granic parków. Sprawdź PARK_NAMES.")

    if not features:
        raise SystemExit("Nie znaleziono żadnych odcinków szlaków.")

    collection = {"type": "FeatureCollection", "features": features}

    with open(args.output, "w", encoding="utf-8") as file:
        json.dump(collection, file, ensure_ascii=False, separators=(",", ":"))

    print_stats(args.area, parks, used_relations, skipped_relations, features, args.output)


if __name__ == "__main__":
    main()
