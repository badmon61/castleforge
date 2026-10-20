# Castle identities and architectural reports (0.2.1 alpha)

## What is implemented

`CastleIdentity.generate(seed, x, z, region, condition)` is a stateless, independent core function:
- Site name and regional dynasty are derived from seed and culture and can be recreated without a save-file database.
- Regional heraldry is a **textual motif**; Minecraft banner/block entities have not been generated.
- Ages range from 120 to 800 fictional years. Construction events are sorted from oldest to newest and depend on restoration status.
- Intended population level is `INHABITED`, `SPARSE`, or `ABANDONED`, but **does not spawn entities**.
- All returned collections are immutable. `CastleIdentity.VERSION = 1` is the metadata algorithm version. It is independent from world geography and geometry versions.

`PlanDiagnostics.analyze(plan)` counts planned cells, foundation/weathered/stair blocks, module storeys, bridge links and navigation target declarations, plus a 3D bounding box. It reports the *plan* rather than measured FPS, actual height-map validation or proven Minecraft pathfinding.

The Fabric bridge exposes metadata in the existing preview summary and an additional read-only `/castleforge inspect` command. This command reads the most recent, still-valid preview **for the current operator**. It performs no extra chunk generation or block placement. It cannot inspect already-placed castles or discover existing structures.

Facades use a culturally more suitable decorative trim and timber block where possible. Existing geometry is unchanged. Material changes only affect future manually placed castles, not existing world blocks.

## Test and acceptance

- `gradle --no-daemon build` must run all three core suites and compile the Fabric adapter.
- `bash scripts/verify-core.sh` can verify the pure Java core with JDK 21+.
- Verify the metadata suite covers all eight culture IDs, 100 seeds, three preservation states, chronology, reproducibility, population intentions and dynasty consistency.
- In Minecraft test `/castleforge preview terraced`, then `/castleforge inspect`; confirm no world blocks changed.
- Inspect both classic and terraced plans; compare the preview report to the actual built result.
- Test aged/ruined castles' trim/wood combinations under all cultural palettes and watch for unsupported block states.
- Keep `main` and the previously compiled terraced alpha intact until in-game feedback validates the new build.

This metadata release intentionally does **not** alter `CultureMap`, `CastlePlanner`, `TerracedCastlePlanner` or `PlanNavigation`. It does not solve large monuments, natural generation, automatic biome measurement or Bedrock.
