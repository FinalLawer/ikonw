# iknow —— MC 1.21.1 / NeoForge 21.1.248 模组项目

基于官方 MDK（NeoGradle 版，`net.neoforged.gradle.userdev` 7.1.38）搭建，
已配置国内镜像加速下载。完整说明见 `ModDev/README.md`。

## 环境要求

- JDK 21（已在 `gradle.properties` 中指向本机 `C:\Program Files\Java\jdk-21`）
- 无需预装 Gradle（使用项目内 `gradlew` 包装器，发行版从腾讯镜像下载）

## 常用命令（在本目录下执行）

| 命令 | 作用 |
|---|---|
| `gradlew build` | 构建模组，产物在 `build/libs/iknow-1.0.0.jar` |
| `gradlew runClient` | 启动开发环境客户端 |
| `gradlew runServer` | 启动开发环境服务端（需先在 `run/server/eula.txt` 中同意 EULA） |
| `gradlew runData` | 运行数据生成器，输出到 `src/generated/resources` |
| `gradlew clean` | 清理构建产物 |

## 修改模组信息

编辑 `gradle.properties` 中的 `mod_id`、`mod_name`、`mod_version` 等，
并同步修改：

1. 主类 `src/main/java/com/example/iknow/IknowMod.java` 中的 `MODID` 常量
2. 包名（`com.example.iknow` → 你的 `mod_group_id`）
3. 语言文件目录名（`assets/iknow` → `assets/<新modid>`）

## 首次构建提示

首次 `gradlew build` 会下载 NeoForge 与 Minecraft 并进行反编译，
耗时较长属正常现象。NeoForge/Minecraft 部分来自国外服务器，
如遇卡顿可稍后重试或使用代理。
