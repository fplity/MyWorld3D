# 方境 · BlockHorizon

一个从零实现的 Java 3D 体素沙盒游戏。世界、贴图、天气与音效均由程序生成，中文字体随包附带。当前整合版：**1.1.0**。

这是 Minecraft 风格的独立单人小游戏，不是 Mojang/Microsoft 官方产品，也不包含其游戏资源。世界范围为 **64 × 64**，建筑高度上限 **40**；没有无限地图、联机服务器或完整 Minecraft 模组系统。

## 下载、启动、发给朋友

到本仓库 [Releases](https://github.com/fplity/MyWorld3D/releases/tag/v1.1.0) 下载 `BlockHorizon-1.1.0-windows-x64.zip`。

1. 完整解压 ZIP，不要在压缩包内直接运行。
2. 打开 `BlockHorizon` 文件夹，双击 `BlockHorizon.exe`。
3. 便携包内置裁剪后的 OpenJDK 25 运行时，**不需要朋友另外安装 Java、Gradle或下载依赖**。转发时发送完整 ZIP，不能只发送 EXE。

目标平台是 Windows 10/11 x64，显卡需支持 OpenGL 3.2+。便携 EXE 未做代码签名，Windows 可能提示未知发布者；请核对 Release 上的 SHA-256 校验文件。仓库为私有仓库，朋友不能直接访问时，可以由仓库所有者下载 ZIP 后转发。

`BlockHorizon-1.1.0.zip` 是较小的普通 Java 发行包，需要已安装 Java 17+，解压后运行 `bin/BlockHorizon.bat`。

## 直接运行

系统需要 Java 17 或更高版本。项目已自带 Gradle Wrapper，无需单独安装 Gradle。

在 Windows 上双击：

```text
run-game.bat
```

也可以在 PowerShell 中运行：

```powershell
.\gradlew.bat run
```

第一次启动会自动下载 LibGDX/LWJGL 依赖，后续可离线运行。

## 已实现玩法

- 程序化种子世界：草甸、森林、沙漠、雪原、高地、海岸、洞穴与地下晶体
- 第一人称移动、冲刺、跳跃、游泳、方块碰撞、坠落伤害和创造飞行
- 连续采集、射线选块、方块放置、九格热栏和粒子反馈
- 背包与四种配方：木板、石砖、星辉石、装饰树篱
- 生命、饱食、体力、浆果食物、回血和死亡重生
- 5 分钟昼夜循环、太阳/月亮、星空、云层、动态雾、雨、雷电和流星
- 友善的“小苔团”、夜间敌对生物“影徘徊者”及战利品
- 林间小屋、古旧宝箱、回响遗迹、分阶段探索任务与成就
- 小地图、调试面板、隐藏 HUD、游戏截图和程序化音效
- 自动存档、手动存档、继续游戏、随机种子与创造/生存切换
- 一个藏在遗迹深处、会真正改变旅程的小惊喜

## 操作

| 按键 | 功能 |
|---|---|
| `W A S D` | 移动 |
| 鼠标 | 环顾 |
| 左键按住 | 采集方块 / 攻击生物 |
| 右键 | 放置方块 / 打开宝箱 |
| `Space` | 跳跃 / 水中上浮 |
| `Shift` | 冲刺 |
| `1`—`9` / 滚轮 | 选择热栏 |
| `E` | 背包与合成 |
| `M` | 旅者地图 |
| `R` | 食用浆果 |
| `C` | 切换生存/创造模式 |
| `F` | 创造模式下切换飞行 |
| `Ctrl` | 飞行下降 |
| `T` | 创造模式下切换天气 |
| `F1` | 隐藏 HUD |
| `F2` | 保存截图 |
| `F3` | 调试信息 |
| `F5` | 手动存档 |
| `Esc` | 暂停 / 返回 |

死亡后按 `R` 重生。地图、背包打开时移动输入暂停，但世界时间和生存模拟仍继续；`Esc` 才会暂停世界。

## 存档与截图

Windows 默认存储在 `%LOCALAPPDATA%\BlockHorizon\`，不再依赖 EXE 所在目录。

- `saves/world.json`：当前世界（单一存档槽）。
- `saves/world.json.bak`：上一份验证有效的存档。当前文件损坏时自动尝试备份。
- `screenshots/`：`F2` 截图。

按 `F5` 手动保存，也会约每 42 秒自动保存、暂停时保存，并在正常关闭游戏窗口时保存。写入先完成临时文件，再替换主存档；文件系统不支持原子移动时回退普通替换，不承诺在所有硬件故障下零丢失。备份代表上一次快照，不一定是最后一秒的状态。

旧版本的 `BlockHorizon/saves/world.json` 若仍在当前启动目录下，首次继续游戏会尝试迁移并保留原文件。旧存档也可以手动复制到新的 `saves/` 目录。支持版本 1/2 的正常存档读取；新版本为 3，新增建筑次数、旅行距离持久化。创建新世界会使用同一个存档槽；重要世界请先手动备份。

非 Windows 默认是用户主目录 `.blockhorizon/`；开发测试可用 JVM 参数 `-Dblockhorizon.dataDir=指定目录` 覆盖。截图和测试存档不会提交到仓库或公开发布。

## 测试与打包

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat installDist
```

打包完成后，可直接运行：

```text
build\install\BlockHorizon\bin\BlockHorizon.bat
```

## 技术结构

- `world`：方块定义、噪声、世界生成、体素射线与编辑记录
- `render`：运行时像素图集、分块网格、天空天气和粒子
- `player`：移动物理、生存状态、背包与配方
- `entity`：生物行为、战斗和伙伴系统
- `screen` / `ui`：标题、加载、游戏循环与中文界面
- `save`：JSON 本地存档
- `audio`：Java Sound 实时合成音效

第三方库与字体许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 1.1.0 修复与优化

- 修复随机按钮点击优先级、鼠标水平转向、整数边界射线距离、越界坐标打包别名。
- 高速移动分段检测并贴近碰撞边界，避免穿过薄墙/地板；水中按住空格持续上浮。
- 修复飞行惯性、重生残留飞行状态和从实体方块内退出创造/飞行的问题。
- 死亡后禁止放置和领取交互，敌人造成致命伤时立即进入死亡状态；攻击不会穿过射线命中的墙面。
- 修复窗口退出时当前 Screen 未释放/未保存，保存失败时不直接丢弃当前世界。
- 存档校验、同目录临时写入、有效备份、旧格式迁移，以及建筑/旅行进度保存。
- 根据相机视锥剔除区块；复用移动临时向量；雨滴改用有限相位，避免系统运行时间过长后的浮点精度问题。
- 修正 Windows Wrapper 构建失败退出码；提供可复现的验证、免 Java 便携包构建与离线依赖归档。

完整交接见 [项目说明](docs/PROJECT.md)、[验证记录](docs/VERIFICATION.md) 和 [更新日志](CHANGELOG.md)。

## 可复现验证与离线构建

```powershell
.\scripts\verify.ps1
.\scripts\package-windows.ps1 -RuntimeJdk 'C:\path\to\openjdk-25'
```

如果 Maven 无法连接，可从本次 Release 下载 `BlockHorizon-1.1.0-offline-deps.zip`，解压到 `.offline-deps/`（其中包含 `lib/`），然后：

```powershell
.\gradlew.bat --offline '-PofflineLibs=.offline-deps/lib' clean test build installDist --no-daemon
.\scripts\verify.ps1 -OfflineLibs .offline-deps/lib
.\scripts\package-windows.ps1 -OfflineLibs .offline-deps/lib -RuntimeJdk 'C:\path\to\openjdk-25'
```

第一次使用 Gradle Wrapper 仍需联网下载 Gradle，或者预置相同 Wrapper 发行包；离线依赖 ZIP 并不是完整离线开发环境。普通构建使用 Maven 固定版本依赖；离线开关只读取显式指定目录中的 JAR。测试使用 JUnit 4.13.2，原有测试与新增回归均保留。

GitHub Actions 的 `Verify` 工作流支持手动运行，使用 Release 归档依赖执行干净构建与单元测试；CI 不代替真实 OpenGL 窗口验证。
