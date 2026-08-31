# 记忆球 (Remembrall)

一款基于「时间 + 地点」的智能提醒 Android App：不仅能在指定时间提醒你，还能在你到达或离开某个地方时提醒你。所有数据保存在本地，开源、免费、无广告。

## 功能

| 功能 | 说明 |
|------|------|
| **时间提醒** | 某个具体时间提醒，支持重复（一次/每天/工作日/每周自定义） |
| **地点提醒** | 到达/离开预设地点时提醒（半径默认 50m，可调 30–1000m） |
| **组合提醒** | 时间段内 + 处于/不处于某地点时提醒 |
| **闹铃提醒** | 系统闹铃音频通道 + 全屏弹窗，接近闹钟 App 体验 |
| **常驻通知** | 通知栏长期停留，不易被清除 |
| **常用地点** | 地图拖动标记"家""公司"等，快速复用 |
| **省电设计** | 位置监测按需唤醒：无地点需求时服务自动停止，不常驻轮询 |

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **架构**: MVVM + Repository
- **数据库**: Room
- **地图/定位**: 高德地图 SDK (3dmap 10.0.600，内置定位)
- **后台**: AlarmManager (setAlarmClock 精确闹铃) + Foreground Service
- **权限管理**: Accompanist Permissions

## 项目结构

```
app/src/main/java/com/remembrall/app/
├── MainActivity.kt              # 入口 + 导航 + 权限申请
├── RemembrallApp.kt             # Application 初始化
├── data/
│   ├── model/                   # Reminder, Place 实体
│   ├── db/                      # Room DAO + Database
│   └── repo/                    # Repository 层
├── engine/
│   ├── AlarmScheduler.kt        # 闹铃调度 (AlarmManager)
│   ├── AlarmReceiver.kt         # 闹铃广播 + 关闭处理
│   ├── BootReceiver.kt          # 开机/更新后重设闹铃
│   └── LocationMonitorService.kt # 按需位置监测前台服务
├── notify/
│   ├── NotifyHelper.kt          # 通知渠道管理
│   ├── ReminderNotifier.kt      # 发送提醒通知
│   └── AlarmService.kt          # 闹铃响铃前台服务
├── ui/
│   ├── home/                    # 提醒列表首页
│   ├── edit/                    # 新建/编辑提醒（滚轮时间选择）
│   ├── places/                  # 常用地点管理
│   ├── map/                     # 地图选点
│   ├── ring/                    # 全屏响铃界面
│   ├── settings/                # 设置 + MIUI 适配指南
│   └── common/                  # 共享组件（卡片/弹窗/空状态）
├── util/                        # TimeUtils, GeoUtils
└── theme/                       # Compose 主题
```

## 构建

环境要求：JDK 17、Android SDK 34、Gradle 8.7（或用仓库自带 wrapper）。

```bash
# 1. 配置本机环境
cp local.properties.example local.properties
#   编辑 local.properties，填入 sdk.dir 与（可选）AMAP_KEY

# 2. 编译 Debug APK
./gradlew assembleDebug

# 3. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

未配置 `AMAP_KEY` 时应用可以正常编译运行，仅地图选点降级为手动输入经纬度。

## 高德 Key

在 [高德开放平台](https://lbs.amap.com/) 申请 Android SDK Key，绑定你自己的包名与签名 SHA1，
写入 `local.properties` 的 `AMAP_KEY=` 字段，编译时自动注入 Manifest 与 BuildConfig。

## Release 签名（可选）

在项目根目录创建 `keystore.properties`（已被 .gitignore 忽略）：

```properties
storeFile=keystore/your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

然后 `./gradlew assembleRelease` 即产出签名包；不创建该文件时 release 构建为未签名。

⚠️ 请务必备份你的 keystore，应用更新要求签名一致。

## 后台保活说明

国内 ROM 的后台管理较激进，为保证闹铃和地点提醒不丢失，用户需在系统设置中开启（App 内"设置"页有快捷引导，以小米/MIUI 为例）：

1. **自启动**：设置 → 应用管理 → 记忆球 → 自启动
2. **省电策略**：设为"无限制"
3. **后台定位**：允许"始终允许"
4. **后台弹出界面**：允许（锁屏闹铃需要）
5. **通知权限**：允许
6. **后台锁定**：最近任务界面下拉卡片锁定

## 已知限制

- 高德 3D 地图 SDK 体积较大（APK 约 46MB）
- 无 Key 时地图功能降级为手动输入坐标
- 各厂商 ROM 的后台限制可能导致地点提醒延迟，需用户配合完成上述设置

## 开源协议

[MIT](LICENSE)
