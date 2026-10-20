# Falkenwacht reference / 0.3.0-alpha.1

An experimental, independently authored reference castle. This is the start of
the new architecture direction, not a final procedural castle library or an
approved visual design. No schematics, external templates or game assets are used.

## Layers

| Layer | Responsibility |
|---|---|
| ArchitectureModel | Typed masses, room functions, floor levels, connections, roofs, windows, stair flights and route reservations, in canonical local coordinates |
| Module library | Authored composition and block construction for the keep, hall, residence, service wing, gate, corner towers and curtains |
| Interior designer | Furniture, bookshelves, seating, timber, decorative hearths and supported lights with circulation respected |
| Owned block buffer | Module ownership and explicit base interfaces; cross-owner solid writes/carving fail instead of silently replacing another module |
| QualityChecks | Structural/model validation and final checks after decoration, including intended routes and directed stairs |
| ReferenceCastlePlanner | Bounded terrain input, separate variant signature, deterministic site orientation and translation to world blocks |
| Fabric reference preview | Tick-sliced snapshot/preflight, bounded pure planning worker, existing protected confirmation and placement/undo jobs |

The model uses integer blocks. Floor Y means the supporting floor block, so room
targets stand one block above it. Stair treads are ordered bottom to top; the
renderer and Fabric palette agree on facing and bottom stair/slab halves.
Canonical model coordinates are independent of the seed-selected world rotation.

## How to try the reference

Use a **separate disposable Creative world**, initially superflat, with cheats.
Keep the pinned Minecraft Java 26.1.2, Java 25, Fabric Loader 0.19.5+ and
Fabric API 0.155.3+26.1.2 environment. Install only the runtime
`castleforge-fabric-0.3.0-alpha.1.jar` from a successful branch build, not a
sources JAR and not two CastleForge versions simultaneously.

```mcfunction
/castleforge preview reference
/castleforge status
```

The default centre is 58 blocks north of the command source. An explicit site
uses `/castleforge preview reference <x> <z>`. The 109 x 109 terrain survey must
already be loaded. The orientation depends on seed and position; the material
study remains central-European even in another culture region.

Preparation takes several ticks and does not place blocks. Wait for
`PREVIEW READY`, then:

```mcfunction
/castleforge inspect
/castleforge build confirm
/castleforge status
```

Confirm within 120 seconds after readiness and remain near the site. Inspect
reports the design signature, dimensions, rooms, stair flights, route checks,
rotation, relief, edits and timings. It describes the plan, not a rendered ghost.
At most one large reference preview is retained per server session. A pending
reference request belongs to its operator; a disconnected or dimension-changed
operator cannot later publish or reuse that request.

`/castleforge cancel` cancels preparation or discards the operator's ready
reference preview. During placement it stops further edits, leaving a partial
castle. `/castleforge undo` reverts this session's most recent build, skipping
conflicting later edits. It is in-memory, is lost on shutdown and does not journal
all neighboring physics effects. It is not a world backup.

Legacy controls remain `/castleforge preview [x z]` and
`/castleforge preview terraced [x z]`. No automatic placement is registered.

## Deliberate boundaries

- One maintained, authored layout; not eight cultural architectural grammars,
  historical reconstruction, arbitrary castle diversity or ruin simulation.
- The residential wing still has generous open storeys; finer interior zoning,
  richer courtyard composition and more individual facade/roof details need a
  later visual iteration. A complete accessible wall-walk system is not included:
  corner-tower guardrooms are targets, curtain tops and roofs are not main rooms.
- Shallow, dry, open terrain only. Difficult slopes and unknown/protected ground
  are rejected; maximum survey relief is three blocks, and a rough approach can
  be rejected separately. There is no deep excavation or automatic site search.
- Furniture is architectural blockwork. Bookshelves are ordinary blocks;
  storage forms are decorative. No working containers, beds, doors, loot or NPCs.
- Hearths contain no real fire or lava. Lighting uses supported hanging lanterns,
  without an exposed glowstone ceiling.
- The quality model supports the deliberately limited primitive palette. It
  does not prove Minecraft collision shapes, stair corner recomputation,
  player pathfinding, lighting, spawn safety, neighbor updates or undo at runtime.
- Snapshot and preflight have cooperative budgets (256 cells / 2 ms per tick),
  and pure planning uses a worker. Final confirmation still checks synchronously;
  individual Minecraft operations can exceed a cooperative budget.
- Existing terrain/building detection is conservative and permits only the
  existing natural/replaceable-block set. No chunks are loaded automatically.
- No Bedrock, consoles, NeoForge, worldgen hooks, exploration-time generation,
  settlement roads, monuments or existing-world migration.

## Manual acceptance checklist (still required)

1. Start the matching client, record the exact JAR hash, world seed and site.
   Check startup/logs without shader packs first.
2. Preview, inspect and confirm on superflat. Walk the approach and gate into
   the court. Visit every listed main room using its intended staircase, with
   flight disabled. Check landings, headroom, stairs and floor openings.
3. Inspect both exterior directions, the gate, hall, residence, keep, windows,
   roof valleys/joins and interiors using Vanilla textures. Look for missing
   support, disconnected rooms, blocked windows and awkward repeated details.
4. Inspect light attachment and neighboring stair shapes after updates/reload.
   Record darkness/light distribution rather than assuming the geometry proves it.
5. In another disposable build, cancel mid-placement and undo. After a complete
   build, edit a few blocks and add a container deliberately, then undo and
   verify those conflicts survive. Test unload/pause and reconnect separately.
6. Start a dedicated test server with an already accepted, applicable EULA;
   verify server-only startup and the command path. This session does not accept
   an EULA or claim a dedicated-server run.
7. Only then test shallow natural ground, site-change rejection between preview
   and confirmation, and protected blocks. Record actual timings from inspect
   and logs. Do not infer FPS from a core benchmark.

## Reproducing geometry evidence

Compile with the normal build (or the dependency-free verification script), then
export the actual generated canonical block data:

```sh
java -Xmx512m -cp core/build/classes/java/main:core/build/classes/java/test \
  dev.castleforge.core.ReferenceExport build/reference 20261020
python scripts/render-reference.py build/reference/blocks.tsv build/reference/southeast.png
python scripts/render-reference.py build/reference/blocks.tsv build/reference/northwest.png --turn 2
python scripts/render-reference.py build/reference/blocks.tsv build/reference/ground-plan.png --top --cut-y 3
python scripts/render-reference.py build/reference/blocks.tsv build/reference/hall-section.png --crop -19 -30 16 -18 --cut-y 13
python scripts/render-reference.py build/reference/blocks.tsv build/reference/stair-section.png --crop -38 -29 -28 -14 --cut-y 24
python scripts/render-reference.py build/reference/blocks.tsv build/reference/gate.png --crop -12 27 12 41 --elevation 15
```

On Windows use `;` instead of `:` in the Java classpath. The standalone renderer
requires Python 3.10+ and Pillow. It draws only the generated block primitives;
it imports no Minecraft textures and is not an in-game screenshot. CI exports
the TSV/model and verifies the JAR metadata/hash separately. The evidence record
contains the actual tested commands/results and known visual findings.

These cuts deliberately remove intersected walls/floors to expose the inside;
cropped beam ends are section boundaries, not floating beams in the full plan.
