#!/usr/bin/env python3
"""Validate the produced mod archive and record its actual SHA-256 (no game launch)."""
import hashlib
import json
import sys
import zipfile
from pathlib import Path

artifact = Path(sys.argv[1])
with zipfile.ZipFile(artifact) as archive:
    names = set(archive.namelist())
    metadata = json.loads(archive.read('fabric.mod.json'))
    assert metadata['id'] == 'castleforge', metadata
    assert metadata['version'] == '0.3.0-alpha.2', metadata
    assert metadata['environment'] == '*', metadata
    assert '26.1.2' in str(metadata['depends']['minecraft']), metadata
    for name in ('dev/castleforge/core/CastlePlanner.class',
                 'dev/castleforge/core/TerracedCastlePlanner.class',
                 'dev/castleforge/core/architecture/ReferenceCastlePlanner.class',
                 'dev/castleforge/core/architecture/QualityChecks.class',
                 'dev/castleforge/fabric/ReferencePalette.class'):
        assert name in names, f'Missing runtime class: {name}'
    assert not any(n.startswith('net/minecraft/') for n in names), 'Minecraft binaries must not be bundled'
    assert not any(n.endswith('Verification.class') for n in names), 'Tests must not be bundled'
digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
record = f'{digest}  {artifact.name}\n'
Path('build').mkdir(exist_ok=True)
Path('build/JAR_SHA256.txt').write_text(record, encoding='utf-8')
print('PASS artifact metadata, runtime classes, asset boundaries')
print(record, end='')
