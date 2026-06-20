<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="icon.png"><source media="(prefers-color-scheme: light)" srcset="icon.png"><img alt="HWOPT" src="icon.png" width="128"></picture>

# HWOPT — 硬件加速优化模组

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![NeoForge](https://img.shields.io/badge/NeoForge-26.2.0.6-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2%20-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-WIP-red)

[English](README_EN.md)

</div>

HWOPT (Hardware Optimization) 是一个 Minecraft 优化模组，使用 C++ 重写原版算法，通过 Java 25 FFM API 实现低开销本地调用，并利用 GPU（SYCL/DPC++）加速游戏性能。

## 交流频道

- [pd34429710](https://pd.qq.com/s/fcy3gqy4x) (QQ 频道)

## 项目状态

**正在开发中...** GPU 加速（SYCL）及其他优化仍在开发中。

#### 功能列表

| 名称         | 类别 | 状态   | 模块版本 | 备注               |
| ------------ | ---- | ------ | -------- | ------------------ |
| 地形生成优化 | 优化 | 开发中 | 26.1     |                    |
| 实体碰撞优化 | 优化 | 开发中 | 26.1     |                    |
| 实体渲染优化 | 优化 | 开发中 | 26.1     |                    |
| 实体AI优化   | 优化 | 开发中 | 26.1     |                    |
| 村庄出生点   | 辅助 | 已完成 | 26.1     | 非测试环境亦可使用 |
| 125区块视距  | 辅助 | 已完成 | 26.1     | 性能开销较大       |

- 模块版本号为 `年份/修订版本`

#### 模组加载器、游戏版本支持情况

| 游戏版本 | MOD版本 | NeoForge | Forge | Fabric |
| -------- | ------- | -------- | ----- | ------ |
| 26.2     | 26.1.x  | ✓        | ✕     | ✕      |

#### 操作系统、硬件支持情况

| MOD版本 | Windows  | Linux | Intel CPU | Intel iGPU | Intel dGPU | AMD CPU | AMD iGPU | AMD dGPU | NVIDIA dGPU  |
| ------- | -------- | ----- | --------- | ---------- | ---------- | ------- | -------- | -------- | ------------ |
| 26.1.x  | ≥10 19H1 | ✕     | AVX2      | 11th Gen+  | ✓          | AVX2    | ✕        | ✕        | CUDA 12.0+\* |

- `Intel 11代及更早`/`AMD`/`NVIDIA` 无硬件平台进行测试，表格内容仅为相关文档资料参考
- 基础功能 CPU 满足任意条件即可
- SYCL/DPC++ (GPU加速) 需要硬件、驱动、运行时支持，详情参见 [Intel oneAPI 系统要求](https://www.intel.com/content/www/us/en/developer/articles/system-requirements/intel-oneapi-base-toolkit-system-requirements.html)
- MOD版本号为 `年份/功能版本/修订版本`
- 预编译二进制仅提供 Windows x64；自行编译可支持 Linux 及其他 GPU 后端
- `*` NVIDIA/AMD GPU 后端基于 Codeplay 插件，该插件已停止分发，后续更新可能受影响

#### 常见问题

1. 为什么更适合 `iGPU` 而不是 `dGPU`？

- 在 SYCL 编程模型中，内存模型分为三类：`host` 内存（即系统内存）、`device` 内存（即显存/VRAM）和 `shared` 内存（由驱动自动迁移，但仍存在隐式拷贝）。多数 GPU 加速方案使用 `device` 内存，需要反复在系统内存与显存之间拷贝数据，开销较大；`shared` 内存虽简化了编程，但底层仍依赖驱动进行数据迁移，无法彻底消除拷贝。本模组统一使用 `host` 内存，彻底省去了这部分拷贝开销，因此对"内存即显存"的 `iGPU`（核显）尤为友好。现代 `iGPU` 通常通过 `Ring Bus` 总线（而非 `PCIE`）与 CPU 相连，并配备大容量缓存，可直接访问系统内存（部分型号还带有 `L3`/`L4` 缓存），无需额外拷贝。而 `dGPU`（独显）通过 `PCIE` 连接，延迟较高且必须频繁拷贝数据，该方案并不适合。

2. 为什么选择 SYCL 而不是 Vulkan 计算着色器？

- SYCL 是单源 C++ 编程模型，可直接在 C++ 代码中编写 GPU 核函数，无需学习 GLSL/HLSL 等着色器语言，代码复用和移植成本更低。Vulkan 计算着色器需要管理复杂的管线状态、内存屏障和描述符集，开发效率较低。SYCL 的 C++ 集成度和跨平台性显著优于 Vulkan。

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

原生 DLL（`hwopt.dll`、`hwopt-sycl.dll`、`Fortran.dll` 等）存放于 `src/main/resources/native/win64/`，构建时自动打包。

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
- **Mod 版本** — 请注明版本号
- **复现步骤** — 配置选项、地图种子等
- **是否安装其他 Mod** — 是/否
- **设备信息**
  - CPU / GPU 具体型号
  - 操作系统版本 (如 Windows 11 25H2 26200.8655)
  - 系统是否未经过修改（原版镜像，未修改 GPU 型号、未使用游戏优化工具及系统相关 patch 工具，其他情况请说明）
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
