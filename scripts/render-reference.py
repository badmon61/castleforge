#!/usr/bin/env python3
"""Orthographic technical views of ReferenceExport TSV. No Minecraft assets.

Requires Python 3.10+ and Pillow. Shapes are the explicitly supported primitive
subset (cubes, straight stairs, bottom slabs, hanging lanterns), not Minecraft's
renderer. This checks composition and clearances, NOT lighting/physics/textures.
"""
import argparse
import csv
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

COLORS = {
    'FOUNDATION': '#6b6962', 'STONE': '#a8a28f', 'TRIM': '#cec7b3',
    'TIMBER': '#614733', 'PLANKS': '#a08055', 'ROOF': '#424f58',
    'GLASS': '#86b3c0', 'BOOKSHELF': '#796340', 'WOOL': '#8d3d37',
    'STONE_STAIRS': '#b3aa94', 'WOOD_STAIRS': '#a08055',
    'ROOF_STAIRS': '#424f58', 'SLAB': '#a08055',
    'ROOF_SLAB': '#424f58', 'LANTERN': '#ffd68c',
    'PLASTER': '#e2dac2', 'PAVING': '#8d8a81', 'GARDEN': '#607d43',
    'TABLE_TOP': '#a08055', 'STONE_SLAB': '#b3aa94',
}


def boxes(kind, facing):
    if kind.endswith('_STAIRS'):
        upper = {'NORTH': (0, .5, 0, 1, 1, .5),
                 'SOUTH': (0, .5, .5, 1, 1, 1),
                 'EAST': (.5, .5, 0, 1, 1, 1),
                 'WEST': (0, .5, 0, .5, 1, 1)}[facing]
        return [(0, 0, 0, 1, .5, 1), upper]
    if kind in ('SLAB', 'ROOF_SLAB', 'STONE_SLAB'):
        return [(0, 0, 0, 1, .5, 1)]
    if kind == 'TABLE_TOP':
        return [(0, .5, 0, 1, 1, 1)]
    if kind == 'LANTERN':
        return [(.3, .15, .3, .7, .65, .7), (.45, .65, .45, .55, 1, .55)]
    return [(0, 0, 0, 1, 1, 1)]


def rgb(hexcolor, light=1):
    return tuple(min(255, int(int(hexcolor[i:i+2], 16)*light)) for i in (1, 3, 5))


def font(size):
    for name in ('DejaVuSans.ttf', 'C:/Windows/Fonts/arial.ttf'):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default()


def render(args):
    cells = []
    with open(args.input, encoding='utf-8') as source:
        for row in csv.DictReader(source, delimiter='\t'):
            x, y, z = (int(row[k]) for k in ('x', 'y', 'z'))
            if row['kind'] == 'AIR' or y > args.cut_y:
                continue
            if args.crop and not (args.crop[0] <= x <= args.crop[2] and args.crop[1] <= z <= args.crop[3]):
                continue
            cells.append((x, y, z, row['kind'], row['facing']))
    if not cells:
        raise ValueError('No blocks in selected view')
    occupied = {(x, y, z): kind for x, y, z, kind, _ in cells}
    sx, sz = (1, 1) if args.turn == 0 else (-1, -1)
    angle = math.radians(args.elevation)

    def project(p):
        x, y, z = p
        if args.top:
            return x, z
        return (sx*x-sz*z)*.7071, (sx*x+sz*z)*.7071*math.sin(angle)-y*math.cos(angle)

    faces = []
    for x, y, z, kind, facing in cells:
        color = COLORS[kind]
        for x1, y1, z1, x2, y2, z2 in boxes(kind, facing):
            x1 += x; x2 += x; y1 += y; y2 += y; z1 += z; z2 += z
            candidates = [([(x1,y2,z1),(x2,y2,z1),(x2,y2,z2),(x1,y2,z2)], 1.10, (0,1,0))]
            if not args.top:
                xx = x2 if sx == 1 else x1
                zz = z2 if sz == 1 else z1
                candidates += [([(xx,y1,z1),(xx,y1,z2),(xx,y2,z2),(xx,y2,z1)], .72, (sx,0,0)),
                               ([(x1,y1,zz),(x2,y1,zz),(x2,y2,zz),(x1,y2,zz)], .9, (0,0,sz))]
            for vertices, light, normal in candidates:
                neighbor = occupied.get((x+normal[0], y+normal[1], z+normal[2]))
                # Cull only complete outside cube faces. Partial stair/slab faces stay.
                if kind not in ('SLAB','ROOF_SLAB','LANTERN','TABLE_TOP','STONE_SLAB') and not kind.endswith('_STAIRS'):
                    if neighbor and neighbor not in ('SLAB','ROOF_SLAB','LANTERN','TABLE_TOP','STONE_SLAB') and not neighbor.endswith('_STAIRS'):
                        continue
                depth = sum((sx*v[0]+sz*v[2])*math.cos(angle)+v[1]*math.sin(angle)*1.4142 for v in vertices)/4
                if args.top:
                    depth = y2
                faces.append((depth, [project(v) for v in vertices], rgb(color, light)))
    allpoints = [p for _, polygon, _ in faces for p in polygon]
    minx = min(p[0] for p in allpoints); maxx = max(p[0] for p in allpoints)
    miny = min(p[1] for p in allpoints); maxy = max(p[1] for p in allpoints)
    width, height, margin, header = 1800, 1260, 80, 130
    scale = min((width-margin*2)/(maxx-minx), (height-header-margin*2)/(maxy-miny))
    offx = (width-(maxx-minx)*scale)/2
    offy = header+(height-header-(maxy-miny)*scale)/2
    image = Image.new('RGB', (width,height), '#eeeae2')
    draw = ImageDraw.Draw(image)
    for depth, polygon, color in sorted(faces, key=lambda f:f[0]):
        points = [(offx+(p[0]-minx)*scale,offy+(p[1]-miny)*scale) for p in polygon]
        if args.top and depth > 1:
            color = tuple(int(c*.84) for c in color)
        draw.polygon(points, fill=color)
        if args.top:
            draw.line(points+[points[0]], fill=tuple(int(c*.88) for c in color), width=1)
    draw.text((55,30), args.title, font=font(34), fill='#242d31')
    draw.text((55,82), 'CastleForge 0.3 | Generated block geometry | No Minecraft textures, lighting or physics', font=font(20), fill='#555d60')
    draw.text((55,height-40), f'{len(cells):,} visible-plan cells | straight stair/slab primitives | seed/position: see adjacent evidence record', font=font(18), fill='#555d60')
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    image.save(args.output)
    print(f'{args.output}: {len(cells)} cells, {len(faces)} faces')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input'); parser.add_argument('output')
    parser.add_argument('--title', default='Falkenwacht / reference geometry')
    parser.add_argument('--turn', type=int, choices=(0,2), default=0)
    parser.add_argument('--elevation', type=float, default=30)
    parser.add_argument('--top', action='store_true')
    parser.add_argument('--cut-y', type=int, default=10000)
    parser.add_argument('--crop', nargs=4, type=int)
    render(parser.parse_args())
