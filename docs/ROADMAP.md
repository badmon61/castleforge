# Roadmap

## M0 — first manual prototype (this change)

- [x] Pure deterministic core and Fabric build pipeline.
- [x] Persisted culture reference scale with multiplier 7.
- [x] One procedural castle family, conservative foundations, interior routes, three conditions.
- [x] Palette adapter, explicit preview/confirm and bounded manual placement.
- [x] Core regression verification over 50 seeds.
- [ ] Complete the in-game checklist before advertising playability.

## M1 — architecture and safe world generation

- Data-driven building grammars, module sockets, roofs, room purposes and navigation contracts.
- Site suitability scoring, terrace graph, cliff/bridge constraints, contour-following defenses and entrance planning.
- Registered worldgen structures with chunk-order tests; do not ship chunk-load retro-placement as a shortcut.
- Crash-safe operation journal, integration tests, golden geometry snapshots and dedicated-server smoke tests.
- Configured palettes/tags for modded materials, claim protection and structure-spacing integration.
- Deterministic auto-scale adapters for known biome generators, with a persisted fallback.

## M2 — culture, history and life

- Distinct historical/fantasy architectural grammars, border blending, climate-aware roofs and materials.
- Coherent construction phases, regional heraldry, maintenance, structural decay and reconstruction.
- Inhabitants driven by condition and culture; vanilla-compatible baseline, bounded population and optional NPC integration.
- Loot, exploration rewards and accessible interiors, without requiring combat in every site.

## M3 — monumental sites

- Rare giant castles and palace complexes, real route-length targets instead of arbitrary bounding-box sizes.
- Global lightweight planning, bounded chunk-local detail generation, hard memory/work budgets and no recursive chunk loading.
- Profile actual FPS, server tick time, first-generation stalls and save size with representative hardware/modpacks.

## M4 — other platforms

NeoForge adapter; separate Bedrock behavior/resource packs and generator backend; real console distribution/testing plan. Reuse specifications and assets where feasible, not promises of identical APIs or performance.
