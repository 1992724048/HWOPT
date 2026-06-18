<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="icon.png"><source media="(prefers-color-scheme: light)" srcset="icon.png"><img alt="HWOPT" src="icon.png" width="128"></picture>

# HWOPT — 硬件加速优化模组

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![NeoForge](https://img.shields.io/badge/NeoForge-26.2.0.2-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2%20-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-WIP-red)

[English](README_EN.md)
</div>

HWOPT (Hardware Optimization) 是一个 Minecraft 优化模组，使用 C++ 重写了原版的算法，通过 Java 25 FFM API 实现低开销本地调用。通过 GPU（SYCL/DPC++）加速游戏性能。

## 加入我们
- 811499127 (QQ)

## 项目状态
**正在开发中...** 噪声算法已可用。GPU 加速（SYCL）和剩余优化正在开发。

#### 噪声算法加速

- **ImprovedNoise** — Java -> C++
- **PerlinNoise** — Java -> C++
- **SimplexNoise** — Java -> C++
- **NormalNoise** — Java -> C++
- **BlendedNoise** — Java -> C++

#### 游戏功能

- **村庄出生点** — 可选将世界出生点设置为最近村庄

#### 模组加载器及版本支持情况
| 游戏版本 | NeoForge                                           | Forge | Fabric |
|-----------|----------------------------------------------------|-------|--------|
| 26.2 | ![支持](https://img.shields.io/badge/支持-brightgreen) | ![无计划](https://img.shields.io/badge/无计划-lightgrey) | ![无计划](https://img.shields.io/badge/无计划-lightgrey) |

## 快速开始

### 前置要求
- **Java 25**
- **NeoForge** 26.1.2.75（Minecraft 26.1.2）
- **Windows x64**（原生 DLL 预编译为 win64；其他平台需要自行编译 C++）
### 构建
```bash
cd JAVA
./gradlew build
```
原生 DLL（`hwopt.dll`、`hwopt-sycl.dll`、`Fortran.dll` 等）预置于 `src/main/resources/native/win64/`，构建时自动打包。

C++ 开发者：
| 环境 |
|------|
| Visual Studio 2026 |
| Intel® oneAPI Base Toolkit 2026.0 |
| Intel VTune (可选) / Intel Advisor (可选) |

## License / 许可
MIT License — 参见 [LICENSE](LICENSE)。

## Issue 提交规范

### 标题格式

```
[崩溃/错误/建议/...] 内容描述
```

### 必填内容

- **latest.log**（压缩为 zip 上传）
- **MOD版本** — 请注明版本号
- **复现步骤** — 设置选项、地图种子等
- **是否存在其他 MOD** — 是/否
- **设备信息**
  - CPU / GPU 具体型号
  - 操作系统版本 (如 Windows 11 25H2 26200.8655)
  - 是否无修改系统（原版镜像, 未修改GPU型号、未使用游戏优化工具、系统相关patch工具，其他情况请说明）
  - 是否安装/更新硬件驱动

## PR 合并规范

无过多要求，提交 PR 前请确认以下条件：

- [ ] 是否通过本地编译
- [ ] 是否能够正常运行游戏
- [ ] 是否对修改/添加部分进行简单游戏测试
- [ ] 分支无冲突（非强制）
- [ ] 是否有 AI 参与开发
  - [ ] 是否对 AI 生成代码进行验证/测试
  - [ ] 是否对 AI 生成代码进行检查/可读性修改/可维护性修改
