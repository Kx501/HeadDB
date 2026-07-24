#!/usr/bin/env python3

import hashlib
import json
import os
import urllib.parse
import urllib.request

MANIFEST_URL = os.environ.get("MANIFEST_URL", "https://data.headsdb.com/manifest.json")
USER_AGENT = "HeadDB-Remote-Health/1.0"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def read_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        require(response.status == 200, f"{url} returned HTTP {response.status}")
        return response.read()


def read_json(url: str) -> dict:
    return json.loads(read_bytes(url).decode("utf-8"))


def validate_integrity(url: str, integrity: dict) -> None:
    payload = read_bytes(url)
    expected_bytes = integrity["bytes"]
    expected_digest = integrity["digest"].lower()
    actual_digest = hashlib.sha256(payload).hexdigest()

    require(len(payload) == expected_bytes, f"{url} has {len(payload)} bytes, expected {expected_bytes}")
    require(actual_digest == expected_digest, f"{url} SHA-256 mismatch")


def main() -> None:
    manifest = read_json(MANIFEST_URL)

    require(manifest.get("schema") == 1, "manifest schema must be 1")
    require(manifest.get("id") == "heads", "manifest id must be heads")
    require(isinstance(manifest.get("revision"), int), "manifest revision must be an integer")
    require(isinstance(manifest.get("timestamp"), str) and manifest["timestamp"], "manifest timestamp is required")
    require(isinstance(manifest.get("mirrors"), list) and manifest["mirrors"], "manifest mirrors are required")
    require(isinstance(manifest.get("resources"), dict), "manifest resources are required")

    mirrors = sorted(manifest["mirrors"], key=lambda mirror: mirror.get("priority", 0))

    for mirror in mirrors:
        require(isinstance(mirror, dict), "each mirror must be an object")
        require(isinstance(mirror.get("id"), str) and mirror["id"], "mirror id is required")
        require(isinstance(mirror.get("url"), str) and mirror["url"], f"mirror {mirror.get('id')} URL is required")

    for resource_id in ("catalog", "revocations"):
        resource = manifest["resources"].get(resource_id)
        require(isinstance(resource, dict), f"{resource_id} resource is required")
        index = resource.get("index")
        require(isinstance(index, dict), f"{resource_id} index is required")
        require(isinstance(index.get("path"), str) and index["path"], f"{resource_id} index path is required")
        integrity = index.get("integrity")
        require(isinstance(integrity, dict), f"{resource_id} index integrity is required")
        require(integrity.get("algorithm") == "sha256", f"{resource_id} integrity algorithm must be sha256")
        require(isinstance(integrity.get("digest"), str) and len(integrity["digest"]) == 64, f"{resource_id} digest must be a SHA-256 hex string")
        require(all(character in "0123456789abcdefABCDEF" for character in integrity["digest"]), f"{resource_id} digest must be hexadecimal")
        require(isinstance(integrity.get("bytes"), int) and integrity["bytes"] >= 0, f"{resource_id} bytes must be non-negative")

        for mirror in mirrors:
            artifact_url = urllib.parse.urljoin(mirror["url"], index["path"])
            validate_integrity(artifact_url, integrity)
            print(f"Validated {resource_id} index on mirror {mirror['id']}: {artifact_url}")

    print(f"Validated manifest revision {manifest['revision']} from {MANIFEST_URL}")


if __name__ == "__main__":
    main()
