# AZSL Language Support for CLion/Rider

Plugin providing syntax highlighting and **azslc** validation for **AZSL** (Amazon Shader Language) — the shader language used by [O3DE](https://o3de.org/) (Open 3D Engine) — plus schema-driven code completion for O3DE's JSON `.shader` files.

![Example](example.png)

## What's New (v1.18.0)

### `.shader` completion

O3DE `.shader` files (the JSON side-car that configures how an `.azsl` gets compiled) now get
code completion, driven by the engine's own `ShaderSourceData` / `AZ::RHI` render pipeline states:

- **Render pipeline states** — `DepthStencilState` (`depth`/`stencil`, `enable`, `writeMask`,
  `compareFunc`, `failOp`/`passOp`/`depthFailOp`, `frontFace`/`backFace`), `RasterState`
  (`cullMode`, `fillMode`, depth bias, clip/conservative raster), `BlendState`,
  `GlobalTargetBlendState`, `TargetBlendStates`
- **Enum values** — `Never/Less/Equal/LessEqual/Greater/NotEqual/GreaterEqual/Always`,
  `Zero/All`, `Keep/Replace/IncrementSaturate/…`, `None/Front/Back`, `Solid/Wireframe`,
  `Add/Subtract/…`, `ColorSource/AlphaSource/…` — plus `true`/`false` and entry point stages
- **Whole schema** — all 15 top-level keys (`Source`, `DrawList`, `ProgramSettings`,
  `Supervariants`, `AddBuildArguments`/`RemoveBuildArguments` (`azslc`, `dxc`, `preprocessor`, …),
  `Definitions`, `DisabledRHIBackends`, `KeepTempFolder`, …)
- **`"Source"` paths** — completes relative `.azsl` paths from the project
- **Auto-popup** on structure characters (`{`, `:`, `,`, `"`) as well as while typing
- Works without a running engine — it's all static schema data transcribed from the engine sources

The engine matches member names case-insensitively, so `"Depth"` and `"depth"` are both valid;
completion suggests the canonical serialize-context spelling.

### Fixed

- The **Shader File** template now creates a valid `.shader` JSON skeleton (it previously emitted
  a `//` comment, which the Asset Processor rejects)

## What's New (v1.17.0)

### New file templates

Right-click in the project tree (or editor) → **New** now offers the three O3DE shader file kinds:

| Menu entry | Creates |
|------------|---------|
| **AZSL File** | `*.azsl` |
| **AZSL Include** | `*.azsli` (starts with `#pragma once`) |
| **Shader File** | `*.shader` |

They are standard file templates: edit them under **Settings → Editor → File and Code Templates → Files**.

## What's New (v1.16.0)

### O3DE tooling, navigation & structure

- **azslc tool window** — run `azslc --srg` / `--options` / `--ia` / `--bindingdep` on the current file and inspect SRG layout, shader options, entry points and bindings
- **O3DE include resolution** — go-to-declaration and completion now resolve engine/project `<Atom/...>` / ShaderLib includes
- **Auto-detected O3DE roots** — engine (`project.json`, `Gems/Atom`) and project (`ShaderLib/viewsrg.srgi`) roots are discovered automatically (settings still override)
- **Structure view** — outline of ShaderResourceGroups, SRG semantics, structs, enums, functions and shader options
- **Hover documentation** — quick docs for built-ins, types, SRG semantics, sampler states and AZSL keywords
- **Live templates** — `srg`, `srgsem`, `option`, `sampler`, `struct`, `vs`, `ps`, `cs`
- **Intentions** — "Add `#pragma once`"
- **Format on save** — optional clang-format on save (Settings → Tools → AZSL / clang-format)
- **Distinct colors** for SRG frequency semantics, sampler state keywords and the `option` keyword

### AZSL-only

This release focuses exclusively on AZSL/O3DE:

- **File extensions**: `.azsl`, `.azsli`, `.srgi`
- **Dedicated file types & icons** for `.azsl` and `.azsli`
- **azslc validation** — real-time errors/warnings from O3DE's official AZSL compiler
- **Separate AZSL color scheme** — Settings → Editor → Color Scheme → **AZSL**

## Features

- **Syntax highlighting** — keywords, types (including `float2`/`float3`/`float4`, matrices, textures, buffers), AZSL extensions (SRGs, options, samplers), semantics, preprocessor directives, numbers, strings, operators, and comments
- **AZSL extensions**
  - **Shader Resource Groups (SRG)** — `ShaderResourceGroup`, `partial ShaderResourceGroup`, `ShaderResourceGroupSemantic`
  - **Shader Options** — `option bool`, `option enum class` with defaults
  - **SRG Frequency Semantics** — `SRG_PerDraw`, `SRG_PerObject`, `SRG_PerMaterial`, `SRG_PerSubPass`, `SRG_PerPass`, `SRG_PerPass_WithFallback`, `SRG_PerView`, `SRG_PerScene`, `SRG_Bindless`, `SRG_RayTracingGlobal`, `SRG_RayTracingScene`, `SRG_RayTracingMaterial`
  - **Sampler State Properties** — `AddressU/V/W`, `MinFilter`, `MagFilter`, `MipFilter`, `ComparisonFunc`, `BorderColor`, `MaxAnisotropy`, `MipLODBias`, `MaxLOD`, `MinLOD`
  - **Sampler State Values** — `Wrap`, `Clamp`, `Mirror`, `Border`, `MirrorOnce`, `Point`, `Linear`, `Anisotropic`, comparison functions
- **azslc tool window** — SRG layout / shader options / entry points / bindings (see below)
- **Structure view** — SRGs, SRG semantics, structs, enums, functions and shader options
- **Hover documentation** for built-ins, types, SRG semantics and sampler states
- **Live templates** — `srg`, `srgsem`, `option`, `sampler`, `struct`, `vs`, `ps`, `cs`
- **New file templates** — right-click → New offers `AZSL File` (.azsl), `AZSL Include` (.azsli) and `Shader File` (.shader)
- **Struct/class name highlighting** — struct, class, interface, enum, SRG names are highlighted at declaration and every usage site, including names declared in transitively `#include`d files
- **Go to declaration** (`Ctrl+Click` / `Ctrl+B`) — resolve across local and O3DE engine/project includes
- **Code completion** — keywords, types, built-in functions, semantics (suggested after `:`), local identifiers, and symbols pulled from included files
- **`.shader` completion** — O3DE render pipeline states (`DepthStencilState`, `RasterState`, `BlendState`), their enum values, entry points, build arguments and `Source` `.azsl` paths
- **Code folding** — collapse multi-line `{ ... }` blocks and comments
- **clang-format integration** — Reformat Code routes AZSL files through a user-configured `clang-format` binary; optional format-on-save
- **Line and block commenting** (`Ctrl+/`, `Ctrl+Shift+/`)
- **Brace matching** for `()`, `{}`, `[]`
- **Intentions** — "Add `#pragma once`"
- **Color settings page** — customize highlight colors under Settings → Editor → Color Scheme → **AZSL**

## Supported File Extensions

| File type | Extensions |
|-----------|------------|
| AZSL File | `azsl`, `srgi` |
| AZSL Include | `azsli` |

Additional extensions can be added via Settings → Editor → File Types → AZSL.

> **Note on `.shader`:** O3DE `.shader` files (JSON pass assets) are intentionally **not** registered by this plugin. CLion/Rider ship a built-in **"Shader" file type** (a globe icon, from the C++/Rider backend) that claims the `shader` extension, and the platform does not let a plugin override a bundled/existing type for the same extension. If you'd prefer JSON highlighting for `.shader` files, map them to the built-in **JSON** type in **Settings → Editor → File Types**.
>
> The **Shader File** entry under **New** relies on that built-in type: the platform hides a file template when its extension maps to no known file type, so the entry appears in CLion/Rider but not in IDEs that don't know `shader`.
>
> **Code completion** does work in `.shader` files regardless of which type owns them: render pipeline states, enum values, entry points, build arguments and `Source` paths are completed from the static O3DE schema.

## O3DE Tooling

### azslc Tool Window

**View → Tool Windows → AZSL** opens a panel that runs azslc on the file currently selected in the editor (uses the same azslc/cpp paths and include roots as validation):

| Button | Command | Shows |
|--------|---------|-------|
| **Validate** | `azslc --semantic` | full semantic errors/warnings |
| **SRG layout** | `azslc --srg` | constant buffers, textures, samplers |
| **Options** | `azslc --options` | all `option` declarations |
| **Entry points** | `azslc --ia` | entry functions and `numthreads` |
| **Bindings** | `azslc --bindingdep` | which entry points access which SRG resources |

Output is shown as plain text (no line mapping), mirroring the standalone CLI workflow from the O3DE docs.

### Structure View

The **Structure** tool window (or `Ctrl+F12`) outlines the current AZSL file:

- `ShaderResourceGroup` / `ShaderResourceGroupSemantic` (with their `struct`s, functions and `option`s as children)
- `struct` / `class` / `interface` / `enum` / `cbuffer` / `tbuffer`
- functions (definitions)
- shader `option`s

Double-clicking an entry navigates to the declaration.

### Hover Documentation

Hover a built-in function, type, SRG frequency semantic, sampler state property/value, or an AZSL keyword (`option`, `partial`, interpolation modifiers, …) to see a short description.

### Live Templates

Type the abbreviation and press **Tab** in an `.azsl`/`.azsli` file:

| Abbreviation | Expands to |
|--------------|------------|
| `srg` | `ShaderResourceGroup … : SRG_PerMaterial { … };` |
| `srgsem` | `ShaderResourceGroupSemantic … { FrequencyId = …; };` |
| `option` | `option bool … = …;` |
| `sampler` | `Sampler … { AddressU/V = …; MinFilter/MagFilter/MipFilter = Linear; };` |
| `struct` | `struct … { … };` |
| `vs` | vertex-shader entry point |
| `ps` | pixel-shader entry point |
| `cs` | compute-shader entry point (`[numthreads(...)]`) |

### Intentions

- **Add `#pragma once`** — inserts an include guard at the top of an `.azsl`/`.azsli` file (available when not already present). AZSL is preprocessed with a C-style preprocessor, and O3DE's own `.azsli` files use `#pragma once`; it does not interfere with azslc validation.

### Include Resolution & Auto-detection

Go-to-declaration (`Ctrl+Click`) and completion resolve `#include` targets against the file's directory **and** the O3DE include roots:

- `<engine>/Gems`, `<engine>/Gems/Atom/Feature/Common/Assets/ShaderLib`, `<engine>/Gems/Atom/RPI/Assets/ShaderLib`
- `<project>/ShaderLib`

Engine and project roots are auto-detected when not set in **Settings → Tools → AZSL / azslc**:

- **Engine root** — nearest ancestor of a content root containing `Gems/Atom`
- **Project root** — a directory containing `project.json`, or containing `ShaderLib/viewsrg.srgi`

### .shader Completion

O3DE `.shader` files (the JSON side-car that configures how an `.azsl` is compiled) get code
completion everywhere — no engine, tool path or extra setting required. The schema is static data
transcribed from the engine's own serializers:

| Position in the file | Completion offers |
|----------------------|-------------------|
| Top level | all 15 keys: `Source`, `DrawList`, `DepthStencilState`, `RasterState`, `BlendState`, `GlobalTargetBlendState`, `TargetBlendStates`, `ProgramSettings`, `AddBuildArguments`, `RemoveBuildArguments`, `Definitions`, `ShaderOptions`, `DisabledRHIBackends`, `Supervariants`, `KeepTempFolder` |
| `DepthStencilState` / `RasterState` / `BlendState` / `GlobalTargetBlendState` / `TargetBlendStates` / `StencilOpState` | their members (`depth`, `stencil`, `enable`, `writeMask`, `compareFunc`, `frontFace`/`backFace`, `cullMode`, `fillMode`, blend state, …) |
| Any enum member | engine enum values — `GreaterEqual`, `Keep`, `IncrementSaturate`, `Front`, `Wireframe`, `SrcAlpha`, `Add`, stage names, … |
| Any boolean | `true` / `false` (inserted **unquoted** — the engine's JSON reader rejects `"true"`) |
| `"Source"` value | relative `.azsl` paths found in the project |
| `"DisabledRHIBackends"` entries | `dx12`, `vulkan`, `metal` |

Behaviour:

- Pops up after `{`, `:`, `,`, `"` and while typing; inside quotes only enum/bool values are
  offered, outside quotes only keys (deduped against keys already present in the object).
- Keys are inserted as `"key" : `, enum strings as `"value"`, so a half-typed entry completes to
  valid JSON.
- The engine matches member names **case-insensitively** (keys are compared via lower-cased CRC),
  so `"Depth"` and `"depth"` both load; completion suggests the canonical serialize-context
  spelling. Map keys such as `"0"` in `TargetBlendStates` are left alone.
- Works regardless of which file type owns `.shader` (CLion/Rider's built-in Shader type or the
  JSON mapping suggested above) — the completion provider is text-based and makes no assumptions
  about the file's PSI.

## azslc Validation (O3DE)

The plugin runs the **AZSL Compiler (azslc)** — O3DE's official shader compiler — to validate AZSL shaders with full engine include-path resolution. This mirrors the `ShaderAssetBuilder` pipeline: preprocess with `cpp` → validate with `azslc`.

### Prerequisites

- **O3DE Engine** — installed (provides `azslc` and ShaderLib includes)
- **C Preprocessor (cpp)** — Linux/macOS: `build-essential` / Xcode Command Line Tools; Windows: **MSYS2/MinGW** (`pacman -S mingw-w64-x86_64-cpp`)

### Setup

1. Go to **Settings → Tools → AZSL / azslc**
2. **azslc executable** — auto-detected from `~/.o3de/3rdParty/packages/azslc-*/azslc/bin/Release/azslc`; or set manually
3. **O3DE Engine root** — required (e.g., `/home/user/o3de` or `C:\o3de`). Used to resolve:
   - `Gems` (repo root — for `<Atom/Feature/.../ShaderResourceGroups/...>` includes)
   - `Gems/Atom/Feature/Common/Assets/ShaderLib`
   - `Gems/Atom/RPI/Assets/ShaderLib`
   - `Gems/Atom/Feature/Common/Assets/ShaderResourceGroups`
4. **O3DE Project root** (optional) — the project the shader is built in (e.g. `.../VolumetricClouds/DemoProject`). Provides `ShaderLib/scenesrg.srgi` and `ShaderLib/viewsrg.srgi`, plus gem-local includes
5. **Additional include paths** — add/remove/reorder custom directories
6. **Validation mode** — `SYNTAX` (fast, `--syntax`), `SEMANTIC` (full, `--semantic`, default), `FULL` (`--full`)
7. **Validate on file save only** — toggle to run only on save instead of on every keystroke

### How It Works

When editing an `.azsl` / `.azsli` / `.srgi` file:

1. **Preprocess** — `cpp -nostdinc -undef -w -x c -I<engine Gems> -I<engine ShaderLib> -I<project ShaderLib> -I<gem> file.azsl`
2. **Map line numbers** — `#line` markers are used to map diagnostics back to the original file, then stripped (azslc does not understand them)
3. **Validate** — `azslc --syntax|--semantic|--full <preprocessed>`
4. **Annotate** — errors/warnings appear inline with correct line/column

This matches the O3DE `ShaderAssetBuilder` pipeline, so you see the same errors the build would produce.

### Auto-detection

| Tool | Location |
|------|----------|
| `azslc` | `~/.o3de/3rdParty/packages/azslc-*/azslc/bin/Release/azslc` or PATH |
| `cpp` | PATH (Linux/macOS), MSYS2/MinGW on Windows |

### Windows Notes

azslc requires the C preprocessor (`cpp`). Install MSYS2:
```powershell
# In MSYS2 terminal:
pacman -S mingw-w64-x86_64-cpp
```
Ensure MSYS2 `usr/bin` is in your PATH, or set `cpp` path manually in settings.

## clang-format

Because AZSL isn't a language clang-format recognizes by extension, the built-in JetBrains ClangFormat integration does not engage on `.azsl`/`.azsli` files. This plugin adds its own integration that transparently routes the Reformat Code action through clang-format.

### Setup

1. Go to **Settings → Tools → AZSL / clang-format**
2. The plugin auto-detects `clang-format` from your PATH. Optionally set the path manually
3. Set a fallback style (LLVM, Google, Chromium, Mozilla, WebKit, Microsoft) for files not covered by a `.clang-format`
4. Use Reformat Code (`Ctrl+Alt+L`, or whatever you've mapped it to — e.g. `Alt+Shift+F`) on an `.azsl`/`.azsli` file
5. Optionally enable **Format with clang-format on save** in the same page to format automatically when a file is saved

Your `.clang-format` is discovered automatically by walking up from the file's directory. Range formatting (reformat selection) is supported.

## Building from Source

### Prerequisites

- **JDK 17** (required — the plugin targets Java 17)
- **Gradle 8.10+** (or use the included Gradle wrapper)

### Linux

```bash
# Install JDK 17 (Ubuntu/Debian)
sudo apt-get update && sudo apt-get install -y openjdk-17-jdk-headless

# Or download Eclipse Temurin JDK 17 manually:
# https://adoptium.net/temurin/releases/?version=17

# Clone and build
git clone https://github.com/your-repo/AZSL-Plugin.git
cd AZSL-Plugin

# Make wrapper executable (if needed)
chmod +x gradlew

# Build the plugin
./gradlew build

# The plugin ZIP will be in:
# build/distributions/AZSLPlugin-<version>.zip
```

**Note:** If you have JDK 21+ as default, you must point Gradle to JDK 17. Create/edit `gradle.properties`:
```properties
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
org.gradle.jvmargs=-Xmx2g
```

### Windows

```powershell
# Install JDK 17:
# - Eclipse Temurin: https://adoptium.net/temurin/releases/?version=17
# - Microsoft Build of OpenJDK: https://learn.microsoft.com/en-us/java/openjdk/download
# - Or via winget: winget install EclipseAdoptium.Temurin.17.JDK

# For azslc validation: install MSYS2/MinGW (provides cpp preprocessor)
# https://www.msys2.org/
# Then in MSYS2 terminal:
# pacman -S mingw-w64-x86_64-cpp

# Clone and build
git clone https://github.com/your-repo/AZSL-Plugin.git
cd AZSL-Plugin

# Build the plugin (uses included Gradle wrapper)
.\gradlew.bat build

# The plugin ZIP will be in:
# build\distributions\AZSLPlugin-<version>.zip
```

**Note:** If `gradlew.bat` fails with "Unsupported class file major version", ensure `gradle.properties` points to JDK 17:
```properties
org.gradle.java.home=C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot
org.gradle.jvmargs=-Xmx2g
```

### Verifying the Build

```bash
# Run the plugin in a sandbox IDE (IntelliJ Community) for testing
./gradlew runIde
```

### Maintaining the .shader Schema (Developers)

The `.shader` completion lives in `src/main/java/com/azsl/completion/shader/`:

| File | Role |
|------|------|
| `ShaderFileSchema.java` | static catalog: section keys, enum options, bool keys (no IntelliJ API — kept portable so it can be tested headlessly) |
| `ShaderPathScanner.java` | pure-text scan of `text[0, caret)` → path / key-value position (no PSI assumptions, because CLion/Rider own the `.shader` file type) |
| `ShaderCompletionContributor.java` | IntelliJ completion provider (registered `language="any"`), insert handlers |
| `ShaderTypedHandler.java` | gates auto-popup to positions the scanner resolves to schema positions |

The schema is transcribed from these engine sources (re-sync from them when the engine changes):

- `Gems/Atom/RPI/Code/Source/RPI.Reflect/Shader/ShaderSourceData.cpp` — the 15 top-level keys
- `Gems/Atom/RHI/Code/Source/RHI.Reflect/RenderStates.cpp` + `Include/Atom/RHI.Reflect/RenderStates.h`,
  `SamplerState.h` — depth/stencil/raster/blend states and their enums
- `Gems/Atom/RPI/Code/Include/RPI.Reflect/Shader/ShaderCommonTypes.h` — entry-point stage names
- `Gems/Atom/Asset/Shader/.../ShaderBuildArguments` (`RenderModules/.../ShaderBuildArguments.*`) —
  `AddBuildArguments`/`RemoveBuildArguments` keys; RHI backends `dx12`, `vulkan`, `metal`

After touching `ShaderFileSchema` or `ShaderPathScanner`, run the headless sweep (JDK only — no
Gradle, no network):

```bash
./tools/verify-shader-schema.sh <o3de-engine-root>
```

It compiles both IntelliJ-free classes plus `tools/shader-schema-check/Check.java` and walks **every
caret offset of every `*.shader` file in the engine tree**, asserting that each key and enum/bool
value the engine really uses is offered by the schema (currently 341 files / ~193k offsets / ~358k
assertions). If `javac` is not on your PATH, point the script at a JDK via `JAVAC=...`/`JAVA=...`
or `JBR=/path/to/jbr`. A non-zero exit means the schema fell behind the engine.

## Installation

1. Download the latest `.zip` from the [Releases](../../releases) page, or build it yourself (see above)
2. In CLion/IntelliJ/Rider, go to **Settings → Plugins → ⚙ → Install Plugin from Disk...**
3. Select the `.zip` file
4. Restart the IDE

## Compatibility

- **IntelliJ Platform:** 2024.1 – 2026.1
- **IDE Support:** CLion, IntelliJ IDEA (Community/Ultimate), Rider, PyCharm, WebStorm, etc.
- Works alongside the C/C++ plugin without conflicts

## Credits

- **DOCC** — original HLSL Language Support plugin that this project is derived from (lexer, parser, syntax highlighter, completion, go-to-declaration, folding, brace matching, clang-format integration)
- **Cengiz Terzibas** — AZSL/O3DE support and modifications

## License

This project is licensed under the [MIT License](LICENSE).
