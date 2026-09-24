#!/usr/bin/env python3
"""Hardware-free device client; Python standard library only."""
import argparse
import datetime as dt
import json
import os
from pathlib import Path
import random
import sqlite3
import struct
import sys
import time
import urllib.error
import urllib.request
import uuid
import zlib


def now():
    return dt.datetime.now(dt.timezone.utc).isoformat()


def sample_image():
    """A valid 2x2 PNG, for testing image uploads without a camera."""
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 2, 2, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"\x00" + b"\xff\x00\x00" * 2 + b"\x00" + b"\x00\x00\xff" * 2))
            + chunk(b"IEND", b""))


class Spool:
    def __init__(self, path, device_id):
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(path, timeout=10)
        self.db.row_factory = sqlite3.Row
        self.db.execute("PRAGMA journal_mode=WAL")
        self.db.execute("PRAGMA synchronous=FULL")
        self.db.executescript("""
            CREATE TABLE IF NOT EXISTS metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS events(
              seq INTEGER PRIMARY KEY AUTOINCREMENT, event_id TEXT UNIQUE NOT NULL,
              payload TEXT NOT NULL, image BLOB, status TEXT NOT NULL DEFAULT 'PENDING',
              retries INTEGER NOT NULL DEFAULT 0, next_retry REAL NOT NULL DEFAULT 0, error TEXT);
        """)
        owner = self.db.execute("SELECT value FROM metadata WHERE key='device_id'").fetchone()
        if owner and owner["value"] != device_id:
            raise ValueError("Use a separate spool file for each device")
        with self.db:
            self.db.execute("INSERT OR IGNORE INTO metadata VALUES ('device_id',?)", (device_id,))

    def enqueue(self, payload, image=None):
        with self.db:
            self.db.execute("INSERT INTO events(event_id,payload,image) VALUES (?,?,?)",
                            (payload["eventId"], json.dumps(payload, ensure_ascii=False, separators=(",", ":")), image))
        return payload["eventId"]

    def config(self, value=None):
        if value is not None:
            with self.db:
                self.db.execute("INSERT OR REPLACE INTO metadata VALUES ('configuration',?)", (json.dumps(value),))
        row = self.db.execute("SELECT value FROM metadata WHERE key='configuration'").fetchone()
        if row is None:
            raise ValueError("Run setup at least once before enqueueing decisions")
        return json.loads(row["value"])

    def head(self):
        # A blocked/late decision must not be overtaken by its discharge event.
        return self.db.execute("SELECT * FROM events WHERE status!='ACKED' ORDER BY seq LIMIT 1").fetchone()

    def ack(self, event_id):
        with self.db:
            self.db.execute("UPDATE events SET status='ACKED',error=NULL WHERE event_id=?", (event_id,))

    def fail(self, row, error, blocked=False):
        delay = min(60, 2 ** min(row["retries"], 6)) + random.random()
        with self.db:
            self.db.execute("UPDATE events SET status=?,retries=retries+1,next_retry=?,error=? WHERE event_id=?",
                            ("BLOCKED" if blocked else "PENDING", time.time() + delay, error, row["event_id"]))

    def requeue(self, event_id):
        with self.db:
            result = self.db.execute("UPDATE events SET status='PENDING',next_retry=0,error=NULL WHERE event_id=?", (event_id,))
            if result.rowcount != 1:
                raise ValueError("Unknown event ID")

    def statuses(self):
        return [dict(row) for row in self.db.execute(
            "SELECT event_id,status,retries,error FROM events ORDER BY seq")]


class Client:
    def __init__(self, base_url, device_id, key):
        self.base = base_url.rstrip("/")
        self.headers = {"X-Device-Id": device_id, "X-Device-Key": key}

    def request(self, method, path, data=None, content_type="application/json"):
        headers = dict(self.headers)
        if data is not None:
            headers["Content-Type"] = content_type
        req = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=15) as response:
            body = response.read()
            return json.loads(body) if body else None

    def setup(self):
        result = self.request("GET", "/api/v2/device/setup")
        self.request("PUT", "/api/v2/device/applied-version", json.dumps({"versionId": result["id"]}).encode())
        return result

    def send(self, row):
        boundary = "parcel-" + uuid.uuid4().hex
        body = (("--" + boundary + "\r\nContent-Disposition: form-data; name=\"payload\"\r\n"
                 "Content-Type: application/json\r\n\r\n").encode()
                + row["payload"].encode("utf-8") + b"\r\n")
        if row["image"] is not None:
            body += (("--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"capture.png\"\r\n"
                      "Content-Type: application/octet-stream\r\n\r\n").encode() + row["image"] + b"\r\n")
        body += ("--" + boundary + "--\r\n").encode()
        return self.request("POST", "/api/v2/device/events", body, "multipart/form-data; boundary=" + boundary)


def enqueue_decision(spool, image):
    config = spool.config()
    rule = min(config["rules"], key=lambda r: (r["priority"], r["id"]))
    timestamp = now()
    payload = {"eventId": str(uuid.uuid4()), "attemptId": str(uuid.uuid4()), "eventType": "DECISION",
               "occurredAt": timestamp, "capturedAt": timestamp, "decidedAt": timestamp,
               "ruleVersionId": config["id"], "decisionStatus": "MATCHED",
               "recognizedInputType": rule["input_type"], "recognizedValue": rule["input_value"],
               "ruleId": rule["id"], "chuteId": rule["chute_id"]}
    spool.enqueue(payload, image)
    return payload


def flush(spool, client, wait=False):
    while True:
        row = spool.head()
        if row is None:
            return True
        if row["status"] == "BLOCKED":
            print("Blocked event needs review: " + row["event_id"], file=sys.stderr)
            return False
        delay = row["next_retry"] - time.time()
        if delay > 0:
            if not wait:
                return False
            time.sleep(min(delay, 60))
            continue
        try:
            ack = client.send(row)
            payload = json.loads(row["payload"])
            if ack.get("eventId") != row["event_id"] or ack.get("attemptId") != payload.get("attemptId"):
                raise ValueError("Server acknowledgment does not identify this event")
            spool.ack(row["event_id"])
            print(json.dumps(ack))
        except urllib.error.HTTPError as error:
            # 404 is retryable for dependent events; 429/5xx apply backoff.
            blocked = error.code in (400, 401, 403, 409, 413, 415, 422)
            spool.fail(row, "HTTP " + str(error.code), blocked)
            if wait and not blocked:
                continue
            return False
        except (urllib.error.URLError, TimeoutError, OSError, ValueError) as error:
            spool.fail(row, type(error).__name__)
            if wait:
                continue
            return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=os.environ.get("PARCEL_BASE_URL", "http://127.0.0.1:8080"))
    parser.add_argument("--spool", default=".local/device.sqlite3")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("setup")
    decision = sub.add_parser("enqueue-decision")
    decision.add_argument("--image", type=Path)
    error = sub.add_parser("enqueue-error")
    error.add_argument("--code", default="CAMERA_ERROR")
    error.add_argument("--attempt-id")
    error.add_argument("--image", type=Path)
    discharge = sub.add_parser("enqueue-discharge")
    discharge.add_argument("--attempt-id", required=True)
    discharge.add_argument("--failed", action="store_true")
    discharge.add_argument("--code", default="SERVO_ERROR")
    send = sub.add_parser("flush")
    send.add_argument("--wait", action="store_true")
    retry = sub.add_parser("requeue")
    retry.add_argument("event_id")
    sub.add_parser("status")
    demo = sub.add_parser("demo")
    demo.add_argument("--image", type=Path)
    args = parser.parse_args()
    device_id = os.environ.get("PARCEL_DEVICE_ID")
    if not device_id:
        parser.error("Set PARCEL_DEVICE_ID")
    spool = Spool(args.spool, device_id)
    key = os.environ.get("PARCEL_DEVICE_KEY")
    if args.command in ("setup", "flush", "demo") and not key:
        parser.error("Set PARCEL_DEVICE_KEY")
    client = Client(args.base_url, device_id, key)
    if args.command == "setup":
        config = client.setup()
        spool.config(config)
        print(json.dumps(config, ensure_ascii=False))
    elif args.command in ("enqueue-decision", "demo"):
        if args.command == "demo":
            spool.config(client.setup())
        image = args.image.read_bytes() if args.image else sample_image()
        decision = enqueue_decision(spool, image)
        print(json.dumps({"eventId": decision["eventId"], "attemptId": decision["attemptId"]}))
        if args.command == "demo":
            spool.enqueue({"eventId": str(uuid.uuid4()), "eventType": "DEVICE_ERROR",
                           "occurredAt": now(), "errorCode": "SIMULATED_CAMERA_ERROR"})
            return 0 if flush(spool, client) else 1
    elif args.command in ("enqueue-error", "enqueue-discharge"):
        event_type = "DEVICE_ERROR" if args.command == "enqueue-error" else (
            "DISCHARGE_FAILED" if args.failed else "DISCHARGE_CONFIRMED")
        event = {"eventId": str(uuid.uuid4()), "eventType": event_type, "occurredAt": now()}
        if args.attempt_id:
            event["attemptId"] = str(uuid.UUID(args.attempt_id))
        if event_type != "DISCHARGE_CONFIRMED":
            event["errorCode"] = args.code
        image = args.image.read_bytes() if args.command == "enqueue-error" and args.image else None
        print(spool.enqueue(event, image))
    elif args.command == "flush":
        return 0 if flush(spool, client, args.wait) else 1
    elif args.command == "requeue":
        spool.requeue(args.event_id)
    else:
        print(json.dumps(spool.statuses()))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, OSError, urllib.error.URLError) as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
