"""Generates the golden kundli test set from an independent reference.

Reference: JPL DE421 (via the legacy jplephem reader) for where each body is, with light-time iteration,
and ERFA for aberration, precession and nutation (astropy only for time scales and sidereal time), giving apparent geocentric
longitudes on the true ecliptic and equinox of date. Neither is the Astronomy Engine the app ships,
so agreement within 1 arcminute is a real check. ERFA's built-in plan94 is not used: its Jupiter and
Saturn errors approach 1 arcminute on their own.

Local time to UTC uses Python's zoneinfo (separate from java.time).
Ayanamsa uses the same Lahiri definition as the engine (it is a convention, not a measurement).
Rahu and Ketu are not in this set: there is no independent true node here; LunarNode has its own tests.

Everyone in the set is fictional. Setup (once): pip install astropy jplephem, then pip download --no-deps --no-binary :all: de421
and unpack it (the PyPI package holds the DE421 data as .npy files and no longer installs).
Run: python3 tools/golden/generate_golden.py <unpacked de421-2008.1 dir> > engine/src/test/resources/golden/charts.csv
"""
import math
import random
import sys
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

import erfa
from astropy import units as u
import numpy as np
from astropy.time import Time
from astropy.utils import iers
from jplephem.ephem import Ephemeris

CITIES = [
    ("Pune", 18.5204, 73.8567, "Asia/Kolkata"),
    ("Chennai", 13.0827, 80.2707, "Asia/Kolkata"),
    ("Delhi", 28.6139, 77.2090, "Asia/Kolkata"),
    ("Jaipur", 26.9124, 75.7873, "Asia/Kolkata"),
    ("Varanasi", 25.3176, 82.9739, "Asia/Kolkata"),
    ("Guwahati", 26.1445, 91.7362, "Asia/Kolkata"),
    ("Kochi", 9.9312, 76.2673, "Asia/Kolkata"),
    ("Kathmandu", 27.7172, 85.3240, "Asia/Kathmandu"),
    ("Colombo", 6.9271, 79.8612, "Asia/Colombo"),
    ("Dubai", 25.2048, 55.2708, "Asia/Dubai"),
    ("London", 51.5072, -0.1276, "Europe/London"),
    ("Berlin", 52.5200, 13.4050, "Europe/Berlin"),
    ("New York", 40.7128, -74.0060, "America/New_York"),
    ("Toronto", 43.6532, -79.3832, "America/Toronto"),
    ("Singapore", 1.3521, 103.8198, "Asia/Singapore"),
    ("Sydney", -33.8688, 151.2093, "Australia/Sydney"),
    ("Nairobi", -1.2921, 36.8219, "Africa/Nairobi"),
]
GRAHAS = [("SURYA", "sun"), ("CHANDRA", "moon"), ("MANGAL", "mars"), ("BUDHA", "mercury"),
          ("GURU", "jupiter"), ("SHUKRA", "venus"), ("SHANI", "saturn")]
FIRST = ["Asha", "Ravi", "Meera", "Kabir", "Isha", "Arjun", "Tara", "Dev", "Nila", "Vikram"]
LAST = ["Testwal", "Sampleton", "Fictor", "Demora", "Placeholt"]


def lahiri(t_centuries_tt):
    # Same definition as engine Ayanamsa.lahiri: value at JD 2435553.5 plus IAU 2006 general precession.
    t0 = (2435553.5 - 2451545.0) / 36525.0
    def p(t):
        return (5028.796195 * t + 1.1054348 * t * t) / 3600.0
    return 23.245524743 + p(t_centuries_tt) - p(t0)


LIGHT_KM_PER_DAY = 299792.458 * 86400.0
AU_KM = 149597870.7


class De421:
    def __init__(self, path):
        sys.path.insert(0, path)
        import de421  # noqa: E402  the unpacked data package, not an installed module
        self.eph = Ephemeris(de421)

    def earth(self, jd):
        return (self.eph.position("earthmoon", jd) - self.eph.earth_share * self.eph.position("moon", jd)).ravel()

    def body(self, name, jd):
        if name == "moon":
            return (self.eph.position("earthmoon", jd) + self.eph.moon_share * self.eph.position("moon", jd)).ravel()
        return self.eph.position(name, jd).ravel()

    def apparent_longitude(self, name, t):
        jd = t.tdb.jd
        earth = self.earth(jd)
        tau = 0.0
        for _ in range(3):  # light-time iteration
            pos = self.body(name, jd - tau)
            tau = np.linalg.norm(pos - earth) / LIGHT_KM_PER_DAY
        geo = pos - earth
        # Annual aberration (ERFA ab), from Earth's barycentric velocity and distance to the Sun.
        velocity = (self.earth(jd + 1e-3) - self.earth(jd - 1e-3)) / 2e-3 / LIGHT_KM_PER_DAY
        sun_au = np.linalg.norm(earth - self.eph.position("sun", jd).ravel()) / AU_KM
        direction = erfa.ab(geo / np.linalg.norm(geo), velocity, sun_au, math.sqrt(1.0 - velocity @ velocity))
        # Precession and nutation (IAU 2006/2000A) to the true equator of date, then the true ecliptic.
        x, y, z = erfa.pnm06a(t.tt.jd1, t.tt.jd2) @ direction
        _, deps = erfa.nut06a(t.tt.jd1, t.tt.jd2)
        eps = erfa.obl06(t.tt.jd1, t.tt.jd2) + deps
        return math.degrees(math.atan2(y * math.cos(eps) + z * math.sin(eps), x)) % 360.0


def lagna_tropical(t, lat, lon):
    last = t.sidereal_time("apparent", longitude=lon * u.deg).to_value(u.deg)
    eps = math.degrees(erfa.obl06(t.tt.jd1, t.tt.jd2))
    ramc, phi, e = math.radians(last), math.radians(lat), math.radians(eps)
    lam = math.atan2(math.cos(ramc), -(math.sin(ramc) * math.cos(e) + math.tan(phi) * math.sin(e)))
    return math.degrees(lam) % 360.0


def main():
    iers.conf.auto_download = False  # bundled tables only, so reruns give the same file
    rng = random.Random(20261016)
    out = sys.stdout
    out.write("# Golden kundli set, fictional people. Generated by tools/golden/generate_golden.py; do not edit.\n")
    out.write("name,local,zone,lat,lon,utc_millis,ayanamsa,lagna," + ",".join(g for g, _ in GRAHAS) + "\n")
    de421 = De421(sys.argv[1])
    for i in range(50):
        city, lat, lon, zone = CITIES[i % len(CITIES)]
        while True:
            local = datetime(1950, 1, 1) + timedelta(minutes=rng.randrange(0, 75 * 365 * 24 * 60))
            aware = local.replace(tzinfo=ZoneInfo(zone))
            # Skip times that do not exist or happen twice; those have their own unit tests.
            round_trip = aware.astimezone(ZoneInfo("UTC")).astimezone(ZoneInfo(zone)).replace(tzinfo=None)
            if round_trip == local and aware.fold == 0 and aware.replace(fold=1).utcoffset() == aware.utcoffset():
                break
        millis = int(aware.timestamp() * 1000)
        t = Time(millis / 1000.0, format="unix", scale="utc")
        ayan = lahiri((t.tt.jd - 2451545.0) / 36525.0)
        lagna = (lagna_tropical(t, lat, lon) - ayan) % 360.0
        lons = [(de421.apparent_longitude(b, t) - ayan) % 360.0 for _, b in GRAHAS]
        name = f"{FIRST[i % len(FIRST)]} {LAST[i // len(FIRST)]} ({city})"
        row = [name, local.strftime("%Y-%m-%dT%H:%M"), zone, f"{lat}", f"{lon}", str(millis),
               f"{ayan:.6f}", f"{lagna:.6f}"] + [f"{x:.6f}" for x in lons]
        out.write(",".join(row) + "\n")


if __name__ == "__main__":
    main()
