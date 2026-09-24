#!/usr/bin/env python3
"""Create two independent demo belts using the authenticated management API."""
import argparse
import json
import os
from pathlib import Path
import sys
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=os.environ.get("PARCEL_BASE_URL", "http://127.0.0.1:8080"))
    parser.add_argument("--output", type=Path, default=Path(".local/demo-devices.json"))
    parser.add_argument("--signup", action="store_true", help="Create a new manager account first")
    args = parser.parse_args()
    token = os.environ.get("PARCEL_ADMIN_TOKEN")

    def call(method, path, body=None):
        data = None if body is None else json.dumps(body).encode()
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        req = urllib.request.Request(args.base_url.rstrip("/") + path, data=data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=20) as result:
            content = result.read()
            return json.loads(content) if content else None

    if not token:
        email, password = os.environ.get("PARCEL_EMAIL"), os.environ.get("PARCEL_PASSWORD")
        if not email or not password:
            parser.error("Set PARCEL_ADMIN_TOKEN or PARCEL_EMAIL and PARCEL_PASSWORD")
        if args.signup:
            call("POST", "/api/auth/signup", {"email": email, "password": password, "name": "Demo manager", "role": "MANAGER"})
        result = call("POST", "/api/auth/login", {"email": email, "password": password})
        token = result["data"]["accessToken"]
    output = []
    suffix = uuid.uuid4().hex[:8]
    for label, value, angle in [("A", "K1S", 45), ("B", "K2T", 90)]:
        belt = call("POST", "/api/v2/belts", {"code": "DEMO-" + label + "-" + suffix, "name": "Demo belt " + label})["id"]
        chute = call("POST", f"/api/v2/belts/{belt}/chutes", {"code": "C1", "name": "Destination " + label})["id"]
        version = call("POST", f"/api/v2/belts/{belt}/versions", {
            "groupName": "Demo", "chutes": [{"chuteId": chute, "servoDeg": angle}],
            "rules": [{"name": "Text rule", "priority": 1, "inputType": "TEXT",
                       "inputValue": value, "itemName": "Item " + label, "chuteId": chute}]})["id"]
        call("POST", f"/api/v2/belts/{belt}/versions/{version}/publish")
        call("PUT", f"/api/v2/belts/{belt}/active-version", {"versionId": version, "expectedVersionId": None})
        device = call("POST", f"/api/v2/belts/{belt}/devices", {"code": "DEMO-" + label + "-" + suffix})
        output.append({"label": label, **device})
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(output, indent=2), encoding="utf-8")
    print(f"Created belts A/B. Device credentials saved locally in {args.output}; do not commit this file.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
