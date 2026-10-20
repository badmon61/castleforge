# Architecture and non-negotiable requirements

## Product decisions

- Historical architecture mixed with fantasy. Extremely rare monumental sites should eventually span large landscapes and provide multi-minute traversal routes.
- Inhabitants will depend on culture, condition and site history; not every castle is a hostile dungeon.
- Virtual culture regions are independent of real biomes and default to sevenfold **linear** reference scale. Area is only approximately 49-fold for geometrically comparable regions.
- Local materials and climate adapt a regional style without making one culture equal one biome.
- No half-buried buildings, impossible entry routes or automatic destructive retro-generation. Prefer rejecting a site.
- Preserve long-term worlds. Version algorithms and freeze the generation contract per save. Updating a mod must not silently reshuffle already explored culture geography.
- Vanilla blocks first, optional integration later. Broad compatibility is a design objective, not a universal guarantee.

## Current layers

`core`: pure Java, no game classes, global mutable RNG or chunk loading. `CultureMap` evaluates a world-coordinate field. `CastlePlanner` accepts a finite surface sampler and produces an immutable, deduplicated final voxel plan. Abstract material roles are resolved by an adapter.

`fabric`: server-side command and block-placement integration. It works inside a client-hosted singleplayer server or a dedicated Fabric server, without renderer hooks. No custom blocks or entity registrations.

The shared specification and material roles can later be implemented by a different Bedrock backend. This does not promise a Java JAR will run on consoles or that unrestricted Java world-generation APIs exist in Bedrock.

## Determinism contract v1

Inputs: world seed, coordinates, fixed culture IDs, algorithm version, per-save linear scale. Use explicit 64-bit mixing with floor division at negative coordinates. Nearest-site ties are deterministic. Culture lookup does not inspect generated chunks and does not depend on travel order.

The map is jittered Voronoi; borders are initially polygonal rather than smoothly warped. Neighbor culture and a blend weight are computed, but architectural blending is not applied yet. Multiple neighboring regions can independently receive the same culture.

Scale is a **configured reference**, not a runtime biome measurement. Automatic scaling needs generator-specific adapters or bounded, deterministic reference sampling at fixed coordinates, followed by persistence. Never continuously rescale based on what the player explores.

## Placement contract in the prototype

1. Require operator permission and the Overworld.
2. Verify the complete 49×49 survey area is already loaded.
3. Sample every ground column, reject water, unknown ground and relief over six blocks.
4. Plan a small, supported court and a ramp; reject an unreachable ramp endpoint.
5. Validate planned cells against height, world border, fluids, block entities and conservative replaceability.
6. Retain a text-preview write set for at most 120 seconds; require separate `build confirm`.
7. Recheck all changed cells before starting; check every cell immediately before writing.
8. Place bottom-up, with at most 256 edits and a cooperative 2 ms budget per server tick. A single Minecraft block update may exceed the time budget, so this is not a hard frame-time guarantee.
9. Pause on unloaded chunks rather than force-loading them. Stop on conflicts and retain session-only undo.

The small preview/planning pass is synchronous and bounded; giant monuments must **not** use this implementation unchanged. Undo records explicit block states only. It is not crash-safe and does not capture every neighboring physics update, entity effect or plugin callback. No claim/protection-mod integration exists yet. Player-placed natural blocks cannot be reliably distinguished from natural terrain by block type alone.

## Production world-generation design (not implemented)

Use registered structures and bounded per-chunk placement rather than placing huge buildings from chunk-load callbacks. Plan globally, write only owned chunk sections. Terrain sampling must avoid recursively generating neighbors and must handle structure-stage ordering. Persist/cache plan manifests with explicit budgets and test chunk generation in different orders.

Conflict avoidance requires deterministic reservations for CastleForge sites plus integration with Minecraft structure starts and documented protection/structure mods; no system can infer every third-party change. Save a manifest that records the algorithm, palette contract, site bounds and generation stage so upgrades are auditable.

## Verification limits

Core verification tests coordinate/seed stability, geometry budgets, foundations and simplified two-block-high walking/jumping/ladder connectivity on 50 seeds. This is **not Minecraft entity pathfinding**, not a rendering test and not a game-engine physics test. Compile the adapter in CI and test placement, undo, dedicated startup and modpacks in Minecraft separately.
