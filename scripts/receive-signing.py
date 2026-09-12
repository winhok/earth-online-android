#!/usr/bin/env python3
"""Run-scoped encrypted handoff of the EXISTING Android signing identity.

Only the runner's public recipient certificate and ciphertext are transported.
The runner's ephemeral private key and decrypted owner material never enter Git,
artifacts, command-line password arguments, or public logs.
"""
from __future__ import annotations
import base64
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import tarfile
import time
from pathlib import Path

EXPECTED_CERT='447baf2063785d0e45d1f267c8b8a3fb1904f7b33d88bef6cf0f922aa0b017bd'
PRIVATE=Path(os.environ['RUNNER_TEMP'])/'v15-private-signing'
PUBLIC=Path(os.environ['RUNNER_TEMP'])/'v15-public-recipient'
RUN=os.environ['GITHUB_RUN_ID']
SOURCE=os.environ['SOURCE_COMMIT']
REPO=os.environ['GITHUB_REPOSITORY']
BRANCH=os.environ['HANDOFF_REF']


def run(*args,**kwargs):
    return subprocess.run(args,check=True,capture_output=True,**kwargs)


def prepare():
    os.umask(0o077)
    PRIVATE.mkdir(mode=0o700,exist_ok=False)
    PUBLIC.mkdir(mode=0o700,exist_ok=False)
    run('openssl','req','-x509','-newkey','rsa:3072','-nodes','-days','1',
        '-subj','/CN=Earth Online ephemeral signing recipient',
        '-keyout',str(PRIVATE/'recipient.key'),'-out',str(PUBLIC/'recipient.crt'))
    (PUBLIC/'request.json').write_text(json.dumps(dict(repository=REPO,run_id=RUN,source_commit=SOURCE,
        certificate_sha256=hashlib.sha256((PUBLIC/'recipient.crt').read_bytes()).hexdigest()),indent=2))
    print('Created a public recipient for this exact workflow run and source commit.')


def get(endpoint):
    p=subprocess.run(['gh','api',endpoint],capture_output=True,timeout=40)
    if p.returncode:return None
    return json.loads(p.stdout)


def receive():
    os.umask(0o077)
    assert (PRIVATE/'recipient.key').is_file()
    deadline=time.monotonic()+18*60
    pointer=None
    while time.monotonic()<deadline:
        response=get(f'repos/{REPO}/contents/release/handoff-{RUN}.json?ref={BRANCH}')
        if response is not None:
            pointer=json.loads(base64.b64decode(response['content']));break
        time.sleep(8)
    assert pointer is not None,'No encrypted signing handoff received within this run; no signing identity was generated.'
    assert pointer['run_id']==RUN and pointer['source_commit']==SOURCE and pointer['repository']==REPO
    blob=pointer['cipher_blob'];digest=pointer['cipher_sha256']
    assert re.fullmatch('[0-9a-f]{40}',blob) and re.fullmatch('[0-9a-f]{64}',digest)
    raw=get(f'repos/{REPO}/git/blobs/{blob}')
    assert raw is not None
    cipher=base64.b64decode(raw['content']);assert len(cipher)<=128*1024
    assert hashlib.sha256(cipher).hexdigest()==digest
    assert hashlib.sha1(f'blob {len(cipher)}\0'.encode()+cipher).hexdigest()==blob
    (PRIVATE/'handoff.cms').write_bytes(cipher)
    run('openssl','cms','-decrypt','-binary','-inform','DER','-in',str(PRIVATE/'handoff.cms'),
        '-recip',str(PUBLIC/'recipient.crt'),'-inkey',str(PRIVATE/'recipient.key'),
        '-out',str(PRIVATE/'owner-material.tar.gz'))
    with tarfile.open(PRIVATE/'owner-material.tar.gz','r:gz') as archive:
        assert set(archive.getnames())=={'release.p12','credentials.json'}
        for entry in archive.getmembers():
            assert entry.isfile() and entry.size<=64*1024
            data=archive.extractfile(entry);assert data is not None
            (PRIVATE/entry.name).write_bytes(data.read())
    (PRIVATE/'owner-material.tar.gz').unlink()
    credentials=json.loads((PRIVATE/'credentials.json').read_text())
    assert credentials['application_id']=='xyz.winhok.earthonline'
    assert re.fullmatch('[A-Za-z0-9._-]{1,64}',credentials['alias'])
    values=dict(ANDROID_KEYSTORE_PATH=str(PRIVATE/'release.p12'),
        ANDROID_KEYSTORE_PASSWORD=credentials['store_password'],
        ANDROID_KEY_PASSWORD=credentials['key_password'],ANDROID_KEY_ALIAS=credentials['alias'])
    for key,value in values.items():
        assert isinstance(value,str) and '\n' not in value and '\r' not in value and value
        if key.endswith('PASSWORD'):print('::add-mask::'+value,flush=True)
    env=os.environ|values
    run('keytool','-exportcert','-keystore',values['ANDROID_KEYSTORE_PATH'],
        '-storepass:env','ANDROID_KEYSTORE_PASSWORD','-alias',values['ANDROID_KEY_ALIAS'],
        '-file',str(PRIVATE/'signing-certificate.der'),env=env)
    assert hashlib.sha256((PRIVATE/'signing-certificate.der').read_bytes()).hexdigest()==EXPECTED_CERT, 'Refusing a different signing identity'
    with open(os.environ['GITHUB_ENV'],'a') as f:
        for key,value in values.items():f.write(f'{key}={value}\n')
    (PRIVATE/'credentials.json').unlink()
    print('Verified the original v1.0 signing certificate; no replacement identity created.')


if __name__=='__main__':
    assert re.fullmatch('[0-9a-f]{40}',SOURCE) and RUN.isdigit()
    assert BRANCH=='release/1.5.0-completion'
    if sys.argv[1]=='prepare':prepare()
    elif sys.argv[1]=='receive':receive()
    else:raise SystemExit('Use prepare or receive')
