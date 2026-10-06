# 1.1.0 完成交接

## 获取与启动

- 公开源码：[fplity/MyWorld3D](https://github.com/fplity/MyWorld3D)。
- 游戏下载：[v1.1.0 Release](https://github.com/fplity/MyWorld3D/releases/tag/v1.1.0)。
- 给朋友的版本：`BlockHorizon-1.1.0-windows-x64.zip`，完整解压后双击 `BlockHorizon/BlockHorizon.exe`，不需安装 Java。
- 普通 Java ZIP、离线依赖 ZIP、窗口证据 ZIP 与 SHA256SUMS 同样保留在 Release。

## 完成内容

整合保留世界生成、采集建造、背包与合成、生存、昼夜天气、生物战斗、宝箱、任务、成就、地图、截图、音效与遗迹伙伴；修复输入、射线、碰撞、死亡交互和退出保存，加入视锥剔除与稳定雨滴相位，升级存档安全和 Windows 便携交付。

存档改为独立用户目录，当前 JSON 与上一次有效快照分开保存。建筑与旅行计数持久化；旧版正常存档可迁移。游戏仍为单人、有限地图、单存档槽，没有联机、无限世界、模组平台或代码签名。

## 已验证

- 本地 30 项单元回归：0 失败、0 错误。
- 7 个真实 OpenGL 场景与 7 个免系统 Java 的便携 EXE 场景通过。
- 从 GitHub 新克隆的源码，使用从 Release 下载的依赖再次干净构建并通过 30 项测试。
- GitHub Windows Java 17/25 两个 CI 任务成功，见 [验证运行](https://github.com/fplity/MyWorld3D/actions/runs/37546936910)。
- 五个附件下载回读 SHA-256 与上传前相同；公开下载已验证 HTTP 200。

完整测试范围和未覆盖项见 [VERIFICATION](VERIFICATION.md)，玩法与技术设计见 [PROJECT](PROJECT.md)，修复清单见 [CHANGELOG](../CHANGELOG.md)。这些结论不等于独立 tester 或商业发布认证。

## 清理与恢复

用户授权上传完成后删除本地 `D:\app\CodexProject\MyWorld3D`。删除前必须先确认远程源码及安装包完整，并处理工程内指向全局 Gradle 的 Junction：只解除链接，不删除外部缓存。其他项目与独立用户数据目录不属于清理范围。

实际清理是否被系统锁或安全策略阻挡，以最终聊天和外置长期项目记录为准；这里不把授权或计划当作已经执行。需要恢复源码时重新克隆公开仓库，需要玩游戏时直接下载 Release。
