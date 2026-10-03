# RBMK Terminals

Wall mounted display and input terminals for Minecraft 1.7.10, modelled after the RBMK terminal of
[HBM's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT).

两块仿 HBM 的 RBMK 终端做出来的壁挂式终端方块：一块只能显示，一块既能显示也能输入，
自带一套打字命令和一套无线红石（ROR）收发，装了 HBM 时还能直接用 HBM 的螺丝刀配置。

| 方块 | 注册名 | 说明 |
| --- | --- | --- |
| 显示终端 | `rbmkterminals:display_terminal` | **只能显示**：没有界面、没有键盘，内容只能由外部推送 |
| 输入终端 | `rbmkterminals:input_terminal` | **兼顾输入与显示**：右键打开键盘，输入的命令会执行并回显到屏幕上 |

两者都是贴在墙上的薄板，屏幕用 TESR 画在朝外的那一面（最多 18 行、每行 10 像素的等宽文字）。
输入终端还能像红石方块一样输出 0–15 的强度，等于一个用打字控制的红石电源。

## 和 HBM 的关系

HBM's Nuclear Tech Mod 是**可选**依赖，`dependencies.gradle` 里是 `compileOnly`：

* 装了 HBM 时，两块终端会出现在 HBM 的机器标签页（`itemGroup.tabMachine`，即 `MainRegistry.machineTab`）里，
  和它们模仿的 RBMK 控制台放在一起；
* 装了 HBM 时，用**螺丝刀**右键任一终端可以打开配置界面，调频道、文字颜色、是否监听；
* 装了 HBM 时，终端会接进 HBM 自己的 ROR 总线，所以 `tile.radio_autocal`（AUTOCAL）发出来的东西能直接显示在
  终端上，终端打的 `send` 也能被 HBM 的设备听到；
* 没装 HBM 时，终端退回成普通版本（没有螺丝刀配置、不接 HBM 总线），位于原版的红石标签页，
  mod 照常启动，不会报 `NoClassDefFoundError`。

## 上手

进游戏后（创造模式最快）：

```
/give @p rbmkterminals:display_terminal
/give @p rbmkterminals:input_terminal
```

把两块终端贴在墙上，右键输入终端敲 `help`，或者对着显示终端执行 `/terminal write hello world`。

想试无线红石：对着显示终端执行 `/terminal chan control`，再去输入终端里敲 `chan control`、
`send write!hello radio`，显示终端上就会出现 `> hello radio`。

输入终端认识的命令：

| 命令 | 作用 |
| --- | --- |
| `help` / `?` | 列出所有命令 |
| `echo <文本>` / `write <文本>` | 把文本打到屏幕上 |
| `set <行号> <文本>` | 覆写第 1 – 18 行中的某一行 |
| `clear` / `cls` | 清屏 |
| `rs <0-15>` | 设置该终端的模拟红石输出强度 |
| `chan <频道名>` | 调到 ROR 频道（不带频道名表示离开频道） |
| `send <命令>` / `start <命令>` / `stop` | 在频道上发一条命令 / 每 tick 重复发 / 停止重复 |
| `time` / `pos` / `dim` / `players` | 世界时间、坐标、维度、该维度玩家数 |
| `horse` | 向 HBM 致敬 |
| `selfdestruct` | 自毁（默认在配置里关掉） |

显示终端没有键盘，写入只能从外部来，用 `/terminal` 命令（别名 `term`、`dterm`），
它作用于准星指着的、8 格以内的那台终端：`write` / `set` / `clear` / `chan` / `read` / `functions`。

## 文档

完整的技术文档在 [docs/terminals.md](docs/terminals.md)：放置与交互、螺丝刀配置、命令语言、
ROR 频道与 HBM 总线桥接、网络同步、源文件结构都在那里。

## 构建

需要 JDK 17+（构建脚本会自己处理 1.7.10 的工具链）：

```
./gradlew build          # 产物在 build/libs/
./gradlew runClient      # 起一个开发客户端
./gradlew test           # 命令语言与 ROR 桥接的单元测试
```

螺丝刀那部分要编译就得有 HBM 的 jar：把 `HBM-NTM-<版本>.jar` 放到 `libs/`（`dependencies.gradle`
里的 `compileOnly(rfg.deobf(files("libs/HBM-NTM-1.0.27_X5778_H261.jar")))`），
或者换成 HBM maven 上的 `com.hbm:HBM-NTM:<版本>:dev`。想在 `runClient` 里试 HBM 相关的功能，
把 HBM 和它需要的 CodeChicken 那几个 mod 丢进 `run/client/mods`。

## 许可

MIT，见 [LICENSE](LICENSE)。

## 致谢

* 构建脚本来自 [GTNewHorizons/ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10)（MIT，
  Copyright (c) 2021 Johann Bernhardt）；
* 灵感与互操作对象是 [HBM's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT)。
