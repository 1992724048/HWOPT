<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="icon.png"><source media="(prefers-color-scheme: light)" srcset="icon.png"><img alt="HWOPT" src="icon.png" width="128"></picture>

# HWOPT — Hardware-Accelerated Optimization Mod

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![NeoForge](https://img.shields.io/badge/NeoForge-26.1.2.75-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2%20-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-WIP-red)

[中文](README.md)
</div>

HWOPT (Hardware Optimization) is a Minecraft optimization mod that rewrites vanilla algorithms in C++, invoked via the Java 25 FFM API for low-overhead native calls. GPU acceleration (SYCL/DPC++) is used to boost game performance.

## Project Status

**Work in progress.** Noise algorithms are functional. GPU acceleration (SYCL) and remaining optimizations are under development.

### Noise Algorithm Acceleration

- **ImprovedNoise** — native C++ implementation replacing vanilla 3D noise
- **PerlinNoise** — native C++ implementation with multi-octave support
- **SimplexNoise** — native C++ implementation for 2D/3D
- **NormalNoise** — native C++ implementation

### Gameplay

- **Village spawn** — optionally override world spawn to nearest village

### Modloader & Version Support

| Game Version | NeoForge | Forge | Fabric |
|-------------|----------|-------|--------|
| 26.1.2 | ![Supported](https://img.shields.io/badge/Supported-brightgreen) | ![Not Planned](https://img.shields.io/badge/Not%20Planned-lightgrey) | ![Not Planned](https://img.shields.io/badge/Not%20Planned-lightgrey) |
| 26.2 | ![Planned](https://img.shields.io/badge/Planned-brightgreen) | ![Not Planned](https://img.shields.io/badge/Not%20Planned-lightgrey) | ![Not Planned](https://img.shields.io/badge/Not%20Planned-lightgrey) |

## Quick Start

### Prerequisites

- **Java 25**
- **NeoForge** 26.1.2.75 (Minecraft 26.1.2)
- **Windows x64** (native DLLs are pre-built for win64; other platforms require rebuilding C++ code)

### Build

```bash
cd JAVA
./gradlew build
```

Native DLLs (`hwopt.dll`, `hwopt-sycl.dll`, `Fortran.dll`, etc.) are pre-built in `src/main/resources/native/win64/` and bundled automatically.

For C++ developers:

| Environment |
|-------------|
| Visual Studio 2026 |
| Intel® oneAPI Base Toolkit 2026.0 |
| Intel VTune (optional) / Intel Advisor (optional) |

## License

MIT License — see [LICENSE](LICENSE).

## Issue Guidelines

### Title Format

```
[Crash/Bug/Suggestion/...] Brief description
```

### Required Information

- **latest.log** (compressed as zip)
- **Mod version** — please specify the version number
- **Reproduction steps** — configuration options, world seed, etc.
- **Other mods present** — yes/no
- **System information**
  - CPU / GPU model
  - OS version (e.g., Windows 11 25H2 26200.8655)
  - Clean system (unmodified image, no GPU model spoofing, no game optimization tools, no system patching tools; otherwise please specify)
  - Hardware drivers installed/updated

## PR Merge Checklist

No strict requirements beyond confirming the following before submitting a PR:

- [ ] Local build passes
- [ ] Game runs without errors
- [ ] Basic in-game testing of modified/added functionality
- [ ] Branch has no conflicts (not mandatory)
- [ ] AI was involved in development
  - [ ] AI-generated code has been verified/tested
  - [ ] AI-generated code has been reviewed for readability and maintainability
