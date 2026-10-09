"""Verify the reviewed native checkpoint without touching the retained main branch."""
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
STAGE = ROOT / 'tools/native-maintenance'

def blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

def allowed(path):
    p = Path(path)
    return not p.is_absolute() and '..' not in p.parts and (
        path.startswith(('src/main/java/', 'src/test/java/', 'docs/ports/26.1.2/')) or
        path in {'README.md', 'RELEASING.md', 'CHANGELOG.md', 'build_mod.bat', 'gradle.properties'})

def main():
    if os.environ.get('GITHUB_REF') != 'refs/heads/mc-26.1.2':
        raise RuntimeError('This checkpoint can only modify mc-26.1.2, never main/1.21.1')
    ready = json.loads((STAGE / 'READY.json').read_text())
    if ready['baseline'] != 'c194eb4e83263596c82a46a3320a12f602c52acd' or ready['version'] != '0.1.0-beta-port.2':
        raise RuntimeError('Unexpected source checkpoint')
    delta = subprocess.check_output(['git', 'diff', '--name-only', ready['baseline'], 'HEAD'], cwd=ROOT, text=True).splitlines()
    for name in delta:
        if not name.startswith('tools/native-maintenance/') and name != '.github/workflows/native-maintenance.yml':
            raise RuntimeError('Concurrent source changes require review: ' + name)
    patches, expected = [], {}
    for name, checksum in sorted(ready['parts'].items()):
        if not re.fullmatch(r'[0-9]{2}\.patch', name):
            raise RuntimeError('Invalid patch name')
        path = STAGE / name
        content = path.read_bytes()
        if len(content) > 100000 or hashlib.sha256(content).hexdigest() != checksum:
            raise RuntimeError('Transport checksum mismatch: ' + name)
        for section in re.split(r'(?=^diff --git )', content.decode(), flags=re.M):
            if not section:
                continue
            entry = re.match(r'diff --git a/(\S+) b/(\S+)\n', section)
            index = re.search(r'^index ([0-9a-f]{40})\.\.([0-9a-f]{40})(?: 100644)?$', section, re.M)
            if not entry or not index or entry[1] != entry[2] or not allowed(entry[1]):
                raise RuntimeError('Unsupported source patch')
            target = ROOT / entry[1]
            if entry[1] in expected or target.is_symlink():
                raise RuntimeError('Duplicate or symlink source')
            old, new = index.groups()
            if old == '0' * 40:
                if target.exists():
                    raise RuntimeError('New file already exists: ' + entry[1])
            elif not target.is_file() or blob(target.read_bytes()) != old:
                raise RuntimeError('Source identity mismatch: ' + entry[1])
            if new == '0' * 40:
                raise RuntimeError('Source deletions are not part of this checkpoint')
            expected[entry[1]] = new
        patches.append(str(path))
    if len(expected) != ready['files']:
        raise RuntimeError('Unexpected source file count')
    subprocess.run(['git', 'apply', '--check', *patches], cwd=ROOT, check=True)
    subprocess.run(['git', 'apply', *patches], cwd=ROOT, check=True)
    for name, checksum in expected.items():
        if blob((ROOT / name).read_bytes()) != checksum:
            raise RuntimeError('Applied source identity mismatch: ' + name)
    print(f'Verified {len(expected)} native source/doc/test changes. No 1.21.1 writes are performed.')

if __name__ == '__main__':
    main()
