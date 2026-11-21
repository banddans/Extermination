#!/usr/bin/env python3
"""Apply a finished game's results to the leaderboard database.

Invoked by ExterminationMod.restartGame() as:

    python3 updateDatabase.py <database.csv> <update.csv> <serverInput.data>

database.csv      Persistent player database (owned by this script).
                  Columns: uuid,name,tier,progress,prestige

update.csv        Written by GameData.playerVictoryRecord / fallenVictoryRecord.
                  One quoted CSV row per rewarded player:
                  "uuid","name","progressDelta","prestigeDelta"
                  It is consumed (truncated) after being applied, so a restart
                  without a recorded result does not re-apply old rewards.

serverInput.data  Ranked leaderboard read by Leaderboard.java, best first:
                  uuid~name~tier~progress~prestige

Rules (derived from the plugin's victory cards):
  * Unranked players are tier 1 with 0 progress.
  * PROGRESS_PER_TIER progress (shown in game as "x / 20") advances one tier;
    leftover progress carries over. The tier is capped at MAX_TIER.
  * Prestige accumulates without limit.
  * Ranking: tier, then prestige, then progress (all descending).
"""

import csv
import os
import sys
import tempfile

PROGRESS_PER_TIER = 20
MIN_TIER = 1
MAX_TIER = 6  # GameData.participants covers tiers 0..6

DB_FIELDS = ["uuid", "name", "tier", "progress", "prestige"]


def to_int(value, default=0):
    try:
        return int(float(value))
    except (TypeError, ValueError):
        return default


def clean_name(name):
    # '~' is the serverInput.data separator and newlines would break the line
    # format, so strip them from player names.
    return name.replace("~", "").replace("\r", "").replace("\n", "").strip()


def load_database(path):
    players = {}
    if not os.path.exists(path):
        return players
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            uuid = (row.get("uuid") or "").strip()
            if not uuid:
                continue
            players[uuid] = {
                "uuid": uuid,
                "name": clean_name(row.get("name") or ""),
                "tier": min(max(to_int(row.get("tier"), MIN_TIER), MIN_TIER), MAX_TIER),
                "progress": max(to_int(row.get("progress")), 0),
                "prestige": max(to_int(row.get("prestige")), 0),
            }
    return players


def load_updates(path):
    updates = []
    if not os.path.exists(path):
        return updates
    with open(path, newline="", encoding="utf-8") as f:
        for lineno, row in enumerate(csv.reader(f), 1):
            if not row or not row[0].strip():
                continue
            if len(row) < 4:
                print(
                    f"update.csv line {lineno}: expected 4 columns, got {len(row)}; skipped"
                )
                continue
            updates.append(
                (row[0].strip(), clean_name(row[1]), to_int(row[2]), to_int(row[3]))
            )
    return updates


def apply_update(players, uuid, name, progress, prestige):
    p = players.get(uuid)
    if p is None:
        p = {"uuid": uuid, "name": name, "tier": MIN_TIER, "progress": 0, "prestige": 0}
        players[uuid] = p
    if name:
        p["name"] = name  # keep the most recent name

    p["progress"] += max(progress, 0)
    p["prestige"] += max(prestige, 0)

    while p["progress"] >= PROGRESS_PER_TIER and p["tier"] < MAX_TIER:
        p["progress"] -= PROGRESS_PER_TIER
        p["tier"] += 1
    if p["tier"] >= MAX_TIER:
        p["tier"] = MAX_TIER
        p["progress"] = min(p["progress"], PROGRESS_PER_TIER)

    print(
        f"{p['name']} ({uuid}): +{progress} progress, +{prestige} prestige "
        f"-> tier {p['tier']}, progress {p['progress']}, prestige {p['prestige']}"
    )


def ranked(players):
    return sorted(
        players.values(),
        key=lambda p: (-p["tier"], -p["prestige"], -p["progress"], p["name"].lower()),
    )


def atomic_write(path, write_fn):
    """Write via a temp file + rename so the server never reads a partial file."""
    directory = os.path.dirname(os.path.abspath(path))
    os.makedirs(directory, exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=directory, prefix=".tmp-", text=True)
    try:
        with os.fdopen(fd, "w", newline="", encoding="utf-8") as f:
            write_fn(f)
        os.replace(tmp, path)
    except BaseException:
        os.unlink(tmp)
        raise


def main(argv):
    if len(argv) != 4:
        print(f"usage: {argv[0]} <database.csv> <update.csv> <serverInput.data>")
        return 2
    db_path, update_path, output_path = argv[1:]

    players = load_database(db_path)
    updates = load_updates(update_path)
    print(f"loaded {len(players)} players, {len(updates)} updates")

    for update in updates:
        apply_update(players, *update)

    order = ranked(players)

    def write_db(f):
        w = csv.DictWriter(f, fieldnames=DB_FIELDS)
        w.writeheader()
        w.writerows(order)

    def write_server_input(f):
        for p in order:
            f.write(
                f"{p['uuid']}~{p['name']}~{p['tier']}~{p['progress']}~{p['prestige']}\n"
            )

    atomic_write(db_path, write_db)
    atomic_write(output_path, write_server_input)

    # Consume the update so it isn't applied again on the next restart.
    if os.path.exists(update_path):
        open(update_path, "w").close()

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
