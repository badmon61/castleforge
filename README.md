# CastleForge

Terrain-aware procedural castles and independent culture regions for Minecraft.
Original idea: Emre's brother. Concept and project direction: Emre (`badmon61`).

## Status: 0.1.0-alpha.1 — manual prototype

**Use disposable test worlds only. This is not the finished world-generation mod.**
The first milestone provides real generator code and a Fabric adapter, not the final visual quality, mountain planning, population system or giant fantasy monuments.

### Implemented

- Deterministic, jittered Voronoi culture regions with eight stable culture IDs.
- Default culture scale: **7 × a configured linear biome reference**. The fallback reference is 1,024 blocks, giving 7,168-block nominal site spacing. These are design defaults, **not measured vanilla biome dimensions**, fixed region diameters or seven-biome counts.
- Generation version and scale captured per save; subsequent changes to global defaults do not rescale an existing save.
- One procedural prototype castle family with dimension/height variations, four towers, curtain walls, wall walks, a raised keep, interior floors, windows, ladders and an entrance ramp.
- Restored, weathered and ruined states; cultural vanilla-block palettes and a basic sandy-ground adaptation. Eight palettes do **not** yet mean eight different architectural grammars.
- Dry, already-loaded terrain sampling; limited foundations on gentle slopes; unsuitable sites rejected rather than carved through mountains.
- Operator-only textual preview, explicit placement confirmation, bounded build work, cancel and conflict-aware in-memory undo.
- Independent Java core verification across 50 castle seeds.

### Not implemented yet

Automatic generation during exploration; automatic biome-scale detection; full terrain terraces and bridges; huge castles and monuments; inhabitants and loot; cultural architecture blending; roads and settlements; Bedrock, consoles and NeoForge. No modpack compatibility or FPS guarantees.

## First test

Runtime target: **Minecraft Java 26.1.2**, **Java 25**, **Fabric Loader 0.19.5+**, **Fabric API 0.155.3+26.1.2**.

1. Create a separate Prism/Fabric test instance. Do not install this first alpha into a valuable survival world.
2. Get the `castleforge-fabric-26.1.2` artifact from a successful GitHub Actions run. Install `castleforge-fabric-0.1.0-alpha.1.jar` and Fabric API; **do not install the `-sources.jar`**.
3. Create a new Creative superflat world with commands enabled. Remain near the site while building or undoing.
4. Run these separately:

```mcfunction
/castleforge region
/castleforge preview
/castleforge build confirm
/castleforge status
```

Without coordinates, preview selects a site 28 blocks north of you. It only prints a report; it is not a ghost-block visual preview. Confirmation expires after 120 seconds. Explicit coordinates are supported with `/castleforge preview <x> <z>`.

```mcfunction
/castleforge cancel
/castleforge undo
```

Cancel stops future edits; **it does not immediately remove the partial building**. Undo operates on the last build by that operator, in the current server session, skipping blocks subsequently changed by someone else. Starting another build replaces the old undo history. Closing the world, stopping or crashing the server loses undo. Physics side effects are not comprehensively journaled. **Undo is not a backup.**

All commands require Minecraft moderator permission / enabled cheats. No chunks are force-loaded. There is no automatic placement on world load.

## Configuration

Optionally copy `config/castleforge.example.properties` to the instance's `config/castleforge.properties` **before first use in a new save**. The chosen generation values are stored in `<world>/castleforge/generation-v1.properties`. Do not edit or delete that file on a world you intend to preserve. Unknown algorithm versions fail closed.

## Build

Install JDK 25 and Gradle **9.4.0**, then:

```sh
gradle --no-daemon build
gradle :fabric:runClient
```

A Gradle wrapper is not yet checked in; the CI explicitly installs the pinned Gradle release. The core itself targets Java 21 and has no Minecraft dependency. Its offline verification can run with JDK 21+:

```sh
bash scripts/verify-core.sh
```

See [architecture](docs/ARCHITECTURE.md), [test checklist](docs/TESTING.md), [validation record](docs/VALIDATION.md), and [roadmap](docs/ROADMAP.md).

## Licensing

A redistribution license has not been selected. Do not assume that public visibility grants open-source redistribution rights. No Mojang binaries, third-party schematics or texture assets are included.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
