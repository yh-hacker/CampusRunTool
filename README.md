# CampusRunTool

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://developer.android.com/about/dashboards)
[![Version](https://img.shields.io/badge/version-2.0-orange.svg)](https://github.com/yh-hacker/CampusRunTool/releases)

一个集成虚拟定位与 NFC 校园跑打卡功能的 Android 工具应用。

## 功能特性

### 虚拟定位

- **手绘路线模式**：在地图上按住拖动画出路线，设定速度后自动沿路线平滑移动
- **摇杆模式**：无需画路线，通过悬浮窗摇杆实时控制方向和速度
- 支持步行/跑步/骑行/驾车速度预设，运行中可实时调整
- 循环路线、暂停/继续/停止、前台通知控制
- 多地图图层（高德、OpenStreetMap、Esri 卫星、自定义 XYZ）
- 支持 WGS-84 / GCJ-02 坐标系自动转换

### NFC 校园跑打卡

- 模拟 NFC 签到，支持主流校园跑 App
- 三级回退机制：系统 NFC → 微信 → 支付宝
- 液态玻璃风格 UI，动态光斑背景

### 通用功能

- 悬浮速度窗：在其他应用上方显示速度，支持拖动和快捷调整
- 设置页：模拟模式切换、悬浮窗管理、地图图层选择、自检工具
- 状态自动保存：路线、速度、循环、图层等偏好持久化

## 快速开始

### 虚拟定位

1. 安装 APK，打开应用，同意使用声明，授予定位与通知权限
2. 开启开发者选项：
   - 设置 → 关于手机 → 连续点击"版本号"7 次
   - 设置 → 开发者选项 → **选择模拟位置信息应用** → 选择本应用
3. **路线模式**：
   - 点击"绘制路线"，单指按住拖动画线（双指可缩放地图）
   - 可多次绘制，点"结束绘制"完成
   - 输入速度（km/h），可选循环，点击"开始模拟"
4. **摇杆模式**：
   - 设置 → 模拟模式 → 摇杆模式（需要悬浮窗权限）
   - 返回主页点击"开始模拟"
   - 通过悬浮窗摇杆控制方向，松开即停

### NFC 打卡

1. 点击首页顶栏"打卡"按钮进入
2. 确保手机 NFC 功能已开启
3. 页面顶部状态胶囊显示绿色「NFC: 就绪」表示正常
4. 将手机贴近打卡墙 NFC 标签，URL 自动填充到输入框
5. 点击「开始签到」按钮，自动跳转到支付宝完成打卡
6. 如果 NFC 读取失败，可手动在输入框中粘贴 URL

## 构建

### 环境要求

- JDK 17+
- Android SDK Platform 36
- Build-Tools 36.0.0
- Gradle 8.14.3

### 编译步骤

```bash
# 克隆项目
git clone https://github.com/yh-hacker/CampusRunTool.git
cd CampusRunTool

# 编译 Debug 版本
./gradlew assembleDebug

# 运行单元测试
./gradlew :app:testDebugUnitTest
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

### 注意事项

如果项目路径包含非 ASCII 字符（如中文），需要在 `gradle.properties` 中添加：

```properties
android.overridePathCheck=true
```

## 项目结构

```
app/src/main/java/com/campsruntool/yh/
├── MainActivity.java               地图、绘制、速度、状态与权限
├── SettingsActivity.java           设置页
├── campusrun/                      NFC 校园跑打卡模块
│   ├── CampusRunActivity.java      打卡主界面
│   ├── CampusRunAboutActivity.java 关于页面
│   └── CampusRunGlassBackgroundView.java 液态玻璃背景
├── geo/                            几何计算、坐标转换、路径模拟
│   ├── CoordConverter.java         WGS-84 / GCJ-02 转换
│   ├── GeoMath.java                Haversine 距离与方位角
│   ├── RoutePath.java              路径累计距离与插值
│   ├── RouteSimplifier.java        Douglas-Peucker 简化
│   └── RouteSimulator.java         路径推进模拟器
├── map/                            osmdroid 图层与瓦片源
├── service/                        前台定位服务
│   ├── MockLocationService.java    核心服务
│   ├── ServiceDiagnostics.java     诊断信息
│   └── SimulationSnapshot.java     状态快照
├── ui/                             自定义视图
│   ├── DrawOverlayView.java        手绘覆盖层
│   ├── FloatingSpeedWindow.java    悬浮速度窗
│   ├── JoystickView.java           摇杆控件
│   ├── DiagnosticsDialog.java      自检对话框
│   └── LayerPickerDialog.java      图层选择器
└── util/                           工具类
    ├── DeviceState.java            设备状态检测
    ├── FormatUtils.java            格式化工具
    ├── MockLocationHelper.java     模拟定位辅助
    ├── Prefs.java                  SharedPreferences 封装
    └── VersionUtils.java           版本比较
```

## 常见问题

### 开发者选项列表里找不到本应用

确认安装的是最新构建的 APK。部分 ROM 会缓存列表，可尝试重启手机或通过 ADB 授权：

```bash
adb shell appops set com.campsruntool.yh android:mock_location allow
```

### 模拟已启动但位置不变

进入设置 → 自检，查看各项状态。常见原因：

1. **未选中模拟位置应用**：自检显示"未选中"，到开发者选项重新选择
2. **系统定位关闭**：自检显示"已关闭"，打开定位服务
3. **仅有近似位置权限**：自检显示"未授予精确位置"，到应用设置修改
4. **Wi-Fi 干扰**：部分机型会被融合定位覆盖，尝试关闭 Wi-Fi
5. **Provider 添加失败**：自检会显示异常信息，常见于厂商 ROM 限制

### 绘制路线时如何缩放地图

- **单指**：画线
- **双指**：缩放和拖动地图

开始双指操作时，当前未抬手的那一段笔画会被丢弃，避免误画；松开双指后重新单指画线即可。

### 摇杆模式如何使用

1. 设置 → 模拟模式 → 摇杆模式
2. 返回主页点击"开始模拟"
3. 悬浮窗中出现半透明摇杆和 `−/+` 速度按钮
4. 拖动摇杆控制方向，松开即停
5. 点摇杆头切换锁定/解锁状态

### 如何切换地图图层

设置 → 地图图层 → 选择高德 / OpenStreetMap / Esri / 自定义 XYZ

切换图层且坐标系不同时，自动换算已绘制的路线。

## 技术实现

### 无 ROOT 模拟定位

`MockLocationService` 是前台服务（`foregroundServiceType="location"`），启动后：

1. `LocationManager.addTestProvider(GPS / NETWORK / FUSED, ...)` 注册测试 provider
2. 每 200 ms 调用 `setTestProviderLocation()` 写入当前模拟位置
3. `Location` 中填充经纬度、海拔、精度、方向、速度、时间、`elapsedRealtimeNanos`
4. 停止时 `setTestProviderEnabled(false)` + `removeTestProvider()`

### 坐标系处理

- 地图上的绘制点、路线、标记与地图中心统一使用当前图层的坐标系
- 高德图层是 GCJ-02，OpenStreetMap/Esri 是 WGS-84
- 只有与系统定位交互时才转换：启动模拟时把 GCJ-02 路线转换为 WGS-84
- 切换图层且坐标系不同时，自动换算已有路线和地图中心

### 路线推进

纯 Java 几何层不依赖 Android：

- `GeoMath`：Haversine 距离与方位角
- `RoutePath`：累计距离、按行进距离插值、前后几米差分计算朝向
- `RouteSimulator`：`traveled += speed × Δt`，支持暂停、完成、循环取模
- `RouteSimplifier`：Douglas-Peucker 简化手绘点

服务用 `SystemClock.elapsedRealtime()` 计算真实 Δt，避免 Handler 延迟累积误差。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 免责声明

本应用通过 Android 调试 API 模拟位置，**仅用于开发测试、教学演示与定位功能调试**。

请勿用于：
- 游戏作弊
- 考勤/校园跑打卡
- 其他违规用途

部分应用可以检测模拟位置，不同厂商系统也可能存在限制；使用即表示你了解并自行承担相关责任。

## 许可证

Copyright 2026 YH-Hacker

本项目基于 [Apache License 2.0](LICENSE) 开源。

虚拟定位模块基于 [ZCShou/GoGoGo](https://github.com/ZCShou/GoGoGo) 的核心思路开发，感谢原作者的开源贡献。

## 致谢

- [ZCShou/GoGoGo](https://github.com/ZCShou/GoGoGo)：无 ROOT 模拟定位思路参考
- [osmdroid](https://github.com/osmdroid/osmdroid)：地图渲染（Apache-2.0）
- [Material Components for Android](https://github.com/material-components/material-components-android)：UI 组件
- 地图数据归各自提供商所有：© OpenStreetMap contributors、© 高德地图、© Esri

---

**作者**：YH-Hacker  
**GitHub**：[github.com/yh-hacker/CampusRunTool](https://github.com/yh-hacker/CampusRunTool)
