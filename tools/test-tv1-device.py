"""ADB smoke test for the TV1 prototype. Backs up and restores history fixtures.

Requires Python 3, a debug APK already installed, and an unlocked ADB target.
Example: python tools/test-tv1-device.py --adb PATH_TO_ADB --serial emulator-5554
"""
import argparse
import base64
import datetime
import hashlib
import json
import pathlib
import re
import subprocess
import sqlite3
import sys
import time
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8")

parser = argparse.ArgumentParser()
parser.add_argument("--adb", default="adb")
parser.add_argument("--serial", required=True)
parser.add_argument("--output", default="build/tv1-device")
args = parser.parse_args()
output = pathlib.Path(args.output)
output.mkdir(parents=True, exist_ok=True)
package = "com.example.wikipediaclient"
prefix = package + ":id/"
report = {"serial": args.serial, "started": datetime.datetime.now().astimezone().isoformat(), "checks": []}
sequence = 0
database_files = ("personal_articles.db", "personal_articles.db-wal",
                  "personal_articles.db-shm", "personal_articles.db-journal")


def adb(*command, data=None, check=True):
    result = subprocess.run([args.adb, "-s", args.serial, *command], input=data,
                            capture_output=True, timeout=40)
    if check and result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", "replace") or result.stdout.decode("utf-8", "replace"))
    return result


def shell(*command):
    return adb("shell", *command).stdout.decode("utf-8", "replace").strip()


def tree(label="ui"):
    global sequence
    sequence += 1
    raw = adb("exec-out", "uiautomator", "dump", "/dev/tty").stdout.decode("utf-8", "replace")
    start, end = raw.find("<?xml"), raw.rfind("</hierarchy>")
    if start < 0 or end < 0:
        raise RuntimeError("UI hierarchy unavailable: " + raw)
    raw = raw[start:end + len("</hierarchy>")]
    (output / f"{sequence:02d}-{label}.xml").write_text(raw, encoding="utf-8")
    return ET.fromstring(raw)


def find(root, resource=None, text=None, description=None):
    for node in root.iter("node"):
        if resource and node.get("resource-id") != (resource if ":" in resource else prefix + resource):
            continue
        if text is not None and node.get("text") != text:
            continue
        if description is not None and node.get("content-desc") != description:
            continue
        return node
    return None


def require(resource=None, text=None, description=None):
    for attempt in range(3):
        root = tree()
        node = find(root, resource, text, description)
        if node is not None:
            return node
        if attempt == 0:
            scroll = next((n for n in root.iter("node") if n.get("scrollable") == "true"), None)
            if scroll is not None:
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", scroll.get("bounds")))
                shell("input", "swipe", str((x1+x2)//2), str(y2-100), str((x1+x2)//2), str(y1+100), "300")
        time.sleep(.4)
    raise AssertionError(f"Missing UI node: {resource}, {text}, {description}")


def tap(resource=None, text=None, description=None):
    node = require(resource, text, description)
    assert node.get("enabled") == "true", "Control is disabled"
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def shot(name):
    (output / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p").stdout)


def passed(name):
    report["checks"].append({"name": name, "result": "PASS"})
    print("PASS:", name, flush=True)


def launch():
    shell("am", "start", "-W", "-n", package + "/.MainActivity")


def stop():
    shell("am", "force-stop", package)


def set_history(value):
    stop()
    # Isolate each legacy-migration fixture from the previous SQLite test state.
    for name in database_files:
        shell("run-as", package, "rm", "-f", "databases/" + name)
    document = ET.Element("map")
    ET.SubElement(document, "string", name="entries").text = value
    shell("run-as", package, "mkdir", "-p", "shared_prefs")
    adb("shell", "run-as", package, "tee", "shared_prefs/reading_history.xml",
        data=ET.tostring(document, encoding="utf-8", xml_declaration=True))
    shell("run-as", package, "rm", "-f", "shared_prefs/reading_history.xml.bak")
    launch()


def rotate(value):
    shell("settings", "put", "system", "accelerometer_rotation", "0")
    shell("settings", "put", "system", "user_rotation", str(value))
    for _ in range(4):
        if tree("rotation").get("rotation") == str(value):
            return
    raise AssertionError("Device did not rotate")


backup = {}
rotation = {}
failure = None
try:
    report["androidApi"] = shell("getprop", "ro.build.version.sdk")
    report["androidRelease"] = shell("getprop", "ro.build.version.release")
    report["deviceModel"] = shell("getprop", "ro.product.model")
    apk_path = shell("pm", "path", package).splitlines()[0].removeprefix("package:")
    report["apkSha256"] = hashlib.sha256(adb("exec-out", "cat", apk_path).stdout).hexdigest().upper()
    crash_before = adb("logcat", "-b", "crash", "-d").stdout
    for key in ("accelerometer_rotation", "user_rotation"):
        rotation[key] = shell("settings", "get", "system", key)
    stop()
    paths = ["shared_prefs/" + name for name in ("reading_history.xml", "reading_history.xml.bak")]
    paths += ["databases/" + name for name in database_files]
    for name in paths:
        result = adb("exec-out", "run-as", package, "cat", name, check=False)
        backup[name] = result.stdout if result.returncode == 0 else None
        if result.returncode and b"No such file" not in result.stderr + result.stdout:
            raise RuntimeError("Cannot back up app history: " + result.stderr.decode("utf-8", "replace"))
    # Persist a recovery copy before writing fixtures, including when this script is interrupted.
    (output / "history-backup.json").write_text(json.dumps(
        {"encoding": "base64", "files": {
            name: None if value is None else base64.b64encode(value).decode("ascii")
            for name, value in backup.items()}},
        ensure_ascii=False), encoding="utf-8")
    now = int(time.time() * 1000)
    fixture = [dict(language="vi", pageId=1, title="Bài A kiểm thử", canonicalUrl="https://vi.wikipedia.org/wiki/A", lastReadAt=now-10000),
               dict(language="vi", pageId=2, title="Bài B kiểm thử", canonicalUrl="https://vi.wikipedia.org/wiki/B", lastReadAt=now)]
    rotate(0)
    set_history(json.dumps(fixture, ensure_ascii=False))
    tap("historyFragment")
    require("tv_history_count", "Đã đọc: 2 bài")
    titles = [n.get("text") for n in tree("history-populated").iter("node") if n.get("resource-id") == prefix+"history_title"]
    assert titles == ["Bài B kiểm thử", "Bài A kiểm thử"], titles
    shot("history-populated")
    tap("history_open", description="Bài B kiểm thử")
    require("article_title", "Bài B kiểm thử")
    shell("input", "keyevent", "4")
    require("tv_history_count", "Đã đọc: 2 bài")
    passed("History sorted newest first; open correct article; Back returns to history")

    tap("history_remove", description="Xóa Bài B kiểm thử khỏi lịch sử")
    require("tv_history_count", "Đã đọc: 1 bài")
    stop()
    launch()
    tap("historyFragment")
    require("tv_history_count", "Đã đọc: 1 bài")
    passed("Remove one entry persists after force-stop and relaunch")
    tap("btn_clear_history")
    tap("android:id/button2")
    require("tv_history_count", "Đã đọc: 1 bài")
    tap("btn_clear_history")
    rotate(1)
    require("alertTitle", "Xóa toàn bộ lịch sử?")
    shot("clear-confirmation-landscape")
    tap("android:id/button1")
    require("status_title", "Hành trình khám phá")
    assert require("btn_clear_history").get("enabled") == "false"
    rotate(0)
    shot("history-empty")
    passed("Cancel preserves entries; confirmation survives rotation; confirm clears history")

    set_history("broken-json")
    tap("historyFragment")
    require("status_title", "Không thể tải lịch sử")
    tap("status_action")
    require("status_title", "Không thể tải lịch sử")
    shot("history-error")
    tap("btn_clear_history")
    tap("android:id/button1")
    require("status_title", "Hành trình khám phá")
    passed("Corrupt history shows retry and recovers after confirmed reset")

    # Exercise TV4's populated library without claiming the unfinished reader can save articles.
    stop()
    fixture_db = output / "fixture-personal_articles.db"
    if fixture_db.exists():
        fixture_db.unlink()
    with sqlite3.connect(fixture_db) as db:
        for table, timestamp in (("bookmarks", "saved_at"), ("history", "last_read_at")):
            db.execute(f"CREATE TABLE {table} (language TEXT NOT NULL, page_id INTEGER NOT NULL, "
                       f"title TEXT NOT NULL, canonical_url TEXT NOT NULL, {timestamp} INTEGER NOT NULL, "
                       "PRIMARY KEY (language, page_id))")
            for row in fixture:
                db.execute(f"INSERT INTO {table} VALUES (?, ?, ?, ?, ?)",
                           (row["language"], row["pageId"], row["title"], row["canonicalUrl"], row["lastReadAt"]))
        db.execute("PRAGMA user_version=1")
    shell("run-as", package, "mkdir", "-p", "databases")
    for name in database_files:
        shell("run-as", package, "rm", "-f", "databases/" + name)
    adb("shell", "run-as", package, "tee", "databases/personal_articles.db", data=fixture_db.read_bytes())
    launch()
    tap("libraryFragment")
    require("chip_fav_all", "All (2)")
    shot("bookmarks-populated")
    tap(description="Read Bài B kiểm thử")
    require("article_title", "Bài B kiểm thử")
    shell("input", "keyevent", "4")
    tap(description="Remove Bài B kiểm thử from saved articles")
    tap("android:id/button2")
    require("chip_fav_all", "All (2)")
    tap(description="Remove Bài B kiểm thử from saved articles")
    tap("android:id/button1")
    require("chip_fav_all", "All (1)")
    stop()
    launch()
    tap("libraryFragment")
    require("chip_fav_all", "All (1)")
    passed("SQLite bookmarks open correctly; cancel/remove and persistence after restart")
    tap("historyFragment")
    require("tv_history_count", "Đã đọc: 2 bài")
    tap("btn_clear_history")
    tap("android:id/button1")
    require("status_title", "Hành trình khám phá")
    tap("libraryFragment")
    require("chip_fav_all", "All (1)")
    passed("Clearing SQLite history preserves bookmarks")

    tap("searchFragment")
    tap("query_input")
    shell("input", "text", "Ha%sNoi")
    shell("input", "keyevent", "4")
    rotate(1)
    require("query_input", "Ha Noi")
    rotate(0)
    require("query_input", "Ha Noi")
    # Real Volley/API path: opening a search hit still leads to the prototype reader.
    for _ in range(12):
        root = tree("search-live")
        result_node = find(root, "result_title")
        if result_node is not None:
            break
        time.sleep(1)
    else:
        raise AssertionError("Live Wikipedia search did not return results")
    title = result_node.get("text")
    require("btn_load_more")
    tap("btn_load_more")
    for _ in range(12):
        if require("btn_load_more").get("enabled") == "true":
            break
        time.sleep(1)
    else:
        raise AssertionError("Live pagination did not finish")
    assert find(tree("search-page-two"), "btn_load_more").get("text") != "Thử lại"
    shot("search-results")
    passed("Live Wikipedia search and explicit load-more complete successfully")
    tap("result_title", text=title)
    require("article_title", title)
    shell("input", "keyevent", "4")
    require("query_input", "Ha Noi")
    passed("Search query survives rotation and article Back")
    for tab in ("libraryFragment", "historyFragment", "settingsFragment", "searchFragment"):
        tap(tab)
    require("query_input", "Ha Noi")
    passed("All four tabs open and preserve search state")

    tap("result_title", text=title)
    require("article_title", title)
    shell("input", "keyevent", "3")
    # ActivityManager may protect a recently foreground process briefly; kill is asynchronous too.
    for _ in range(15):
        time.sleep(1)
        shell("am", "kill", package)
        time.sleep(.5)
        if not adb("shell", "pidof", package, check=False).stdout.strip():
            break
    else:
        raise AssertionError("Background process did not stop")
    launch()
    require("article_title", title)
    shell("input", "keyevent", "4")
    require("query_input", "Ha Noi")
    passed("Background process kill restores prototype article and query on return")
    crash_after = adb("logcat", "-b", "crash", "-d").stdout
    (output / "crash-log.txt").write_bytes(crash_after)
    assert crash_after == crash_before or package.encode() not in crash_after[len(crash_before):], "New app crash detected"
    passed("No new app crash in logcat")
except Exception as error:
    failure = str(error)
    report["failure"] = failure
    shot("failure")
    print("FAIL:", failure, flush=True)
finally:
    try:
        stop()
        shell("run-as", package, "mkdir", "-p", "shared_prefs", "databases")
        for name, content in backup.items():
            if content is None:
                shell("run-as", package, "rm", "-f", name)
            else:
                adb("shell", "run-as", package, "tee", name, data=content)
        for key, value in rotation.items():
            if value == "null":
                shell("settings", "delete", "system", key)
            else:
                shell("settings", "put", "system", key, value)
        for name, content in backup.items():
            result = adb("exec-out", "run-as", package, "cat", name, check=False)
            if content is None:
                assert result.returncode != 0, "Unexpected restored file: " + name
            else:
                assert result.returncode == 0 and result.stdout == content, "Restore mismatch: " + name
        launch()
        report["historyAndRotationRestored"] = True
        report["sqliteAndPreferencesRestored"] = True
    except Exception as error:
        report["restoreError"] = str(error)
        failure = failure or str(error)
    report["result"] = "FAIL" if failure else "PASS"
    (output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
if failure:
    raise SystemExit(1)
