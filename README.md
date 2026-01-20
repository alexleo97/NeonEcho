# NEON ECHO // HYTALE MOD
Low-noise startup mod with a neon-skinned vibe. It prints a welcome line on join and exposes a `/netrun` command for a quick status readout.

---

## SIGNALS

- Neon welcome on join.
- `/netrun` command with a short flavor status.

## RUNTIME

- Hytale installed via the launcher.
- Java 25 for building.

## BUILD

```sh
./gradlew build
```

Output:

```
build/libs/NeonEcho-0.0.2.jar
```

## INSTALL (SINGLEPLAYER)

1. Drop the jar into your Mods folder.
2. Restart Hytale (mods load on startup).
3. Join your world and run `/netrun`.

macOS Mods path:

```
~/Library/Application Support/HytaleF2P/release/package/game/latest/Client/UserData/Mods
```

## DEV NOTES

- If Hytale lives elsewhere, set `hytale_home` in `gradle.properties` or pass `-Phytale_home=...` to Gradle.
- Entrypoint: `com.alexleo.neonecho.NeonEchoPlugin`

## LICENSE

MIT (swap if you want a different one).
