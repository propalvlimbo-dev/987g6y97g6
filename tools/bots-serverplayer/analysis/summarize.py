#!/usr/bin/env python3
"""Summarize schema-1 JSONL files inside a user-provided dataset ZIP; no raw data is copied."""
import collections
import json
import statistics
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: python3 summarize.py /path/to/datasets.zip')
with zipfile.ZipFile(sys.argv[1]) as archive:
    for name in sorted(n for n in archive.namelist() if n.endswith('.jsonl')):
        rows = [json.loads(line) for line in archive.read(name).splitlines() if line.strip()]
        meta = next((r for r in rows if r.get('type') == 'meta'), {})
        frames = [r for r in rows if r.get('type') == 'frame']
        hits = [r for r in rows if r.get('type') == 'hit' and r.get('direction') == 'out' and not r.get('cancelled')]
        incoming = [r for r in rows if r.get('type') == 'hit' and r.get('direction') == 'in']
        swings = [r for r in rows if r.get('type') == 'swing']
        intervals = [b['tick'] - a['tick'] for a,b in zip(hits, hits[1:])]
        print(f"{meta.get('label','unknown')}: frames={len(frames)}, hits={len(hits)}, swings={len(swings)}, incoming={len(incoming)}, median_hit_interval_ticks={statistics.median(intervals) if intervals else 'n/a'}, damage={dict(collections.Counter(r['damage'] for r in hits))}, targets={dict(collections.Counter('bot' if x['target_is_bot'] else 'player' for x in frames if 'target_is_bot' in x))}")
