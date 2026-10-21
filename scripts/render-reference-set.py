#!/usr/bin/env python3
"""Reproduce reference geometry views, including the elevated defensive circuit."""
import argparse
import importlib.util
import shutil
from pathlib import Path
from types import SimpleNamespace

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('source', type=Path)
parser.add_argument('destination', type=Path)
parser.add_argument('--label', default='Falkenwacht v3')
args = parser.parse_args()
spec = importlib.util.spec_from_file_location('geometry_renderer', Path(__file__).with_name('render-reference.py'))
renderer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(renderer)
args.destination.mkdir(parents=True, exist_ok=True)
views = [
    ('southeast', 'Falkenwacht v2 / Southeast', {}),
    ('northwest', 'Falkenwacht v2 / Northwest', {'turn': 2}),
    ('ground-plan', 'Falkenwacht v2 / Ground floor', {'top': True, 'cut_y': 3}),
    ('gate', 'Falkenwacht v2 / Gate approach', {'crop': [-12,27,12,41], 'elevation': 15}),
    ('hall-section', 'Falkenwacht v2 / Hall section', {'crop': [-19,-29,16,-12], 'cut_y': 13}),
    ('hall-furniture', 'Falkenwacht v2 / Hall furniture', {'crop': [-19,-29,16,-12], 'cut_y': 3}),
    ('residence-plan', 'Falkenwacht v2 / First-floor chambers', {'crop': [18,-30,36,3], 'cut_y': 11, 'top': True}),
    ('residence-facade', 'Falkenwacht v2 / Residential facade', {'crop': [18,-30,38,3], 'elevation': 15}),
    ('stair-section', 'Falkenwacht v2 / Keep staircase section', {'crop': [-38,-29,-28,-14], 'cut_y': 24}),
    ('wallwalk-plan', 'Falkenwacht v2 / Elevated circuit, cut at Y=7', {'top': True, 'cut_y': 7}),
    ('court-stair', 'Falkenwacht v2 / Court stair and parapets', {'crop': [-49,12,-37,29], 'cut_y': 12}),
    ('gate-link', 'Falkenwacht v2 / Raised gatehouse crossing', {'crop': [-17,27,17,41], 'cut_y': 11, 'top': True}),
    ('tower-crossing', 'Falkenwacht v2 / Northeast tower crossing', {'crop': [39,-40,49,-29], 'cut_y': 10}),
]
for name,title,options in views:
    title=title.replace('Falkenwacht v2', args.label)
    parameters = dict(input=args.source/'blocks.tsv', output=args.destination/f'{name}.png',
                      title=title, top=False, turn=0, elevation=30, cut_y=10000, crop=None)
    parameters.update(options)
    renderer.render(SimpleNamespace(**parameters))
shutil.copyfile(args.source/'model.txt', args.destination/'model.txt')
