"""Validate a platform-specific release tag against the checked-out source."""
import os
import re
from pathlib import Path


def metadata(tag, properties):
    version = properties['mod_version']
    minecraft = properties['minecraft_version']
    expected = f'mc-{minecraft}-v{version}'
    if tag != expected:
        raise ValueError(f'Tag must be {expected!r} for this source, got {tag!r}')
    if minecraft not in {'1.21.1', '26.1.2', '26.3', '26.4'}:
        raise ValueError(f'Unsupported release platform: {minecraft}')
    if not re.fullmatch(r'[0-9]+(?:\.[0-9]+)*(?:[ab](?:\.R[0-9]+)?|-(?:alpha|beta|rc)(?:[.-][0-9A-Za-z]+)*)?', version):
        raise ValueError(f'Unrecognized version format: {version}')
    return {'minecraft': minecraft, 'version': version,
            'java': properties['java_version'],
            'prerelease': str(bool(re.search(r'[ab]|rc', version))).lower(),
            'title': f'Foundations Soil {minecraft} — {version}'}


if __name__ == '__main__':
    props = dict(line.strip().split('=', 1) for line in Path('gradle.properties').read_text().splitlines()
                 if '=' in line and not line.lstrip().startswith('#'))
    result = metadata(os.environ['RELEASE_TAG'], props)
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        for key, value in result.items():
            print(f'{key}={value}', file=output)
