# 1.1.0 验证与交接记录

日期：2026-10-06。全部本地验证由主会话执行，没有调用子智能体、独立 tester，也没有将编译结果等同于实机渲染或商业发布验收。

## 环境与构建

- Windows x64；编译/单测 JVM：JDK 25.0.2；编译目标 Java 17。
- Gradle Wrapper 9.6.1；LibGDX 1.14.2；LWJGL 3.4.1；JUnit 4.13.2。
- 便携运行时：现有 OpenJDK 25.0.1，经 jlink 裁剪为 `java.base,java.desktop,java.logging,jdk.unsupported`；运行时许可保留。
- Maven Central 在本机返回 HTTP 403，普通在线依赖解析未通过。此次使用原有完整发行目录中的图形依赖及已缓存的 JUnit/Hamcrest，通过明确的 `offlineLibs` 参数构建；没有声称在线构建已验证。
- 原有 Gradle 缓存 Junction 导致 workerMain 锁冲突。使用不带 Junction 的隔离 Gradle 用户目录后，干净构建、单测、build、installDist 成功。离线依赖同时在 Release 归档，方便恢复。

实际本地构建入口：

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-verification'
.\gradlew.bat --offline '-PofflineLibs=.offline-deps/lib' clean test build installDist --no-daemon
.\scripts\verify.ps1 -OfflineLibs .offline-deps/lib
```

## 单元回归

总计 **30**，失败 **0**，错误 **0**。原有 12 项案例保留并迁移到 JUnit 4，新增 18 项回归。

| 测试类 | 数量 | 覆盖 |
|---|---:|---|
| InventoryTest | 3 | 合成消耗、材料不足、创造物品、热栏环绕 |
| PlayerStatsTest | 2 | 生命/食物界限、创造状态 |
| NoiseTest | 2 | 种子确定性、数学 floor |
| WorldGeneratorTest | 4 | 确定地形、安全出生点、地标、编辑记录、越界查询隔离 |
| VoxelRaycasterTest | 5 | 最近块、法线、水与触及范围、正/负整数边界、无效射线 |
| PlayerControllerTest | 6 | 高速薄地板/墙碰撞、鼠标方向、飞行惯性/落速、重生清理 |
| SaveSystemTest | 8 | 中文路径完整往返、腐坏主文件回退、备份不被坏文件污染、无效数据不覆盖、旧版迁移、无效 ID/坐标/空集合、失败目录、临时清理、空存档 |

## 真实 OpenGL 窗口

源码安装分发启动 **7/7**，便携 ZIP 解压后 EXE 启动 **7/7**。

- title：标题画面、随机按钮真实触摸命中顺序。
- world：地形、天空、中文 HUD 与热栏。
- inventory：背包、配方、覆盖层。
- map：旅者地图。
- pause：暂停层。
- night：夜间天空及照明渲染。
- gameplay：实际游戏方法依次执行合成、宝箱奖励防重复、遗迹采集和伙伴解锁、方块放置、死亡状态禁止交互、重生及游戏存档写入/读回。

每个场景均要求退出码 0、PNG 截图存在、独立 JSON 成功标记存在；缺少任一条件就失败。游戏烟雾测试禁用真实用户输入，避免焦点/鼠标/按键污染；测试数据保存到隔离目录，不动真实玩家存档。

常规测试视角视锥剔除后提交 **21/64** 个区块；玩法回归后视角提交 **25/64**。仅证明剔除有效，未测试 FPS 提升比例、长时间稳定性或内存上限。

已人工查看标题、世界、背包与地图截图，确认中文显示和画面内容存在。自动证据归档只含测试世界的 PNG/JSON，不含玩家存档、认证信息或完整进程日志。

便携验证从完整 ZIP 解压到含中文和空格的目录，并对测试进程设置无 Java 的 PATH 和无效 JAVA_HOME，仍通过全部七场景。实际使用的是包内运行时，不是要求用户安装的系统 Java。

## 交付与远程核验

Release `v1.1.0` 交付：Windows x64 内置运行时 ZIP、普通 Java 发行 ZIP、离线依赖 ZIP、窗口证据 ZIP，以及 SHA256SUMS。

源码、说明、测试、字体许可、构建 Wrapper 和脚本纳入 Git；缓存、开发截图/测试存档、构建目录与 Junction 不上传为源码。推送后还需要核对远程提交/源码树、下载 Release 并比对 SHA-256，再进行本地删除。最终远程核验及清理状态以执行后的聊天和长期项目记录为准，本文件不预先声明完成。

GitHub `Verify` 手动工作流从 Release 恢复依赖并执行干净构建和 30 项单测；CI 不运行 GUI，不应把 CI 成功当作显卡兼容性验收。

## 未验证与边界

没有独立 tester、全部显卡覆盖、Windows 10 独立机器验证、长期压力/每秒帧率基准、真实断电存档测试、所有历史存档样本、代码签名、安装器、联机、无限地图或 Linux/macOS 实机验证。无声卡等平台错误会降级音效；本次没有声称人工听感测试通过。
