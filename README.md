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

## 交流频道
- 811499127 (QQ)

## 项目状态
**正在开发中...** 噪声算法已可用。GPU 加速（SYCL）和剩余优化正在开发。

#### 功能列表
| 名称 |类别|状态|模块版本|备注|
|------|----|----|--------|----|
|地形生成优化|优化|已完成|26.1|没有改原版调度|
|实体碰撞优化|优化|待评估|26.1|影响不是很大, 除了刷怪塔之类极端场景|
|实体渲染优化|优化|计划中|26.1||
|实体AI优化  |优化|开发中|26.1||
|村庄出生点  |辅助|已完成|26.1|不是测试也能用|
|125区块视距 |辅助|已完成|26.1|很吃性能|

- 模块版本号为 **年份/修订版本**

#### 模组加载器、游戏版本版本、操作系统、硬件支持情况
| 游戏版本 | MOD版本 | NeoForge | Forge | Fabric | Windows | Linux | Intel CPU | Intel iGPU | Intel dGPU | AMD CPU | AMD iGPU | AMD dGPU | NVIDIA dGPU |
|----------|---------|----------|-------|--------|---------|-------|-----------|------------|------------|---------|----------|----------|-------------|
| 26.2 | 26.1.x | ✓ | ✕ | ✕ | 25H2 | ✕ | ✓ | 11代及以上 | ✓ | ✓ | ✕ | ✕ | CUDA 12.0及以上 |

- **Intel 11代及以前/AMD/NVIDIA 无硬件平台进行测试，表格内容仅为相关文档资料参考**
- 基础功能 CPU 满足任意条件即可
- SYCL/DPC++ (GPU加速) 需要 GPU 满足任意条件
- MOD版本号为 **年份/功能版本/修订版本**

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
- Visual Studio 2026
- Intel® oneAPI Base Toolkit 2026.0
- Intel VTune (可选)
- Intel Advisor (可选)

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
