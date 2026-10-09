"""Apply reviewed UTF-8 patches; verify both Git blob identities and transport checksums.

The import does not fetch code, execute embedded commands, modify workflows, or force-push.
Compilation/tests and publication are separate explicit workflow steps.
"""
from pathlib import Path
import hashlib
import json
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
STAGE = ROOT / "tools/beta-import"


def blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def path_ok(name: str) -> bool:
    return not Path(name).is_absolute() and ".." not in Path(name).parts and name.startswith(("src/main/java/", "src/main/resources/", "src/test/", "docs/beta/"))


def main() -> None:
    ready = json.loads((STAGE / "READY.json").read_text())
    if ready["version"] != "0.1.0-beta" or ready["platform"] != "1.21.1":
        raise RuntimeError("Unexpected import target")
    if not 1 <= len(ready["parts"]) <= 30:
        raise RuntimeError("Invalid patch count")
    patches = []
    expected = {}
    for name, checksum in sorted(ready["parts"].items()):
        if not re.fullmatch(r"[0-9]{2}\.patch", name):
            raise RuntimeError("Invalid patch filename")
        patch = STAGE / name
        data = patch.read_bytes()
        if len(data) > 100000 or hashlib.sha256(data).hexdigest() != checksum:
            raise RuntimeError(f"Transport mismatch: {name}")
        for section in re.split(r"(?=^diff --git )", data.decode("utf-8"), flags=re.M):
            if not section:
                continue
            match = re.match(r"diff --git a/(\S+) b/(\S+)\n", section)
            index = re.search(r"^index ([0-9a-f]{40})\.\.([0-9a-f]{40})(?: 100644)?$", section, re.M)
            if not match or not index or match[1] != match[2] or not path_ok(match[1]):
                raise RuntimeError("Unsupported patch entry")
            target = ROOT / match[1]
            if match[1] in expected or target.is_symlink():
                raise RuntimeError("Duplicate path or symlink")
            old, new = index.groups()
            if old == "0" * 40:
                if target.exists():
                    raise RuntimeError(f"New file already exists: {match[1]}")
            elif not target.is_file() or blob(target.read_bytes()) != old:
                raise RuntimeError(f"Source changed since review: {match[1]}")
            if new == "0" * 40:
                raise RuntimeError("Deletion not allowed in beta source patches")
            expected[match[1]] = new
        patches.append(str(patch))
    if len(expected) != ready["source_files"]:
        raise RuntimeError("Unexpected source-file count")
    subprocess.run(["git", "apply", "--unidiff-zero", "--check", *patches], cwd=ROOT, check=True)
    subprocess.run(["git", "apply", "--unidiff-zero", *patches], cwd=ROOT, check=True)
    for name, checksum in expected.items():
        if blob((ROOT / name).read_bytes()) != checksum:
            raise RuntimeError(f"Patched source hash mismatch: {name}")

    # Metadata edits preserve newer documentation rather than copying an old source ZIP.
    metadata_path = STAGE / "metadata.json"
    if hashlib.sha256(metadata_path.read_bytes()).hexdigest() != ready["metadata_sha256"]:
        raise RuntimeError("Metadata transport mismatch")
    metadata = json.loads(metadata_path.read_text())
    allowed = {"build.gradle", "gradle.properties", "README.md", "ROADMAP_0.1.md",
        "ROADMAP_TO_1.0.md", "CHANGELOG.md", "CONFIGURATION.md", "DEVELOPMENT_LOG.md",
        "FIELD_LEDGER.md", "FOUNDATIONS_CANNABIS_SOIL_CONTRACT.md", "KUBEJS_EXAMPLE.js",
        "src/main/templates/META-INF/neoforge.mods.toml"}
    for change in metadata:
        name = change["path"]
        target = ROOT / name
        if name not in allowed or target.is_symlink() or blob(target.read_bytes()) != change["old_blob"]:
            raise RuntimeError(f"Metadata changed since review: {name}")
        original = target.read_text()
        mode = change["mode"]
        if mode == "replace_file":
            result = change["text"]
        elif mode == "prepend":
            result = change["text"] + original
        elif mode == "edit":
            result = original
            for before, after in change["replacements"]:
                if result.count(before) != 1:
                    raise RuntimeError(f"Non-unique metadata edit: {name}")
                result = result.replace(before, after, 1)
            result += change.get("append", "")
        else:
            raise RuntimeError("Unknown metadata operation")
        target.write_text(result)
    (ROOT / "build").mkdir(exist_ok=True)
    (ROOT / "build/beta-import-manifest.json").write_text(json.dumps({"version": ready["version"], "source_blobs": expected}, indent=2) + "\n")
    print(f"Verified and applied {len(expected)} source/resource/test/document changes plus {len(metadata)} metadata edits.")


if __name__ == "__main__":
    main()
