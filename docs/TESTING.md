# Test checklist — 0.1.0-alpha.1

Use a new disposable world. Do not reuse a valued survival save.

## Automated core gate

`gradle :core:verifyCore` or `bash scripts/verify-core.sh` must succeed. Covers sevenfold reference scaling, negative coordinates, world-edge inputs, deterministic geography, exploration-order independence, deterministic plans, rejecting cliffs/invalid heights, gentle slopes, unique cells, placement order, bounds, ladder backing and simplified gate/keep/upper-level reachability over 50 seeds.

The assertions inside per-voxel loops are repetitive invariant checks, not millions of independently designed test cases. A successful build does not certify in-game behavior.

## In-game gate (must be recorded manually before a release)

- [ ] Dedicated Fabric server starts without client-class errors.
- [ ] Singleplayer starts with 26.1.2, Loader 0.19.5+, matching Fabric API and Java 25.
- [ ] No castle appears automatically and no world blocks change from `/castleforge region` or `preview`.
- [ ] Superflat preview gives coordinates and explicit confirmation guidance.
- [ ] Build completes and `/castleforge status` reports completion.
- [ ] Gate, keep entrance, both interior floors, towers, wall walks and roofs can be reached without flight.
- [ ] Ladders remain after neighbor updates, including ruined variants.
- [ ] Repeat at well-separated coordinates: heights, dimensions, condition and culture palettes vary.
- [ ] Uneven terrain within the six-block limit produces supported foundations and a usable entry.
- [ ] Steep slopes, water, block entities, structures and unloaded sites are rejected without placement.
- [ ] An edit between preview and confirm rejects the build before any write.
- [ ] Mid-build changes stop safely; cancel leaves a documented partial build; undo restores unchanged edited cells.
- [ ] Undo preserves deliberately changed castle blocks; unloaded sites pause and resume when revisited.
- [ ] Closing/reopening retains culture IDs. Undo is intentionally lost, not silently advertised as available.
- [ ] Unknown generation version fails closed.
- [ ] Non-operators cannot invoke commands.

## Compatibility matrix: initially UNTESTED

Fabric singleplayer; dedicated Fabric server; Sodium; Iris; Distant Horizons; Terralith; Tectonic; custom heights; other structure mods; land-claim/protection mods. Do not mark any supported based only on absence of mixins.

Bedrock, NeoForge and console editions are not build targets in this milestone.

## Bug reports

Record mod version and commit, Minecraft/Loader/API versions, seed, origin coordinates, culture reference settings, other mods, exact commands, relevant latest.log lines and screenshots. Do not upload an entire personal world or secrets just to report a bug.
