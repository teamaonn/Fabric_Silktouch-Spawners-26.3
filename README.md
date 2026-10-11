# Fabric Silk Touch Spawners — Minecraft 26.3

Small Fabric mod with one purpose: **mine a regular or Trial Spawner with a Netherite Pickaxe enchanted with Silk Touch to receive a regular spawner retaining the mob type.**

- Regular spawner → regular spawner of the same mob type.
- Trial spawner → **regular** spawner of the same mob type (no trial mechanics, waves, rewards, or cooldowns).
- All other tools → vanilla behavior.
- No custom blocks, commands, recipes, configuration menu, or extra features.

## Build

Use Java 25 and Gradle with Fabric Loom. Run `gradle build` from the repository root. Fabric API is required.

## Status

**Initial implementation, not yet compiled or tested in Minecraft 26.3.** Verify Gradle compilation and test both ordinary and Trial Spawner drops and placement before distributing. Trial Spawner data layout may require adjustment based on 26.3 runtime tests.

The original SilkSpawners 1.3 mod was supplied as a behavior reference; this repository uses a separate, minimal implementation.
