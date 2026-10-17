"""Builds the offline city list the app searches for birthplaces and the current place.

Sources (GeoNames data, CC BY 4.0, https://www.geonames.org):
  - geonamescache 3.x (pip): cities with population, coordinates and IANA time zone.
  - reverse_geocoder 1.5.1 (pip download, unpack): rg_cities1000.csv, for state/province names.
Coverage: every city of 15,000+ worldwide, and every place of 500+ in India, Nepal, Sri Lanka and Bangladesh.

Run: python3 tools/cities/build_cities.py <path to rg_cities1000.csv> > engine/src/main/resources/places/cities.tsv
"""
import csv
import sys

import geonamescache

SOUTH_ASIA = {"IN", "NP", "LK", "BD"}
WORLD_MIN = 15000
SOUTH_ASIA_MIN = 500
# Old or common names people still write on birth records, mapped to the GeoNames name (largest Indian match).
# Curated by the Astro Guru and BA; GeoNames' own alternate names are too noisy to show.
INDIA_ALIASES = {
    "Mumbai": ["Bombay"], "Kolkata": ["Calcutta"], "Chennai": ["Madras"], "Bengaluru": ["Bangalore"],
    "Pune": ["Poona"], "Varanasi": ["Benares", "Banaras", "Kashi"], "Vadodara": ["Baroda"],
    "Thiruvananthapuram": ["Trivandrum"], "Kochi": ["Cochin"], "Kozhikode": ["Calicut"],
    "Mangaluru": ["Mangalore"], "Mysuru": ["Mysore"], "Gurugram": ["Gurgaon"], "Prayagraj": ["Allahabad"],
    "Shimla": ["Simla"], "Puducherry": ["Pondicherry"], "Kanpur": ["Cawnpore"], "Guwahati": ["Gauhati"],
    "Tiruchirappalli": ["Trichy", "Trichinopoly"], "Belagavi": ["Belgaum"], "Hubballi": ["Hubli"],
    "Visakhapatnam": ["Vizag", "Waltair"], "Jabalpur": ["Jubbulpore"], "Thanjavur": ["Tanjore"],
    "Kollam": ["Quilon"], "Alappuzha": ["Alleppey"], "Ooty": ["Udhagamandalam", "Ootacamund"],
    "Thrissur": ["Trichur"], "Kalaburagi": ["Gulbarga"], "Vijayapura": ["Bijapur"], "Bhubaneswar": ["Bhubaneshwar"],
}
NAME_NEAR_DEGREES = 0.1
PLACE_NEAR_DEGREES = 0.3


def admin_names(path):
    """State or province by (country, name), nearest point first, plus a nearest-point fallback grid."""
    by_name, grid = {}, {}
    with open(path, encoding="utf-8") as f:
        for row in csv.DictReader(f):
            lat, lon = float(row["lat"]), float(row["lon"])
            by_name.setdefault((row["cc"], row["name"].lower()), []).append((lat, lon, row["admin1"]))
            grid.setdefault((row["cc"], round(lat, 1), round(lon, 1)), []).append((lat, lon, row["admin1"]))
    return by_name, grid


def admin_of(city, by_name, grid):
    lat, lon, cc = city["latitude"], city["longitude"], city["countrycode"]

    def nearest(candidates, limit):
        best = min(candidates, key=lambda c: (c[0] - lat) ** 2 + (c[1] - lon) ** 2, default=None)
        if best and abs(best[0] - lat) < limit and abs(best[1] - lon) < limit:
            return best[2]
        return None

    named = nearest(by_name.get((cc, city["name"].lower()), []), NAME_NEAR_DEGREES)
    if named:
        return named
    steps = [x / 10 for x in range(-3, 4)]
    nearby = [c for dl in steps for dm in steps for c in grid.get((cc, round(lat + dl, 1), round(lon + dm, 1)), [])]
    # The state of the nearest named place; blank when nothing is close, rather than guess.
    return nearest(nearby, PLACE_NEAR_DEGREES) or ""


def main():
    by_name, grid = admin_names(sys.argv[1])
    cities = geonamescache.GeonamesCache(min_city_population=500).get_cities().values()
    alias_for = {}
    for name, names in INDIA_ALIASES.items():
        matches = [c for c in cities if c["countrycode"] == "IN" and c["name"] == name]
        if not matches:
            raise SystemExit(f"alias target not found: {name}")
        alias_for[max(matches, key=lambda c: c["population"])["geonameid"]] = names
    rows = []
    for c in cities:
        minimum = SOUTH_ASIA_MIN if c["countrycode"] in SOUTH_ASIA else WORLD_MIN
        if c["population"] < minimum or not c["timezone"]:
            continue
        admin = admin_of(c, by_name, grid)
        rows.append((c["countrycode"] != "IN", -c["population"], c["name"], [
            str(c["geonameid"]), c["name"], admin, c["countrycode"],
            f"{c['latitude']:.4f}", f"{c['longitude']:.4f}", c["timezone"], str(c["population"]),
            "|".join(alias_for.get(c["geonameid"], [])),
        ]))
    rows.sort(key=lambda r: r[:3])
    out = sys.stdout
    out.write("# AstroLingo city list. Data: GeoNames (CC BY 4.0), built by tools/cities/build_cities.py; do not edit.\n")
    out.write("id\tname\tadmin\tcountry\tlat\tlon\tzone\tpopulation\taliases\n")
    for *_, cells in rows:
        out.write("\t".join(x.replace("\t", " ") for x in cells) + "\n")


if __name__ == "__main__":
    main()
