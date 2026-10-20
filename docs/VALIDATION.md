# Validation record — first alpha

Date: 2026-10-20.

## Verified generator/adapter implementation

Implementation commit: `42009a0cdf8399c8de896142b5dd5e471187467c`.
GitHub Actions run: https://github.com/badmon61/castleforge/actions/runs/39392030630
Job: `118525608409`.

- Full Gradle build succeeded, including `:core:compileJava`, `:core:compileTestJava`, `:core:verifyCore`, `:fabric:compileJava` and `:fabric:jar`.
- Verification: **50 castle seeds**, deterministic culture queries, geometry and route invariants. Output: `PASS: 5727712 assertions, 50 castle seeds; determinism, terrain, routes, supports and budgets.` The large assertion count comes from repeated per-voxel invariant checks, not millions of different test scenarios.
- Core also compiled and passed locally using Java 21 without Minecraft dependencies.
- The compiled artifact contains the pure core classes and Fabric entrypoint. Core bytecode targets Java 21 (major 65); adapter bytecode targets Java 25 (major 69). The metadata contains the expanded version and exact Minecraft target. No Minecraft implementation classes are bundled.
- This documentation revision changes attribution metadata and build/report documentation, not the verified generator or adapter logic. Its own CI result is available on the corresponding commit/PR.

## Not verified

Minecraft client rendering, actual entity movement, block-physics effects, runtime registration, real command invocation, server startup, terrain/modpack compatibility and FPS. Those require the unchecked in-game checklist in `TESTING.md`.

A successful compile plus pure-Java path checks must not be described as an in-game test or proof of production readiness. The downloadable artifact is an experimental manual prototype for disposable test worlds only.
