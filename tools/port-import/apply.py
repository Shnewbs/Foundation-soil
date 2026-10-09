"""Apply reviewed native migrations in a disposable checkout; no downloads or force-pushes."""
from pathlib import Path
import hashlib
import json
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
STAGE = ROOT / 'tools/port-import'

def digest(data):
    return hashlib.sha256(data).hexdigest()

def blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()

def valid_path(name):
    path = Path(name)
    return not path.is_absolute() and '..' not in path.parts and not name.startswith(('.git/', '.github/', 'tools/port-import/'))

def main():
    ready = json.loads((STAGE / 'READY.json').read_text())
    if ready['platform'] != '26.1.2' or ready['version'] != '0.1.0-beta-port.1':
        raise RuntimeError('Unexpected native target')
    if ready['baseline'] != 'b8740a5de6a19036a5a38950312c16ee4f378e44':
        raise RuntimeError('Unexpected baseline')
    changed = subprocess.check_output(['git', 'diff', '--name-only', ready['baseline'], 'HEAD'], cwd=ROOT, text=True).splitlines()
    allowed_workflows = {'.github/workflows/port-toolchain.yml', '.github/workflows/native-port-release.yml'}
    for name in changed:
        if not name.startswith('tools/port-import/') and name not in allowed_workflows:
            raise RuntimeError('Source changed since review; refusing to overwrite: ' + name)
    if digest((STAGE / 'common.py').read_bytes()) != ready['common_sha256']:
        raise RuntimeError('Common migration transport mismatch')
    records = []
    for name, expected in sorted(ready['parts'].items()):
        if not re.fullmatch(r'[0-9]{2}\.json', name):
            raise RuntimeError('Invalid part filename')
        part = json.loads((STAGE / name).read_text())
        if digest(canonical(part)) != expected:
            raise RuntimeError('Patch transport mismatch: ' + name)
        records.extend(part)
    subprocess.run(['python3', str(STAGE / 'common.py')], cwd=ROOT, check=True)
    seen = set()
    for record in records:
        name = record['path']
        if not valid_path(name) or name in seen:
            raise RuntimeError('Invalid or duplicate path: ' + name)
        seen.add(name)
        target = ROOT / name
        if target.is_symlink():
            raise RuntimeError('Symlink target rejected')
        if record['old'] is None:
            if target.exists():
                raise RuntimeError('New file already exists: ' + name)
            result = record['text'].encode()
        else:
            original = target.read_bytes()
            if blob(original) != record['old']:
                raise RuntimeError('Intermediate source identity mismatch: ' + name)
            lines = original.decode().splitlines(keepends=True)
            previous = len(lines)
            for first, last, text in reversed(record['edits']):
                if not 0 <= first <= last <= previous:
                    raise RuntimeError('Invalid line edit')
                lines[first:last] = text.splitlines(keepends=True)
                previous = first
            result = ''.join(lines).encode()
        if blob(result) != record['new']:
            raise RuntimeError('Final file identity mismatch: ' + name)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(result)
    fingerprint = hashlib.sha256()
    for path in sorted((ROOT / 'src').rglob('*')):
        if path.is_file():
            fingerprint.update(str(path.relative_to(ROOT)).encode() + b'\0' + digest(path.read_bytes()).encode() + b'\n')
    if fingerprint.hexdigest() != ready['source_sha256']:
        raise RuntimeError('Native source tree does not match the locally compiled and tested source')
    print('Native source verified against local compiler/test checkpoint: ' + fingerprint.hexdigest())

if __name__ == '__main__':
    main()
