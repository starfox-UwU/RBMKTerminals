# 终端方块：仿 HBM 的 RBMK 终端

本 mod 的中心是两个方块，模仿 HBM's Nuclear Tech Mod 里 `tile.rbmk_terminal`
（`RBMKTerminal` + `TileEntityRBMKTerminal` + `RenderRBMKTerminal` + `GUIScreenRBMKTerminal`）的做法：

| 方块 | 注册名 | 语言文件键 | 说明 |
| --- | --- | --- | --- |
| 显示终端 | `rbmkterminals:display_terminal` | `tile.display_terminal.name` | **只能显示**：没有界面、没有键盘，内容只能由外部推送 |
| 输入终端 | `rbmkterminals:input_terminal` | `tile.input_terminal.name` | **兼顾输入与显示**：右键打开键盘，输入的命令会执行并回显到屏幕上 |

两者都是挂在墙上的薄板（厚 2/16，面板 14/16 × 14/16），屏幕用 TESR 画在面板朝外的那一面，
最多显示 18 行、每行 10 像素的等宽文字，配色沿用 HBM 那种绿字黑底终端的感觉
（显示终端是琥珀色、输入终端是绿色，`rs` 输出大于 0 时变黄色，正在重复发信号时变橙色）。

两者还都能挂到 HBM 那套 **ROR（Redstone-over-Radio）频道**上：输入终端能用
`chan` / `send` / `start` / `stop` 在频道上发命令，两台终端都能接收 `FUN:write`、
`FUN:set<行>`、`FUN:clear`、`FUN:submit` 这些远程功能；装了 HBM 时它们还接进 HBM 自己的那条总线，
所以 `tile.radio_autocal` 之类设备发出来的信号能直接显示在终端上。细节见下面的
[ROR 频道](#ror-频道无线红石)一节。

## 放置与交互

* 只能贴着墙放置：对着墙的面右键即可，穿墙、天花板、地板放不上去（`ItemBlockTerminal#onItemUse`）。
* 背后的墙被拆掉时，终端会掉下来（`BlockTerminalBase#onNeighborBlockChange`）。
* 创造模式里两块终端都在 HBM 的机器标签页（`itemGroup.tabMachine`）里，见下面的
  [创造模式标签页](#创造模式标签页)。
* 显示终端右键没有任何反应，这是刻意的；输入终端右键打开输入界面。
* 装了 HBM 时，用**螺丝刀**右键任一终端会打开配置界面，见下面的
  [螺丝刀配置](#螺丝刀配置需要-hbm)。
* 输入界面里：`回车` 提交、`↑` / `↓` 翻阅本次会话输入过的命令、`ESC` 关闭。
  关闭后屏幕第 1 行会空出来，第 2 行起才是历史记录。

## 创造模式标签页

两块终端默认是原版的红石标签页（`BlockTerminalBase` 构造里的 `CreativeTabs.tabRedstone`），
`ModBlocks#register` 之后会调 `HbmCompat#applyMachineTab` 把它们改挂到 HBM 的机器标签页上
—— 也就是 `com.hbm.main.MainRegistry.machineTab`，标签页 id 为 `tabMachine`、显示名走
`itemGroup.tabMachine`，HBM 自己的机器、结构件和 RBMK 控制台都在那一页。

* `MainRegistry` 是 HBM 的 `@Mod` 类，`machineTab` 是它的静态字段，所以取这个字段只会触发 HBM 自己的类初始化，
  我们不需要等 HBM 的 `preInit`。
* 和别的 HBM 接线一样，取字段这件事写在 `HbmTerminalBlocks#machineTab` 里：`HbmCompat` 先确认
  `Loader.isModLoaded("hbm")`，只有装了 HBM 才会加载那个类，没装时不会碰到任何 `com.hbm.*` 类型。
* 物品形态跟着方块走：1.7.10 的 `ItemBlock#getCreativeTab()` 返回的就是 `block.getCreativeTabToDisplayOn()`，
  所以不用另外去设置 `ItemBlockTerminal` 的标签页。
* 没装 HBM 时两处调用都是空操作，方块留在红石标签页，不会出现"这个物品哪儿都找不到"的情况。

## 螺丝刀配置（需要 HBM）

装了 HBM's Nuclear Tech Mod 时，两个终端会各自多出一个配置界面。这一部分是照 HBM 的
`tile.rbmk_indicator`（`RBMKIndicator` + `TileEntityRBMKIndicator` + `GUIScreenRBMKIndicator`）做的：
方块实现 `api.hbm.block.IToolable`，HBM 的螺丝刀（`com.hbm.items.tool.ItemTooling`）右键时回调
`onScrew`，HBM 那边在那里 `openGui`，这里打开 `GuiTerminalConfig`。

| 配置项 | 说明 | 对应 rbmk_indicator |
| --- | --- | --- |
| `CHANNEL` | 终端所在的 ROR 频道，留空表示离开频道 | `rtty` |
| `COLOUR` | 屏幕常态文字颜色，6 位十六进制，留空回到本类型的默认配色 | `color` |
| `LISTEN` | 关掉后终端只显示、不响应频道上收到的远程信号 | `active` / `polling` |

* `SAVE`（或回车）把三项打包成一个 NBT 交给 `PacketTerminalConfig` 发往服务端，服务端重新核对距离
  （`TileEntityTerminalBase#hasPermission`）后交给 `receiveControl` 应用，再由描述包同步给所有正在看着这个
  方块的客户端 —— 和 HBM 的 `NBTControlPacket` + `IControlReceiver` 是同一套流程，`ESC` 则直接关掉、什么都不改。
* 颜色只决定**常态**文字色：输入终端在重复广播（`REPEAT`）或红石输出大于 0 时仍旧按状态变色，原来那套
  “橙色 / 黄色”提示还在（`RenderTerminal#screenColor`）。
* 颜色字段接受带 `#` 前缀的写法，大小写都行，只认 6 位十六进制；配置界面里那排色块是常用色的快捷方式。
* `LISTEN` 只影响**接收**：自己 `start` 出去的那条重复广播照发不误，重新打开后也不会补执行漏掉的旧信号。
* 一台终端只有一份配置，显示终端和输入终端用的是同一套字段。

### 交互顺序：为什么输入终端也能用螺丝刀

1.7.10 里 `ItemInWorldManager#activateBlockOrUseItem` 的顺序是
`onItemUseFirst` → **方块的 `onBlockActivated`**（潜行时跳过）→ 物品的 `onItemUse`，也就是方块先被问。
HBM 自己的面板（`rbmk_indicator` 之类）右键时方块总是返回 `false`，所以螺丝刀的 `onItemUse` 一定能走到
`onScrew`；而输入终端的右键被键盘占着，光靠 `IToolable` 就只能在潜行时生效。

因此 `compat/hbm` 下的两个适配方块除了实现 `onScrew`，还重写了 `onBlockActivated`：手里拿着螺丝刀时先开配置
界面，否则才交回原本的右键行为。`HbmTerminalBlocks#holdsScrewdriver` 用 HBM 自己的
`IToolable.ToolType#getType` 判断手里的东西，`onScrew` 里也照 `RBMKIndicator` 的写法只接受 `SCREWDRIVER`，
拿扳手、手钻去点都不会有反应。

### 依赖是怎么接的

HBM 是**可选**依赖，`dependencies.gradle` 里是 `compileOnly`（外加给单元测试用的 `testImplementation`）：

```
compileOnly(rfg.deobf(files("libs/HBM-NTM-1.0.27_X5778_H261.jar")))
```

* `libs/` 里那份是 HBM 的正常发布 jar，方法名还是 SRG（`func_77648_a`），所以要先过 `rfg.deobf`；
  换成 HBM maven 上的 `com.hbm:HBM-NTM:<版本>:dev` 就不需要这一步。
* `compileOnly` 意味着**不会**被发布成依赖，运行时那几个类由玩家自己装的 HBM 提供，
  没装 HBM 时 mod 照常启动，只是方块退回成不带螺丝刀功能的普通版本（`HbmCompat#createDisplayTerminal`）。
* `HbmCompat` 里只出现 modid，所有提到 HBM 类型的类都放在 `HbmTerminalBlocks` 后面：只有确认 HBM 装上了才会去
  加载它们，所以缺 HBM 时不会有 `NoClassDefFoundError`。想在本仓库的 `runClient` 里试螺丝刀，把 HBM 和它需要的
  CodeChicken 那几个 mod 丢进 `run/client/mods`，或者把 `compileOnly` 换成 `devOnlyNonPublishable`。

## 命令

输入终端内置一套很小的命令语言（`TerminalCommandProcessor`）：

| 命令 | 作用 |
| --- | --- |
| `help` / `?` | 列出所有命令 |
| `echo <文本>` | 把文本打到屏幕上 |
| `write <文本>` | 追加一行到屏幕 |
| `set <行号> <文本>` | 覆写第 1 – 18 行中的某一行 |
| `clear` / `cls` | 清屏 |
| `rs <0-15>` | 设置该终端的模拟红石输出强度 |
| `chan <频道名>` | 调到 ROR 频道（不带频道名表示离开频道） |
| `send <命令>` | 在频道上发一条 RoR 命令 |
| `start <命令>` | 每 tick 在频道上重复这条命令（屏幕文字变橙色） |
| `stop` | 停止重复 |
| `time` / `pos` / `dim` / `players` | 世界时间、坐标、维度、该维度玩家数 |
| `horse` | 向 HBM 致敬 |
| `selfdestruct` | 自毁（默认在配置里关掉） |

`rs` 是唯一会真正影响世界的命令：终端会像红石方块一样向外输出 0 – 15 的强度
（`BlockInputTerminal#isProvidingWeakPower` / `isProvidingStrongPower`），
可以当成一个用打字控制的红石电源。

## ROR 频道（无线红石）

这部分是照 HBM 的 `RTTYSystem` + `api.hbm.redstoneoverradio.IRORInteractive` 复刻的，
只是把"控制器方块 + 广播总线的转发"简化成了"终端自己既发也收"。

### 频道总线

`com.starfoxuwu.rbmkterminals.ror.RTTYSystem` 和 HBM 一样是一张 `(World, 频道名) -> 最后一条信号` 的表：

* 发送方 `RTTYSystem.broadcast(world, channel, signal, x, y, z)` 写；
* 接收方每 tick `RTTYSystem.listen(world, channel)` 读，自己判断这条信号是不是新的。

一张这样的表就是一条**总线**（`IRORBus`）。终端不只听自己这一条：`RORBus` 里放着一串“终端参与的总线”，
第一项永远是本 mod 自己的 `RTTYSystem`，装了 HBM 时 `HbmCompat#installRORBus`（preInit 里调一次）会把
HBM 那条总线的适配器也加进来。发送时 `RORBus#broadcast` 往每条总线各写一份，接收时终端逐条读。

两条总线有三处不一样，桥接时各按各的规矩来：

| | 本 mod 的 `RTTYSystem` | HBM 的 `RTTYSystem` |
| --- | --- | --- |
| 时间戳 | 发送的那一 tick 就写进去 | 下一 tick 的 PRE 阶段才落表，慢一 tick |
| “还算新鲜”的窗口 | `timeStamp >= now - 1` | `timeStamp > now - 2`，和 HBM 自己的设备一样 |
| 发送者 | 记了坐标，`isFrom` 认得出自己 | 完全没有，只有最后一条信号本身 |

HBM 用 `RTTYChannel.timeStamp` 判断新旧，这里在此基础上多记了发送者坐标，
好让"自己发的信号自己不再处理"（HBM 里终端只发、控制器只收，不需要这一步）。
去重仍然沿用 HBM 的思路：内容一样就不重复执行，所以 `start` 每 tick 重复广播也不会把屏幕刷爆。

HBM 那条总线不记发送者，所以终端在它上面只能**按内容**认自己的回声：记下自己最后发出去的那条
（`ownSignal` 和发送时的 tick），内容相同、又不比发送时更新的表项就当自己人跳过。别人过一会儿发来
一模一样的文本时时间戳会往前走，那就算一条新信号——反正内容相同的两条本来也只执行一次。

另外两点和 HBM 一致的行为，用的时候注意：

* **频道是广播的**：同频道上的每一台终端都会执行收到的功能，没有"点对点寻址"。
  HBM 里是靠贴着设备的控制器方块来寻址，这里简化掉了。
* **一个频道同一 tick 只保留最后一条信号**：`RTTYChannel` 存的就是"最后一条"，
  所以同一 tick 里连发多条 `send` 只有最后一条有效（手动输入不可能快到这个程度，一般碰不到）。
  HBM 的总线还会把同一 tick 里的两条纯数字信号**相加**，那是它自己的老规矩，终端照单全收。

### 信号格式

和 HBM 完全一致：`名字!参数:参数`，`!` 是名字分隔符、`:` 是参数分隔符，
功能调用带 `FUN:` 前缀，读数带 `VAL:` 前缀（`IRORInteractive.PREFIX_*`）。
终端认识的功能（`/terminal functions` 可以列出当前看的那台）：

| 功能 | 作用 |
| --- | --- |
| `FUN:clear` | 清屏 |
| `FUN:write!文本` | 追加一行 |
| `FUN:set<行号>!文本` | 覆写某一行 |
| `FUN:submit!命令` | 让对面那台**输入终端**执行一条命令（显示终端没有键盘，会回一句拒绝） |

除此之外，**所有认不出来的信号都会原样显示到屏幕上**（`TerminalCommandProcessor#receiveSignal`）：
终端既执行 ROR 功能，也当一块公告板。这正是它能显示 `tile.radio_autocal` 消息的原因——AUTOCAL 发什么，
屏幕上就有什么。真正格式不对的信号（比如 `a!b!c` 这种带两个 `!` 的）不会显示原文，而是像其它输入错误
一样在屏幕上回一句 `Exception: ...`。

### 玩法示例

1. 在墙上贴一台显示终端，准星对着它执行 `/terminal chan control`（显示终端没有键盘，只能用命令调频）；
2. 旁边贴一台输入终端，右键打开，敲 `chan control`，再敲 `send write!hello radio`；
3. 显示终端上就出现了 `> hello radio`。

输入终端之间也可以互相驱动：A 敲 `chan control` + `submit clear`，同样在 `control` 上的 B 就会清屏。
`start rs!7` 这类重复广播适合当成"一直按住的按钮"。


### 和 HBM 的 ROR 网络互通（装了 HBM 时）

背景：HBM 的 `com.hbm.tileentity.network.RTTYSystem` 是一张**静态表**，本 mod 复刻的那张是另一张，
两者互不相干。所以最初这两个终端根本看不见 `tile.radio_autocal`（AUTOCAL 自动计算机）往 ROR 网络里
发的东西：AUTOCAL 跑 MSES1 脚本，脚本里的 `send <频道>` 语句走到
`RTTYSystem.broadcast(world, 频道, 缓冲区内容)`，落在 HBM 那张表上，而终端只在自家那张表上找。

`compat/hbm/HbmRORBus` 就是这两个网络之间的桥：

* 它在 preInit 时被 `HbmCompat#installRORBus` 挂进 `RORBus`（`Loader.isModLoaded("hbm")` 为真才会加载
  这个类，没装 HBM 时它和它的依赖一行都不会被加载），之后终端读的、写的都多出来 HBM 这一条；
* 读的时候把 HBM 的 `RTTYChannel.signal`（`Object`，可能是数字、单个字符、编码过的音符）按 HBM 自己的
  写法 `"" + signal` 变成文本；
* 没有发送者坐标这件事，用上面说的“按内容认自己的回声”补上；
* 写的时候走 `RTTYSystem.broadcast`，所以终端的 `send` / `start` 也能被 HBM 的设备（无线红石控制器、
  AUTOCAL 的 `poll` / `listen`）听到。

于是**两个终端和 AUTOCAL 在同一个频道上就能互相收发**，玩家只需要做两件事：给终端调频、让 AUTOCAL 发。

#### AUTOCAL 那边

AUTOCAL 是脚本机，语言是 MSES1-FEIS：`buffer <文本>` 写缓冲区、`eval <表达式>` 算数字存缓冲区、
`send <频道>` 把缓冲区的内容打到频道上、`poll <频道>` / `listen <频道>` 把频道上的信号读回缓冲区
（`ParseMSES1`、`ParseMSES1Ext1`）。最小的一段：

```
clockspeed 1
buffer write!Reactor outlet 1200K
send control
```

把终端调到 `control`，屏幕上就会出现 `> Reactor outlet 1200K`。第二行写成
`buffer Reactor outlet 1200K` 也一样：`write!` 只是 ROR 的功能名，认不出来的一律当文本显示。

想把别的频道转发过来，用 `poll` 当中继：

```
dest loop
poll gauge          # 把 gauge 频道上最新的一条放进缓冲区
send control        # 再原样发到 control
jmp loop
```

`poll` 只在那个频道“刚有信号”（1 tick 之内）时才更新缓冲区，`listen` 则不管新旧都读；两条语句都不会
因为频道空着而报错，所以这种中继脚本放在 `dest` / `jmp` 循环里跑就行。

#### 终端那边

* 用 `/terminal chan control` 或螺丝刀配置界面把终端调到 `control`（显示终端没有键盘，只能靠这两种方式）；
* 屏幕上每一条都来自 ROR：`write!` 追加一行、`set<行>!` 覆写某行、`clear` 清屏，其余原样显示，
  所以 HBM 仪表通过 `RadioTorchReader` 发出去的数值、AUTOCAL 拼出来的任意文本都会直接出现在屏幕上；
* 终端自己的 `send` / `start` 同样会进 HBM 的网络，AUTOCAL 用 `poll control` 就能收到。

> 注意 HBM 的信号格式：`!` 是名字分隔符、`:` 是参数分隔符，参数之间用空格拼回文本。所以从终端发出去的
> 文本里出现 `:` 会被拆成两段再用空格拼回来（`write!a:b` 显示成 `a b`），出现 `!` 则会变成
> `Exception: Multiple Name Separators`。

## 给显示终端写内容

显示终端没有输入界面，写入只能从外部来：

```
/terminal write <文本>        # 给正在看的终端推一行
/terminal set <1-18> <文本>   # 覆写某一行
/terminal clear               # 清屏
/terminal chan <频道名>        # 调频，不带名字表示离开频道
/terminal read                # 把屏幕内容与当前频道打印到聊天栏
/terminal functions           # 列出这台终端在 ROR 上认识的功能
```

命令别名 `term`、`dterm`。它作用于玩家视线前方 8 格内、准星指着的那个终端
（`CommandTerminal#findTerminal`，用 `World#rayTraceBlocks` 找）。

其他 mod 也可以直接推内容，或者挂在同一个频道上：

```java
TileEntity te = world.getTileEntity(x, y, z);
if (te instanceof TileEntityDisplayTerminal) {
    TileEntityDisplayTerminal terminal = (TileEntityDisplayTerminal) te;
    terminal.pushLine("reactor temperature: 1200 K");
    terminal.markChanged(); // 让附近客户端看到新内容
    terminal.getChannel();  // 它当前听哪个频道
}

// 或者当成一台无线设备来用，和 HBM 的写法一样
RTTYSystem.broadcast(world, "control", "write!hello radio", x, y, z);
// 装了 HBM 时想连它的总线一起喂（终端自己发信号就是这么做的），用扇出：
RORBus.broadcast(world, "control", "write!hello radio", x, y, z);
if (te instanceof IRORInteractive) {
    ((IRORInteractive) te).runRORFunction(IRORInteractive.PREFIX_FUNCTION + "write", new String[] { "hi" });
}
```

## 同步

* 屏幕内容存在 `TileEntityTerminalBase#lines`（18 行），随方块 NBT 存档。
* 客户端第一次看到区块时，服务端用 `TileEntity#getDescriptionPacket` 把内容发过去
  （1.7.10 里 `EntityPlayerMP` 在开始观察区块时就会发这个包）。
* 内容变化时 `markChanged()` 调 `World#markBlockForUpdate`，服务端下一个 tick 会在
  `PlayerManager.PlayerInstance#sendChunkUpdate` 里再次发描述包。
* 键盘输入走 `PacketTerminalCommand`（SimpleNetworkWrapper，客户端 → 服务端）。
  服务端在包里重新校验距离，然后把命令交给 `TileEntityInputTerminal#queueCommand`，
  由服务端 tick 里的 `updateEntity()` 执行 —— 1.7.10 的 FML 是在 netty 线程上回调
  `IMessageHandler#onMessage` 的，动世界的东西不能在那儿做。
* 配置（频道、颜色、监听）和屏幕内容共用 `readScreenNBT` / `writeScreenNBT`，所以描述包顺带把它们一起同步；
  老存档里没有这两个键时按“默认配色 + 监听开”读，行为不变。
* 配置界面走 `PacketTerminalConfig`（同一个 SimpleNetworkWrapper），服务端同样不信任客户端发来的坐标，
  核对距离之后才调用 `TileEntityTerminalBase#receiveControl`。

## 配置文件

`config/rbmkterminals.cfg`：

```
terminal {
    B:enableTerminalSelfDestruct=false   # 是否允许 selfdestruct 真的炸
}
```

单台终端的频道、颜色、监听开关不在这里，它们存在方块自己的 NBT 里，用螺丝刀配置（见上）。

## 贴图

`assets/rbmkterminals/textures/blocks/terminal_panel.png` 是脚本生成的，改外观可以直接改脚本再跑一次：

```
<Python> tools/make_terminal_texture.py
```

32×32，外圈 2 像素是金属边框，中间是屏幕区域（正好对应面板 14/16 面上 28/32 的范围，
文字就是画在这个范围里的）。

## 源文件

```
com/starfoxuwu/rbmkterminals/
├── RBMKTerminals.java                      mod 入口，@Mod.Instance
├── CommonProxy.java                注册方块、网络通道、GUI handler、命令、配方
├── ClientProxy.java                绑定 TESR
├── TerminalGuiHandler.java         IGuiHandler
├── ModRecipes.java                 两个合成配方
├── Config.java                     enableTerminalSelfDestruct
├── ror/                            无线红石
│   ├── RTTYSystem.java             频道总线（本 mod 自己的那张表）
│   ├── IRORBus.java                一条总线：广播、读取、新鲜度
│   ├── RORSignal.java              一次读取：信号文本、时间戳、可选的发送者
│   ├── RORBus.java                 终端参与的所有总线 + 广播扇出
│   ├── IRORInteractive.java        信号格式、功能接口、解析助手
│   └── RORFunctionException.java   参数错误
├── block/
│   ├── ModBlocks.java
│   ├── BlockTerminalBase.java      贴墙薄板：碰撞箱、朝向、支撑检查
│   ├── BlockDisplayTerminal.java
│   ├── BlockInputTerminal.java     红石输出 + 右键开界面
│   └── ItemBlockTerminal.java      只能贴在墙上，物品提示
├── tileentity/
│   ├── TileEntityTerminalBase.java 18 行屏幕、频道收发（所有总线）、配置字段、存档、描述包同步
│   ├── TileEntityDisplayTerminal.java
│   └── TileEntityInputTerminal.java 命令执行、rs 输出、自毁
├── terminal/
│   ├── ITerminalHost.java          命令处理器眼中的“终端”
│   ├── TerminalConfig.java         颜色解析/格式化、配置字段名，纯 Java，有单元测试
│   └── TerminalCommandProcessor.java  纯 Java 的命令语言 + ROR 功能分发（认不出的信号当文本显示），有单元测试
├── network/
│   ├── ModNetwork.java
│   ├── PacketTerminalCommand.java
│   └── PacketTerminalConfig.java   配置界面 → 服务端的 NBT 包（对应 HBM 的 NBTControlPacket）
├── command/CommandTerminal.java    /terminal
├── compat/hbm/                     可选：HBM 接线（螺丝刀 + ROR 总线 + 机器标签页），没装 HBM 时这些类不会被加载
│   ├── HbmCompat.java              只认 modid 的工厂：注册哪种方块、挂哪个标签页、要不要接 HBM 的总线
│   ├── HbmRORBus.java              HBM 的 RTTYSystem 适配器，接上后 AUTOCAL 的信号终端也听得到
│   ├── HbmTerminalBlocks.java      真正提到 HBM 类型的地方：IToolable 判断、螺丝刀判断、MainRegistry.machineTab
│   ├── BlockHbmDisplayTerminal.java  implements IToolable，右键开配置界面
│   └── BlockHbmInputTerminal.java    螺丝刀优先，其余交回键盘
└── client/
    ├── RenderTerminal.java         世界内屏幕文字（含可配置文字颜色）
    ├── GuiTerminal.java            输入界面（标题栏显示频道与重复状态）
    └── GuiTerminalConfig.java      螺丝刀打开的配置界面
```

## 在游戏里看效果

```
./gradlew runClient
```

进入世界后（创造模式最快）：

```
/give @p rbmkterminals:display_terminal
/give @p rbmkterminals:input_terminal
```

把两个方块贴在墙上，右键输入终端敲 `help`，或者对着显示终端执行
`/terminal write hello world`。

想试 ROR：对准显示终端 `/terminal chan control`，再去输入终端里敲
`chan control`、`send write!hello radio`，显示终端上就会出现 `> hello radio`；
`/terminal read` 能看某台终端当前的频道。

想试螺丝刀配置：把 HBM（以及它需要的 CodeChicken 那几个 mod）丢进 `run/client/mods`，拿着 HBM 的螺丝刀右键
终端即可（`runClient` 默认不会自动带上 HBM，原因见[依赖](#依赖是怎么接的)）；
配置完对着输入终端敲 `help` 还能看到它仍旧照常工作。

想试 AUTOCAL：装上 HBM 之后，摆一台 `tile.radio_autocal`，脚本里写

```
clockspeed 1
buffer Reactor outlet 1200K
send control
```

开机（AUTOCAL 界面的开关），再把显示终端 `/terminal chan control`；显示终端上就会逐条出现 AUTOCAL
发出来的消息。把两台终端都调到 `control` 时，输入终端打的 `send` 也能被 AUTOCAL 的 `poll control` 读到。

桥接本身是纯逻辑，跑不进游戏的部分由单元测试盯着：

```
./gradlew test --tests "*HbmRORBusTest*"
```

它用 `libs/` 里那份 HBM 真身往 `RTTYSystem` 里塞一条信号，再从 `HbmRORBus` 读回来，所以“AUTOCAL 发的
东西终端能不能看见”这件事不用真的开游戏就能验证。
