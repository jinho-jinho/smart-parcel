import json
from pathlib import Path
import tempfile
import unittest
import urllib.error
from device_simulator import Client, Spool, enqueue_decision, flush, sample_image


CONFIG = {"id": 1, "rules": [{"id": 2, "priority": 1, "inputType": "TEXT",
                             "inputValue": "K1S", "chuteId": 3}]}


class SimulatorTests(unittest.TestCase):
    def test_restart_retains_exact_event_id_payload_and_image(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "spool.sqlite3"
            spool = Spool(path, "device-A")
            spool.config(CONFIG)
            event = enqueue_decision(spool, sample_image())
            before = dict(spool.head())
            spool.db.close()
            reopened = Spool(path, "device-A")
            self.assertEqual(before, dict(reopened.head()))
            self.assertEqual(event["eventId"], reopened.head()["event_id"])
            self.assertEqual(sample_image(), reopened.head()["image"])
            reopened.db.close()

    def test_lost_ack_retries_original_id_and_preserves_following_events(self):
        class Server:
            def __init__(self):
                self.seen = set()
                self.calls = 0
            def send(self, row):
                self.calls += 1
                event = json.loads(row["payload"])
                duplicate = event["eventId"] in self.seen
                self.seen.add(event["eventId"])
                if self.calls == 1:
                    raise TimeoutError("Committed but response lost")
                return {"eventId": event["eventId"], "attemptId": event.get("attemptId"), "duplicate": duplicate}
        with tempfile.TemporaryDirectory() as directory:
            spool = Spool(Path(directory) / "spool.sqlite3", "A")
            spool.config(CONFIG)
            first = enqueue_decision(spool, sample_image())
            server = Server()
            self.assertFalse(flush(spool, server))
            self.assertEqual("PENDING", spool.head()["status"])
            self.assertEqual(first["eventId"], spool.head()["event_id"])
            spool.requeue(first["eventId"])
            self.assertTrue(flush(spool, server))
            self.assertEqual(1, len(server.seen))
            spool.db.close()

    def test_content_conflict_is_retained_for_review(self):
        class Conflict:
            def send(self, row):
                raise urllib.error.HTTPError("http://localhost", 409, "Conflict", {}, None)
        with tempfile.TemporaryDirectory() as directory:
            spool = Spool(Path(directory) / "spool.sqlite3", "A")
            spool.config(CONFIG)
            first = enqueue_decision(spool, sample_image())
            enqueue_decision(spool, sample_image())
            self.assertFalse(flush(spool, Conflict()))
            self.assertEqual("BLOCKED", spool.head()["status"])
            self.assertEqual(first["eventId"], spool.head()["event_id"])
            self.assertEqual(2, len(spool.statuses()))
            spool.db.close()


if __name__ == "__main__":
    unittest.main()
