# Obfuscate Lib Compatible

Fixes the compatibility issue between [MrCrayfish's Obfuscate](https://www.curseforge.com/minecraft/mc-mods/obfuscate) and [Haubna's Physics Mod](https://www.curseforge.com/minecraft/mc-mods/physics-mod) by automatically setting `required` to `false` in `obfuscate.mixins.json`.

## The Problem

Physics Mod causes Obfuscate's Mixin injection to fail. Because Obfuscate has `"required": true` in its Mixin config, this failure is treated as fatal and crashes the game.

## How It Works

This mod runs at the earliest stage of game startup (via `ITransformationService`) and patches `obfuscate.mixins.json` inside the Obfuscate jar before it is read by the Mixin framework. A backup of the original jar is saved as `obfuscate-xxx.jar.bak` in your mods folder the first time the patch is applied.

## Requirements

- Minecraft 1.16.1 – 1.16.5
- Forge 32+
- [Obfuscate](https://www.curseforge.com/minecraft/mc-mods/obfuscate)
- [Physics Mod](https://www.modrinth.com/mod/physicsmod) (Optional)

## Installation

1. Install Obfuscate as usual.
2. Drop this mod's jar into your `mods` folder.
3. Launch the game — the patch is applied automatically.

## Restoring the Original

If you want to revert Obfuscate to its original state, rename `obfuscate-xxx.jar.bak` back to `obfuscate-xxx.jar` in your mods folder.

## License

This mod is licensed under the MIT License. See [LICENSE](LICENSE) for details.