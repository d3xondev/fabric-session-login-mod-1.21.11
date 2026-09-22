# DexSessionLogin

A Fabric mod for Minecraft 1.21.11 that adds session token login and account management.

## Features

- Switch between saved accounts in-game
- Direct login using a session ID without saving
- Live session validity indicator on the title and multiplayer screens
- In-game username and skin changer for active sessions
- Restore back to the launcher's original session at any time

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.16.x or newer
- Fabric API

## Building

Requires JDK 21.

```bash
./gradlew build
```

The compiled mod will be located in `build/libs/`.

## Storage

Account configurations are saved locally in `.minecraft/config/dexsessionlogin/accounts.json`.
