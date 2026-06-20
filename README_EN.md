<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="icon.png"><source media="(prefers-color-scheme: light)" srcset="icon.png"><img alt="HWOPT" src="icon.png" width="128"></picture>

# HWOPT — Hardware-Accelerated Optimization Mod

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![NeoForge](https://img.shields.io/badge/NeoForge-26.2.0.6-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2%20-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-WIP-red)

[中文](README.md)

</div>

HWOPT (Hardware Optimization) is a Minecraft optimization mod that rewrites vanilla algorithms in C++, using the Java 25 FFM API for low-overhead native calls, with GPU (SYCL/DPC++) acceleration to boost game performance.

## Community

- [pd34429710](https://pd.qq.com/s/fcy3gqy4x) (QQ Channel)

## Project Status

**Work in progress...** GPU acceleration (SYCL) and remaining optimizations are under development.

#### Feature List

| Name | Category | Status | Module Version | Notes |
| ---- | -------- | ------ | -------------- | ----- |
| Terrain Generation Optimization | Optimization | In Development | 26.1 | |
| Entity Collision Optimization | Optimization | In Development | 26.1 | |
| Entity Rendering Optimization | Optimization | In Development | 26.1 | |
| Entity AI Optimization | Optimization | In Development | 26.1 | |
| Village Spawn | Utility | Completed | 26.1 | Works outside testing as well |
| 125-Chunk Render Distance | Utility | Completed | 26.1 | Heavy on performance |

- Module version format: `year/revision`

#### Mod Loader & Game Version Support

| Game Version | Mod Version | NeoForge | Forge | Fabric |
| ------------ | ----------- | -------- | ----- | ------ |
| 26.2 | 26.1.x | ✓ | ✕ | ✕ |

#### OS & Hardware Support

| Mod Version | Windows | Linux | Intel CPU | Intel iGPU | Intel dGPU | AMD CPU | AMD iGPU | AMD dGPU | NVIDIA dGPU |
| ----------- | ------- | ----- | --------- | ---------- | ---------- | ------- | -------- | -------- | ----------- |
| 26.1.x | ✓ | ✕ | ✓ | 11th Gen+ | ✓ | ✓ | ✕ | ✕ | CUDA 12.0+ |

- `Intel 11th Gen and earlier`/`AMD`/`NVIDIA` — no hardware available for testing; table content is based on documentation references
- Basic CPU functionality: any qualifying CPU works
- SYCL/DPC++ (GPU acceleration): requires a qualifying GPU
- Mod version format: `year/feature version/revision`

#### FAQ

1. Why is this more suitable for `iGPU` than `dGPU`?

- This mod uniformly uses `host` memory. Compared to `device` or `shared` memory, this avoids the copy overhead to the `device`, making it better suited for `iGPU`s where system memory doubles as VRAM. Modern `iGPUs` are typically connected via the `Ring Bus` rather than PCIE, and come with large caches. `iGPUs` can directly access system memory through the `Ring Bus` (some models have L3/L4 caches) without copying. This approach is unsuitable for `dGPUs` connected via PCIE due to latency and frequent copy issues.

## Quick Start

### Prerequisites

- **Java 25**
- **NeoForge** 26.1.2.75 (Minecraft 26.1.2)
- **Windows x64** (native DLLs pre-built for win64; other platforms require manual C++ build)

### Build

```bash
cd JAVA
./gradlew build
```

Native DLLs (`hwopt.dll`, `hwopt-sycl.dll`, `Fortran.dll`, etc.) are pre-placed at `src/main/resources/native/win64/` and bundled automatically during build.

C++ Developers:

- Visual Studio 2026
- Intel® oneAPI Base Toolkit 2026.0
- Intel VTune (optional)
- Intel Advisor (optional)

## Issue Submission Guidelines

### Title Format

```
[Crash/Bug/Suggestion/...] Description
```

### Required Information

- **latest.log** (compress as zip and upload)
- **Mod version** — please specify the version number
- **Reproduction steps** — configuration options, world seed, etc.
- **Other mods present** — yes/no
- **Device information**
  - CPU / GPU model
  - OS version (e.g., Windows 11 25H2 26200.8655)
  - Clean system (unmodified image, no GPU model spoofing, no game optimization tools, no system patching tools; otherwise please specify)
  - Hardware drivers installed/updated

## PR Merge Guidelines

No strict requirements beyond confirming the following before submitting a PR:

- [ ] Local build passes
- [ ] Game runs without errors
- [ ] Basic in-game testing of modified/added functionality
- [ ] Branch has no conflicts (not mandatory)
- [ ] AI was involved in development
  - [ ] AI-generated code has been verified/tested
  - [ ] AI-generated code has been reviewed for readability/maintainability
