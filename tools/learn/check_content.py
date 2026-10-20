#!/usr/bin/env python3
"""Quick check of Learn chapter files against docs/spec/learn-content.md, for content writers.

Usage: python3 tools/learn/check_content.py [chapter.json ...]
With no arguments it checks every file listed in course.json. The Kotlin tests in learn/ enforce the same rules in CI.
"""
import json
import re
import pathlib
import sys

CONTENT = pathlib.Path(__file__).resolve().parents[2] / "learn/src/main/resources/content/learn"
FACTS_NO_TIME = {"SUN_RASHI", "MOON_RASHI", "MOON_NAKSHATRA", "MOON_NAKSHATRA_LORD", "MANGAL_RASHI", "BUDHA_RASHI",
                 "GURU_RASHI", "SHUKRA_RASHI", "SHANI_RASHI", "RAHU_RASHI", "KETU_RASHI"}
FACTS_TIME = {"LAGNA", "LAGNA_LORD", "SUN_HOUSE", "MOON_HOUSE", "GURU_HOUSE", "SHANI_HOUSE"}
VISUAL_RANGE = {"zodiac": 12, "houses": 12, "nakshatras": 27}
GRAHAS = {"SURYA", "CHANDRA", "MANGAL", "BUDHA", "GURU", "SHUKRA", "SHANI", "RAHU", "KETU"}
LIMITS = {"title": 40, "body": 320, "question": 140, "explanation": 260, "outcome": 70, "badge": 32, "label": 24,
          "value": 60, "think": 40, "facts": 90}
# Chapters with one lesson per graha, rashi or bhava (PO, 20 Oct 2026) may have up to 13 lessons.
DEEP_DIVE = {"grahas"}
NATURES = {"benefic", "malefic", "neutral", "shadow"}
# A card whose title names a graha must say so in "grahas", so the chart bubble marks the right one.
NAMES = {"Surya": "SURYA", "Chandra": "CHANDRA", "Mangal": "MANGAL", "Budha": "BUDHA", "Guru": "GURU", "Shukra": "SHUKRA",
         "Shani": "SHANI", "Rahu": "RAHU", "Ketu": "KETU", "Sun": "SURYA", "Moon": "CHANDRA", "Mars": "MANGAL",
         "Mercury": "BUDHA", "Jupiter": "GURU", "Venus": "SHUKRA", "Saturn": "SHANI"}


def text(errors, where, value, limit=None):
    if not isinstance(value, dict) or not str(value.get("en", "")).strip():
        errors.append(f"{where}: missing English text")
        return
    if limit and len(value["en"]) > limit:
        errors.append(f"{where}: {len(value['en'])} characters, limit {limit}")


def quiz(errors, card):
    cid = card["id"]
    text(errors, f"{cid} question", card.get("question"), LIMITS["question"])
    text(errors, f"{cid} explanation", card.get("explanation"), LIMITS["explanation"])
    options = card.get("options", [])
    if not 3 <= len(options) <= 4:
        errors.append(f"{cid}: needs 3 or 4 options")
    for i, option in enumerate(options):
        text(errors, f"{cid} option {i}", option, 60)
    if len({o.get("en") for o in options}) != len(options):
        errors.append(f"{cid}: duplicate options")
    if not isinstance(card.get("answer"), int) or not 0 <= card["answer"] < len(options):
        errors.append(f"{cid}: answer index outside the options")


def details(errors, cid, card):
    """Graha tags, the nature badge, fact rows, chip groups and the one-page sheet."""
    tags = card.get("grahas", [])
    if any(t not in GRAHAS for t in tags) or len(set(tags)) != len(tags):
        errors.append(f"{cid}: grahas must be distinct graha names")
    visual = card.get("visual") or {}
    marked = set(tags) | ({visual.get("value")} if visual.get("kind") == "graha" else set())
    title = card.get("title", {}).get("en", "")
    named = {g for name, g in NAMES.items() if re.search(rf"\b{name}\b", title)}
    if named and not named & marked:
        errors.append(f"{cid}: title names {sorted(named)} but the card isn't tagged with it")
    badge = card.get("badge")
    if badge:
        if badge.get("kind") not in NATURES:
            errors.append(f"{cid}: badge kind must be one of {sorted(NATURES)}")
        text(errors, f"{cid} badge", badge.get("text"), LIMITS["badge"])
    for row in card.get("rows", []):
        text(errors, f"{cid} row label", row.get("label"), LIMITS["label"])
        text(errors, f"{cid} row value", row.get("value"), LIMITS["value"])
    if len(card.get("rows", [])) > 8 or len(card.get("chips", [])) > 4:
        errors.append(f"{cid}: at most 8 rows and 4 chip groups")
    for group in card.get("chips", []):
        text(errors, f"{cid} chip label", group.get("label"), LIMITS["label"])
        text(errors, f"{cid} chips", group.get("items"), LIMITS["value"])
    sheet = card.get("sheet", [])
    if sheet and sorted(e.get("graha") for e in sheet) != sorted(GRAHAS):
        errors.append(f"{cid}: a sheet lists each of the nine grahas once")
    for entry in sheet:
        if (entry.get("nature") or {}).get("kind") not in NATURES:
            errors.append(f"{cid}: sheet entry {entry.get('graha')} needs a nature")
        text(errors, f"{cid} sheet name", entry.get("name"), LIMITS["label"])
        text(errors, f"{cid} sheet think", entry.get("think"), LIMITS["think"])
        text(errors, f"{cid} sheet nature", (entry.get("nature") or {}).get("text"), LIMITS["badge"])
        text(errors, f"{cid} sheet facts", entry.get("facts"), LIMITS["facts"])


def check_chapter(path, ids):
    errors = []
    chapter = json.loads(path.read_text(encoding="utf-8"))
    chid = chapter.get("id", "?")
    if chapter.get("schemaVersion") != 1:
        errors.append(f"{path.name}: schemaVersion must be 1")
    text(errors, f"{chid} title", chapter.get("title"), LIMITS["title"])
    text(errors, f"{chid} summary", chapter.get("summary"), LIMITS["body"])
    text(errors, f"{chid} outcome", chapter.get("outcome"), LIMITS["outcome"])
    check = chapter.get("check", [])
    if len(check) != 2:
        errors.append(f"{chid}: needs exactly 2 check questions")
    for card in check:
        if card.get("type") != "quiz" or not card.get("id", "").startswith(f"{chid}.check."):
            errors.append(f"{chid}: check questions are quiz cards with ids {chid}.check.N")
        quiz(errors, card)
        ids.append(card.get("id"))
    lessons = chapter.get("lessons", [])
    most = 13 if chid in DEEP_DIVE else 5
    if not 3 <= len(lessons) <= most:
        errors.append(f"{chid}: needs 3 to {most} lessons, has {len(lessons)}")
    for lesson in lessons:
        lid = lesson.get("id", "?")
        ids.append(lid)
        if not lid.startswith(f"{chid}."):
            errors.append(f"{lid}: lesson id must start with {chid}.")
        text(errors, f"{lid} title", lesson.get("title"), LIMITS["title"])
        if not 3 <= lesson.get("minutes", 0) <= 5:
            errors.append(f"{lid}: minutes must be 3 to 5")
        cards = lesson.get("cards", [])
        kinds = [c.get("type") for c in cards]
        learn = [k for k in kinds if k in ("learn", "yourChart")]
        if kinds[-3:] != ["quiz"] * 3 or kinds.count("quiz") != 3:
            errors.append(f"{lid}: must end with exactly 3 quiz cards")
        if not 5 <= len(learn) <= 7:
            errors.append(f"{lid}: needs 5 to 7 learn cards, has {len(learn)}")
        for card in cards:
            cid = card.get("id", "?")
            ids.append(cid)
            if not cid.startswith(f"{lid}."):
                errors.append(f"{cid}: card id must start with {lid}.")
            kind = card.get("type")
            if kind == "quiz":
                quiz(errors, card)
            elif kind in ("learn", "yourChart"):
                text(errors, f"{cid} title", card.get("title"), LIMITS["title"])
                text(errors, f"{cid} body", card.get("body"), LIMITS["body"])
                if kind == "yourChart":
                    fact = card.get("fact")
                    if fact not in FACTS_NO_TIME | FACTS_TIME:
                        errors.append(f"{cid}: unknown fact {fact}")
                    if "{value}" not in card.get("body", {}).get("en", ""):
                        errors.append(f"{cid}: body needs {{value}}")
                    if fact in FACTS_TIME:
                        text(errors, f"{cid} unknownTime", card.get("unknownTime"), LIMITS["body"])
                details(errors, cid, card)
                visual = card.get("visual")
                if card.get("flip") and (kind != "learn" or not visual):
                    errors.append(f"{cid}: only a learn card with a visual can flip")
                if visual:
                    k, v = visual.get("kind"), visual.get("value")
                    if k == "glyph":
                        if not isinstance(v, str) or not 1 <= len(v) <= 2:
                            errors.append(f"{cid}: glyph must be 1 or 2 characters")
                    elif k == "graha":
                        if v not in GRAHAS:
                            errors.append(f"{cid}: unknown graha {v}")
                    elif k in VISUAL_RANGE:
                        if not isinstance(v, int) or not 0 <= v <= VISUAL_RANGE[k]:
                            errors.append(f"{cid}: {k} value out of range")
                    else:
                        errors.append(f"{cid}: unknown visual {k}")
            else:
                errors.append(f"{cid}: unknown card type {kind}")
    return errors


def main(argv):
    if argv:
        paths = [pathlib.Path(a) for a in argv]
    else:
        course = json.loads((CONTENT / "course.json").read_text(encoding="utf-8"))
        paths = [CONTENT / name for name in course["chapters"]]
    ids, errors = [], []
    for path in paths:
        errors += check_chapter(path, ids)
    dupes = sorted({i for i in ids if ids.count(i) > 1})
    if dupes:
        errors.append(f"duplicate ids: {dupes}")
    for e in errors:
        print(e)
    print(f"{len(paths)} file(s), {len(errors)} problem(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
