# NeonEcho

NeonEcho is a small cyberpunk-themed Hytale plugin. It shows a welcome message on join and adds a `/netrun` command for a quick flavor status check.

## Features

- Welcome message when you join a world.
- `/netrun` command that prints a short status readout.

## Requirements

- Hytale installed via the launcher.
- Java 25 for building.

## Build

```sh
./gradlew build
```

Output jar:

```
build/libs/NeonEcho-0.0.2.jar
```

## Install (singleplayer)

1. Copy the jar into your Mods folder.
2. Restart Hytale (mods load on startup).
3. Join your world and run `/netrun`.

macOS Mods folder:

```
~/Library/Application Support/HytaleF2P/release/package/game/latest/Client/UserData/Mods
```

## Development notes

- If your Hytale install is in a custom location, set `hytale_home` in `gradle.properties` or pass `-Phytale_home=...` to Gradle.
- The plugin entrypoint is `com.alexleo.neonecho.NeonEchoPlugin`.

## License

MIT (update if you want something else).
