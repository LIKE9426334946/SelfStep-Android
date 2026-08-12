# SelfStep

SelfStep 是一个完全离线的 Android 自律管理应用，使用 Kotlin 和 Jetpack Compose 开发。

## 功能

- 管理“必须完成”和“禁止事项”两类每日任务
- 在今日清单中勾选或取消勾选
- 自动统计当前连续天数、最长连续天数和累计达标天数
- 保存每天的任务快照，修改或删除任务不会影响过去的历史记录
- 支持浅色和深色主题

## 本地数据

应用不申请网络权限，也不使用数据库。全部数据保存在应用私有目录的 `selfstep_data.json` 中，每次修改后立即写入；重新打开应用时会自动读取。

## 构建

使用 Android Studio 打开项目并运行 `app`，或执行：

```bash
./gradlew test assembleDebug
```
