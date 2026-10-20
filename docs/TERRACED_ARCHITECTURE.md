# Terraced architecture v1 — 0.2.0-alpha.1

## Scope and separation

The classic `CastlePlanner`, `CultureMap` and `WorldSettings` contracts are unchanged. `TerracedCastlePlanner` is an opt-in, bounded manual reference design, selected with `/castleforge preview terraced [x z]`. It does not register automatic world generation. Architecture version 1 and culture version 1 are separate contracts.

This step demonstrates one asymmetric, three-court composition with eight modules. It does not yet grow arbitrary shapes around every mountain, implement settlements, or claim compatibility with all world generators. Terrain that cannot safely accommodate the reference composition is rejected. Building designs are procedural project code, not imported schematics.

## Pipeline

1. The adapter requires moderator permission, Overworld, and already-loaded survey chunks.
2. Capture exactly 105 × 105 column samples once, on the server thread. Unknown, wet or non-whitelisted ground is marked unusable. Unused bad columns need not reject the site.
3. Evaluate four cardinal orientations of seed-derived, offset courtyard footprints. Score foundation volume and prefer the keep uphill of the entrance. No additional world reads are needed for retries.
4. Establish an individual floor for each court. No planned cell may be below the sampled first-free height; excavation is prohibited. Local foundation depth is limited to eight blocks and inter-court floor relief to 24 blocks. These are rejection limits, not a claim that every such slope is accepted.
5. Place halls, towers, keep and gatehouse within the enclosure system. The rooms, storeys, roof family and construction phase are recorded explicitly. Geometry remains one reference grammar; culture affects palettes and selected roof rules.
6. Plan three-wide connector footprints, solve a slope-limited height profile, add steps and short beam/pier sections. Pier depth is limited to twelve blocks. Some exposed bridge footprint cells become parapets; clear walking width can be narrower.
7. Generate weathering in spatial patches. Keep doorways, ladders, access floors and supports protected. Ruined variants remove selected crenels/glazing; realistic collapse and repair simulation remain future work.
8. Verify two-block clearance and conservative walking/jumping/ladder reachability to every declared interior floor and all four enclosure sides. Navigation is bounded to 100,000 visited positions. Stair geometry is conservatively treated as full cubes; this is not a replacement for real game tests.
9. Sort structural writes bottom-up, then attach ladders after every backing wall is present. Directional blocks rotate with the layout.
10. The Fabric adapter validates height/border/replaceability, keeps a 120-second preview and requires `build confirm`. No-op air cells and sampled support blocks are rechecked too. During placement the existing per-edit conflict checks, cancel and session-only undo remain active.

## Data and budgets

- `Plan`: immutable blocks, terraces, rooms, links and navigation targets; world seed influences the plan without mutable global RNG.
- `Terrace` / `Room` rectangles are local coordinates. Block positions, entrance and navigation targets are world coordinates. The plan records its rotation and origin to avoid ambiguity.
- `Room.phase`: 0 oldest core, 1 expansion, 2 repaired/newer work. Phase affects weathering; it is deliberately not advertised as a full historical simulation.
- Maximum 180,000 final cells, deduplicated. Typical reference fixtures are much smaller. The limit includes planned AIR cells.
- Planning is synchronous and suitable only for this bounded prototype. Large monuments need asynchronous pure planning and proper chunk-local worldgen integration rather than simply increasing these constants.
- No claim/protection-mod integration; vanilla-looking player-built ground cannot be distinguished reliably from natural ground. Only disposable test worlds are supported for acceptance work.

## Verification performed by the new suite

32 seeds × five terrain fixtures: flat, northward slope, eastward slope, ridge slope and short transverse gullies. In the recorded local run all 160 combinations succeeded; 100 plans included a bridge section and 96 included directional steps. Additional cases cover protected/water-like terrain, cliffs, invalid heights, unused invalid survey corners, coordinate edges, missing vertical access, duplicate cells, fixed sampling count, deterministic replays and parallel generation.

The flat central-European reference geometry at seed 42 and origin 0,0 is frozen by SHA-256:

```text
6fd20d8855fdc190a7e81e9772cef9d4aad3974a3a405ca25f31161b643f7828
```

The new suite passed locally on Java 21 with a 512 MiB heap. CI additionally compiles the real Fabric adapter and runs both generator suites. The PR checks are the source of truth for CI status; this document does not predeclare their result.

## In-game acceptance still required

- [ ] Start the exact 26.1.2 Fabric instance and a dedicated server without runtime registration errors.
- [ ] Confirm `/castleforge preview` still selects the classic generator.
- [ ] Confirm `/castleforge preview terraced` is read-only and displays three courts/eight modules.
- [ ] Complete a superflat build; enter the gatehouse, all building storeys and every wall-walk side without flying.
- [ ] Repeat in a new disposable normal world on a gradual open slope and a suitable short gully.
- [ ] Check roof/stair orientation and ladder persistence in all four rotations after neighbor updates.
- [ ] Check the doorway/interior clearance in restored, weathered and ruined variants using actual player movement.
- [ ] Put a block in a previously empty doorway between preview and confirmation; confirmation must refuse before writing.
- [ ] Test cancel, conflict-aware undo, automatic stair-shape changes, unloaded-site pause/resume and documented history loss after world close.
- [ ] Inspect visual scale, material transitions, underside supports and bridge entrances; pure geometry tests cannot certify their appearance.
- [ ] Record real preview stalls, generation tick cost and rendering performance before making performance claims.

Existing worlds, other terrain/structure/claim mods, Bedrock/consoles, giant monuments and automatic worldgen remain unverified or unimplemented. Do not install this alpha into a valuable survival save.
