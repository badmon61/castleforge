# CastleForge

Terrain-aware procedural castles and independent culture regions for Minecraft.
Original idea: Emre's brother. Concept and project direction: Emre (`badmon61`).

## Status: 0.3.0-alpha.1 - authored reference architecture

**Experimental, opt-in, disposable test worlds only. No automatic generation.**

0.3 adds one independently authored reference castle, Falkenwacht. A typed
architecture model defines building masses, room functions, floor levels,
connections, stair flights, windows and roofs before any block placement.
The new module and interior passes create a dominant keep, a great hall,
residential/service wings, a framed gatehouse and a courtyard, with protected
routes and final geometry checks. No schematics or third-party templates.

Use `/castleforge preview reference [x z]`. Snapshot and preflight work run over
several server ticks; pure planning runs on a bounded worker. **Wait for
`PREVIEW READY`**, then use `/castleforge inspect` and `/castleforge build confirm`.
The reference surveys 109 x 109 already-loaded columns; its default centre is
58 blocks north of you. `/castleforge status` reports preparation and placement;
`/castleforge cancel` also cancels reference preparation.

This is one maintained central-European architectural study, independent of the
site's culture identity. Existing culture regions, 7x scale and saved settings
are unchanged. Furniture uses safe single-block primitives, not inventories or
multi-block beds/doors. Hearths are decorative. The original classic and terraced
paths remain available and retain their regression tests.

See [reference architecture, limits and manual test](docs/REFERENCE_ARCHITECTURE.md),
[design decision](docs/ADR_0001_REFERENCE_ARCHITECTURE.md) and
[verification evidence](docs/REFERENCE_VALIDATION.md). Technical images are
rendered from generated block data; they are **not Minecraft screenshots or
proof of visual acceptance, block physics, actual collision shapes or FPS**.

## Previously: 0.2.1-alpha.1 - identity and architectural diagnostics

**New in 0.2.1:** Seed-stable, culture-dependent castle names and regional dynasties, symbolic heraldry, age, chronological construction milestones, and a condition-derived *planned* occupancy category. These are narrative metadata only: **no NPCs, loot, actual banners or timeline simulation are spawned**. The two existing castle shapes and culture map remain unchanged. Different regions may coincidentally share generated names; uniqueness is not promised.

The terraced preview now includes a quick dimensional/material/access summary. Use `/castleforge inspect` after `/castleforge preview terraced` (or classic preview) to read the current design report. Preview and inspect are read-only. Reports expire with the preview (120 seconds), and are cleared by build confirmation.

Small visible facade improvement: culture-sensitive accent blocks for trim and timber. This changes palette appearance, **not** the geometry generator, region borders or world settings. Refer to [identity and diagnostic contract](docs/IDENTITY_AND_DIAGNOSTICS.md).

## Previously: 0.2.0-alpha.1 - opt-in terraced architecture

**Disposable test worlds only. No automatic world generation and no completed in-game acceptance test.**

The original small castle remains available. A new, separate planner adds an asymmetric reference complex with three individually elevated courtyards, eight building modules, terrain-oriented placement, stairs and short supported bridges. This is an engineering alpha, not the final visual quality or an unrestricted mountain/monument generator.

### New in this branch

- Three offset, chamfered enclosures: outer bailey, residential court and upper keep; foundations are calculated per terrace rather than flattening the entire site to one height.
- Eight modules: service hall, outer tower, great hall, gallery/workshop, keep, two upper towers and gatehouse. Layout dimensions, offsets, heights, orientation, roofs and condition vary deterministically.
- Four roof forms: gable, hip, spire and battlement. Some choices depend on culture; these are **not eight complete independent architectural grammars**.
- Ground-aware stair connections and short beam/pier bridge sections. Unsuitable slopes, obstacles and excessive support depths are rejected before any block placement.
- Room/storey targets, four wall-walk targets per courtyard and a protected entrance corridor. A conservative navigation check rejects inaccessible plans.
- Ordered structural placement before rotated ladders. Preview confirmation now rechecks unchanged air/doorway cells and sampled ground as well as the write set. Undo tolerates automatic stair-shape updates, but not changed stair material, facing, half or waterlogging.
- Module construction-phase metadata and coherent weathering patches. This is the foundation for construction history, **not a simulation of centuries or structural collapse**.

### Retained

Deterministic culture geography, eight stable culture IDs, and **7 � the configured linear biome reference**. The default reference is 1,024 blocks, resulting in 7,168-block nominal region-site spacing. It is not a measured vanilla biome size or a fixed region diameter. Per-save generation settings and the classic planner are unchanged.

### Still outside this milestone

Automatic exploration-time generation; automatically measured biome scaling; arbitrary contour-following castle grammars; giant monuments; inhabitants, loot and furnishing; roads/settlements; Bedrock, consoles and NeoForge. No general modpack compatibility, FPS or runtime-physics guarantee.

## Test installation

Target remains **Minecraft Java 26.1.2**, **Java 25**, **Fabric Loader 0.19.5+**, **Fabric API 0.155.3+26.1.2**.

1. Use a separate Prism/Fabric instance and a new disposable Creative world with commands enabled.
2. Install `castleforge-fabric-0.3.0-alpha.1.jar` from a successful branch Actions build plus the matching Fabric API. Replace the old CastleForge JAR; do not install two versions or a `-sources.jar`.
3. Start on superflat to verify the reference workflow, then test a second disposable world with shallow, open terrain. Reference uses a 109 � 109 survey; legacy terraced uses 105 � 105. The whole site must already be loaded.
4. Execute separately:

```mcfunction
/castleforge region
/castleforge preview reference
/castleforge status
```

After `PREVIEW READY`:

```mcfunction
/castleforge inspect
/castleforge build confirm
/castleforge status
```

Reference is centred 58 blocks north of you; legacy terraced is centred 56 blocks north. Reference orientation is deterministic from seed and position. Explicit coordinates work with `/castleforge preview reference <x> <z>` and `/castleforge preview terraced <x> <z>`. Preview is a **text report**, not a visual ghost building. Confirmation expires 120 seconds after readiness. Stay near the entire build area.

The original generator is still selected by `/castleforge preview` or `/castleforge preview <x> <z>`; its default centre remains 28 blocks north of you.

```mcfunction
/castleforge cancel
/castleforge undo
```

Cancel leaves a partial building. Undo only covers the last build in this server session and is lost on server shutdown, world close, crash or the next build. It skips conflicting later edits, with the documented exception for automatically recomputed stair shape. It does not journal every neighboring physics effect. **Undo is not a backup.**

All commands require moderator permission/cheats. No chunks are force-loaded.
Reference snapshot/preflight use cooperative tick budgets and its pure planner
runs on a worker. Legacy previews and final confirmation checks remain
synchronous. Placement uses the existing cooperative per-tick budget; a single
Minecraft operation can exceed it. This is not a hard real-time or FPS guarantee.

## Generation settings

Optionally copy `config/castleforge.example.properties` to the instance's `config/castleforge.properties` before first use in a new save. Settings are captured in `<world>/castleforge/generation-v1.properties`. Do not alter/delete this file on a world you intend to preserve. This architecture change does not rescale or reshuffle culture regions.

## Build and verification

Use JDK 25 and Gradle 9.4.0:

```sh
gradle --no-daemon build
gradle :fabric:runClient
```

The CI uses pinned Gradle; a Gradle wrapper is not yet checked in. Pure generator tests also run with JDK 21+:

```sh
bash scripts/verify-core.sh
```

Windows PowerShell (JDK 21+ on PATH):

```powershell
./scripts/verify-core.ps1 -ExportReference
```

The additional `verifyReference` suite checks the separate architecture and final
furnished blocks, deterministic rotations, bounded sites, deliberate invalid
fixtures and the terrain approach. Exact cases and results are recorded in
[reference validation](docs/REFERENCE_VALIDATION.md).

The classic suite checks 50 seeds. A third suite checks narrative identities across 100 seeds and eight cultures, coherent chronology, region-level dynasties, diagnostic counts and reporting. The new suite checks **32 seeds � 5 synthetic terrains = 160 combinations**, all eight culture profiles, all four rotations, roofs and condition states, determinism, navigation, bounded supports, rejection cases, negative/world-edge coordinates and concurrent planning. A golden geometry digest detects unintended generator drift. Repeated per-voxel assertions are not millions of different test scenarios.

Passing these checks is **not a Minecraft launch, entity-pathfinding, block-physics or visual test**. See [terraced architecture and acceptance checklist](docs/TERRACED_ARCHITECTURE.md), [original architecture](docs/ARCHITECTURE.md), [classic test checklist](docs/TESTING.md), [initial alpha validation](docs/VALIDATION.md), and [long-term roadmap](docs/ROADMAP.md).

## Licensing

A redistribution license has not been selected. Public visibility does not grant a redistribution license. No Mojang binaries, third-party schematics or texture assets are included.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

