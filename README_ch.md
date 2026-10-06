# Glyphsmith 字匠

[简体中文](README.md) | [English](readme_en.md)

把任意图片转换成字符画 —— 安卓客户端。三种模式：**中文**、**ASCII**、**彩色**。

示例图：

![Glyphsmith_1](https://github.com/LANZHOU-1/glyphsmith-android/blob/main/Glyphsmith_1.png)

![Glyphsmith_2](https://github.com/LANZHOU-1/glyphsmith-android/blob/main/Glyphsmith_2.png)

![Glyphsmith_3](https://github.com/LANZHOU-1/glyphsmith-android/blob/main/Glyphsmith_3.png)

> 亮处留空、暗处用笔画密的字，所以整幅画是「用字堆出来的照片」。

## 功能

- **三种模式**：中文（`丶一十口日目田回無疆龘` 梯度）、ASCII（` .:-=+*#%@`）、彩色（逐字保留原图像素颜色）
- **彩色模式两种样式**：字符（带颜色的 ASCII 字符）/ 色块（像素风马赛克）
- **可调参数**：宽度 30–240 字符、对比度 1.0–3.0、预览字号、黑/白背景、自动色阶、深色背景反相
- **导出与分享**：复制到剪贴板、保存 TXT、保存彩色网页（HTML）、保存 PNG、系统分享（文本 / PNG）
- **PNG 导出**：按单元格逐字渲染（不是截屏），长边自动限制在 4096 px 内防 OOM，色块样式无缝拼接
- **内置示例图**，打开即见效果；无需联网，**不申请任何权限**
- Material Design 3 界面，Android 12+ 跟随壁纸动态取色；页面转场 / 模式切换 / 预览交叉淡化动画；旋转屏幕或切页保留参数

体积约 1.9 MB，支持 Android 9（API 28）及以上。


## 实现说明

Glyphsmith

1. 把图片缩放到「列数 × 行数」——中文模式按字符宽高比 1:1、ASCII / 彩色按 1:2 换算，保证成品不变形
2. 逐像素计算亮度 `0.2126R + 0.7152G + 0.0722B`；可选**自动色阶**（按 1% 截断做直方图拉伸）与**对比度增强**
3. 按亮度查字符梯度：亮 → 空格 / 笔画疏，暗 → 笔画密（中文梯度从 `丶` 到 `龘`）
4. 彩色模式跳过灰度查表，直接把每个像素的颜色套到对应字符上，逐字上色
5. PNG 导出用 `Canvas` 把每个格子单独绘制（中文 / 色块为正方形格，ASCII 为 0.6 倍宽），长边超过 4096 px 时整体等比缩小

## 第三方组件

| 组件 | 用途 | 许可 |
| --- | --- | --- |
| Jetpack Compose / AndroidX（core-ktx、activity、lifecycle） | 界面与基础能力 | Apache-2.0 |
| Material 3 + Material Icons Extended | 组件与图标 | Apache-2.0 |
| Kotlin 标准库 / Gradle / Android Gradle Plugin | 构建 | Apache-2.0 |

除此之外无其他运行时依赖。


## 安装

1. 下载 `Glyphsmith-1.1.apk`（见 Releases），传到手机点击安装
2. 系统若提示「未知来源」或「不允许安装」，在弹窗里允许本次安装即可
3. 若手机上装过**签名不同**的旧版本，需要先卸载再装

## 构建

需要 JDK 21、Android SDK 36、Gradle 9.7.1（工程未附带 Gradle Wrapper，用你自己的 Gradle 即可）。

release 签名通过工程根目录的 `keystore.properties` 读取（该文件与 `*.jks` 已 gitignore，不会随仓库分发）：

```
storeFile=keystore.jks
storePassword=你的密钥库口令
keyAlias=你的别名
keyPassword=你的密钥口令
```

没有这个文件时构建**依然成功**，只是产出未签名的 release APK。生成自己的密钥：

```powershell
keytool -genkeypair -v -keystore keystore.jks -alias mykey -keyalg RSA -keysize 2048 -validity 10000
```

构建命令与产物：

```powershell
gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

用 Android Studio 打开工程目录直接 Run 也可以（会自动配置 SDK 路径）。

## 项目结构

```
app/src/main/java/com/lanzhou/zj/
├── MainActivity.kt   界面与交互（Jetpack Compose）
├── Converter.kt      转换算法：灰度 → 字符梯度 / 彩色
├── PngExport.kt      PNG 导出：Canvas 逐格渲染
├── ArtViewModel.kt   参数与状态
└── Theme.kt          Material 3 主题与动态取色
app/src/main/res/     图标、主题、values-night（深色）、file_paths（分享用）
```

## 更新日志

**1.1**

- 新增 PNG 导出（字符画 / 色块像素画）与「分享图片」
- 修复：色块样式导出时相邻格之间出现暗色缝隙
- 关于页显示版本号

**1.0**

- 首个版本：中文 / ASCII / 彩色三种模式，支持复制、保存 TXT、保存彩色网页、系统分享
- Material Design 3 界面、Android 12+ 动态取色、页面与模式切换动画、内置示例图

## 作者

- 蓝昼 lanzhou
- 个人网站：<https://lanzhou-1.github.io>
