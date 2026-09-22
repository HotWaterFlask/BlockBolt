BlockBolt
=========

用告示牌锁定箱子、门、活板门等方块的 Minecraft 服务端插件。

BlockBolt 是 [BlockLocker](https://github.com/rutgerkok/BlockLocker)（作者 Rutger Kok）的二次开发版本，
由 **HotWaterFlask** 维护。相比原版，本插件针对中文服务器做了大量改进：默认简体中文、中文告示牌标签、
扩展的命令与权限、更细的连锁控制、支持方块标签写法、适配铜器时代新方块等。

- 适配服务端：Paper / Folia / Purpur（`folia-supported: true`）
- API 版本：1.20 及以上（铜箱、木架等新方块需要 1.21.9+ 服务端）
- 编译目标：Java 21

主要特性
--------

* 使用熟悉的 `[私有]` / `[Private]`、`[更多]` / `[More Users]` 告示牌保护方块。
* 告示牌贴在容器上时自动填写 `[私有]` 和玩家名字，无需手动输入。
* **三类保护与连锁**：独立方块（容器、铁砧、龙蛋、潜影盒、木架、栅栏门）、立门、横向依附方块（活板门），
  每类都有独立的连锁开关，可单独关闭。
* **防重力**：受保护的铁砧、龙蛋不会因悬空而掉落，龙蛋也不会传送逃走。
* **方块标签支持**：配置文件里可以直接写 `"#minecraft:wooden_doors"` 这类标签，一行等于一整类方块。
* **新方块支持**：铜箱、铜门、铜活板门、木架、合成器（Crafter）、讲台等。
* **完整中文支持**：默认载入简体中文语言文件，告示牌标签、命令提示均为中文。
* **扩展命令**：`/blockbolt <2|3|4> <文本>` 直接修改告示牌内容，支持 Tab 补全。
* **权限兼容**：权限节点改为 `blockbolt.*`，同时保留旧的 `blocklocker.*` 作为子权限。
* **自动匹配音效**：开门/开活板门时按材质播放对应音效（铜门、樱花木门、竹门、下界木门等自动识别，
  低版本服务端自动回退到通用音效）。
* **红石与漏斗防护**：受保护的容器默认禁止红石操作和漏斗自动搬运物品，除非告示牌上写了 `[红石]`。
* **保护到期**：容器主人离线超过设定天数后自动解除保护（可关闭）。
* **多插件联动**：群组系统与领地插件自动检测接入（详见下文）。

安装
----

1. 下载 `blockbolt-1.15.jar`（或参考下文自行编译）。
2. 放入服务端的 `plugins` 文件夹。
3. 重启服务器，插件会自动生成 `plugins/BlockBolt/config.yml` 和语言文件。

告示牌标签
----------

告示牌第一行是标签，其余行写玩家名。中文、英文写法都可用，大小写不敏感。

| 写法（中文 / 英文） | 作用 |
| ------------------- | ---- |
| `[私有]` / `[Private]` | 主标签，创建保护；第二行写主人名字。每个保护只能有一个。 |
| `[更多]` / `[More Users]` | 追加有权限使用的人。告示牌正面 3 行 + 背面 3 行，最多写 6 个名字。 |
| `[所有人]` / `[Everyone]` | 任何人都可以使用。 |
| `[定时:X]` / `[Timer:X]` | 门或活板门打开后自动关闭的秒数，覆盖配置里的默认值；填负数表示永不自动关。 |
| `[红石]` / `[Redstone]` | 允许红石信号操作这个保护（默认禁止）。 |
| `[傀儡]` / `[Golem]`（也支持 `[铜傀儡]`、`[铜魔像]`） | 允许铜傀儡访问受保护的容器。 |
| `[群组名]` | 群组内所有成员可用（需要群组插件联动）。 |
| `+群组名+` | 群组领袖可用（需要群组插件联动）。 |
| `名字#UUID` | 兼容 LockettePro 的写法，按 UUID 精确授权。 |

命令
----

| 命令 | 说明 | 权限 |
| ---- | ---- | ---- |
| `/blockbolt help` | 显示帮助。 | 所有人 |
| `/blockbolt <1\|2\|3\|4> <文本>` | 修改你正看着的告示牌的第 N 行。第 1 行仅管理员可改；主标签的第 2 行（主人名）仅管理员可改。 | 保护主人或 `blockbolt.admin` |
| `/blockbolt reload` | 重新加载配置文件。 | `blockbolt.reload` |

命令可用名字：`/blockbolt`、`/blocklocker`、`/deadbolt`、`/lockette`，以及简写 `/bb`、`/bl`。
Tab 补全支持玩家名、`[Everyone]`、`[More Users]`、`[Timer:3]`。

权限
----

| 权限节点 | 默认值 | 说明 |
| -------- | ------ | ---- |
| `blockbolt.protect` | 所有人 | 允许创建保护。 |
| `blockbolt.bypass` | 管理员 | 可以绕过别人的保护使用方块，但不能破坏保护。 |
| `blockbolt.admin` | 管理员 | 编辑别人的告示牌、移除保护、修改主人行。 |
| `blockbolt.reload` | 管理员 | 允许使用 `/blockbolt reload`。 |
| `blockbolt.wilderness` | 所有人 | 允许在 Towny 荒野（领地之外）创建保护。 |

旧的 `blocklocker.protect`、`blocklocker.bypass`、`blocklocker.admin`、`blocklocker.reload`、
`blocklocker.wilderness` 仍然可用，会自动映射到上面这些新权限。

保护机制
--------

### 第一类：独立方块（`protectableStandard`）

包含容器、工作方块、铁砧、龙蛋、潜影盒、木架、栅栏门等。贴上告示牌后，自动向**上下左右前后 6 个方向**
扩散连锁同类方块，大箱子、陷阱箱会当作一个整体处理。

* 竖向堆叠的容器是否合并保护，由 `groupContainersVertically` 控制。
* 铁砧、龙蛋受保护时不再受重力影响（悬空不会掉落），龙蛋也不会传送。
* 连锁由 `chainStandardBlocks` 开关控制，关掉后只保护单个方块。

### 第二类：立门（`protectableDoors`）

保护门上下两半、地基支撑方块、门上方的方块，以及周围相连的同种门。

* 双开门对称联动开闭。
* 开启 `chainDoors` 后，相邻的同类门会整组一起开闭。
* 门可以自动关闭：默认秒数由 `defaultDoorOpenSeconds` 决定，玩家可用 `[定时:X]` 标签单独覆盖。

### 第三类：横向依附方块（`protectableAttachables`）

包含活板门等附着方块。保护依附的原木/墙体方块与整组相连方块；相对面对面的活板门会对称联动翻折。

* 连锁由 `chainAttachables` 开关控制。

### 通用行为

* 只有保护的主人（或管理员）能破坏保护，管理员也只能打开、不能破坏。
* 保护告示牌被破坏时，同一保护的其他告示牌会同步清除。
* 受保护的容器默认拒绝红石信号与漏斗搬运，写了 `[红石]` 标签才放行。
* 在别人的保护旁边放置方块会被阻止；拿着告示牌右键容器时会提示如何上锁（提示有冷却，避免刷屏）。

配置文件
--------

`plugins/BlockBolt/config.yml` 为全中文注释，主要配置项：

| 配置项 | 说明 |
| ------ | ---- |
| `languageFile` | 语言文件，默认 `translations-zh.yml`。 |
| `updater` | 更新检查，`JUST_NOTIFY`（仅提示）或 `DISABLED`（默认关闭）。 |
| `chainStandardBlocks` / `chainDoors` / `chainAttachables` | 三类保护的连锁开关。 |
| `protectableStandard` / `protectableDoors` / `protectableAttachables` | 三类保护的方块列表，支持方块标签写法（见下）。 |
| `groupContainersVertically` | 容器竖向堆叠时是否合并为一个保护。 |
| `groupFurnaces`、`groupDispensers`、`groupCauldrons`、`groupEnchantmentTables`、`groupBrewingStands` | 各类容器是否与相邻同类容器连锁。 |
| `defaultDoorOpenSeconds` | 门/活板门自动关闭的默认秒数，`0` 或负数表示永不自动关。 |
| `autoExpireDays` | 主人离线多少天后保护自动失效，`0` 或负数表示永不过期（仅在线模式有效）。 |
| `allowDestroyBy` | 允许哪些方式破坏保护，可选 `CREEPER`、`TNT`、`BLOCK_EXPLOSION`、`ENDERMAN`、`FIRE`、`GHAST`、`GOLEM`、`PISTON`、`SAPLING`、`VILLAGER`、`ZOMBIE`、`UNKNOWN`。 |

### 方块标签写法

方块列表里除了写单个方块，还可以写方块标签，一行代表一整类方块：

```yaml
protectableDoors:
- "#minecraft:wooden_doors"
- minecraft:iron_door
```

* 标签必须用引号包住（YAML 里 `#` 是注释符号，不加引号会被忽略）。
* 标签在当前服务端版本不存在时，插件会在后台提示并跳过该行，不影响其他配置。
* 常用标签：`#minecraft:wooden_doors`、`#minecraft:wooden_trapdoors`、`#minecraft:fence_gates`、
  `#minecraft:shulker_boxes`、`#minecraft:wooden_shelves`、`#minecraft:copper_chests`、`#minecraft:anvil`。

第三方插件联动
--------------

* **群组系统**（检测到即自动启用）：Permissions、计分板队伍、Factions、Towny、mcMMO、Guilds、SimpleClans。
  启用后可以在告示牌上使用 `[群组名]` 和 `+群组名+`。
* **领地保护**：Towny —— 默认不允许在荒野创建保护，有 `blockbolt.wilderness` 权限的玩家可以例外。

从源码构建
----------

需要 JDK 21 和 Maven：

```bash
mvn clean package
```

生成的插件在 `target/blockbolt-1.15.jar`。

开发者 API
----------

其他插件可以通过 `top.hotwaterflask.blockbolt.BlockBoltAPI` 读取保护信息。
本插件尚未发布到中央仓库，可先 `mvn install` 到本地仓库后再引用：

```xml
<dependency>
	<groupId>top.hotwaterflask</groupId>
	<artifactId>blockbolt</artifactId>
	<version>1.15</version>
	<scope>provided</scope>
</dependency>
```

`BlockBoltAPI` 提供的方法：

```java
Optional<OfflinePlayer> getOwner(Block block)
Optional<String> getOwnerDisplayName(Block block)
boolean isAllowed(Player player, Block block, boolean allowBypass)
boolean isOwner(Player player, Block block)
boolean isProtected(Block block)
BlockBoltPlugin getPlugin()
```

示例：

```java
Optional<OfflinePlayer> owner = BlockBoltAPI.getOwner(block);
```

如果需要限制某些区域内不允许放置保护告示牌，可以监听本插件的 `PlayerProtectionCreateEvent`
（自动放置的告示牌同样会触发），或使用 Bukkit 的 `BlockPlaceEvent`。

检查某个保护是否允许红石操作：

```java
private boolean isRedstoneAllowed(Block block) {
    BlockBoltPlugin plugin = BlockBoltAPI.getPlugin();
    Optional<Protection> protection = plugin.getProtectionFinder().findProtection(block);
    if (!protection.isPresent()) {
        // 没有被保护，允许红石操作
        return true;
    }
    Profile redstoneProfile = plugin.getProfileFactory().fromRedstone();
    // 告示牌上写了 [红石] 或 [所有人] 时返回 true
    return protection.get().isAllowed(redstoneProfile);
}
```

与原版 BlockLocker 的区别
------------------------

* **语言**：默认简体中文；告示牌标签支持中文写法（`[私有]`、`[更多]`、`[定时:X]` 等）。
* **命令**：原版只有 `/blocklocker reload`，本插件提供 4 组命令入口 + 别名、行编辑命令、help 与 Tab 补全。
* **权限**：节点从 `blocklocker.*` 改为 `blockbolt.*`，旧节点保留并自动映射。
* **配置结构**：
  * `protectableContainers` 改名为 `protectableStandard`，并把铁砧、潜影盒、木架、栅栏门等整理进该列表；
  * 原版单一的 `connectContainers` 拆分为 `chainStandardBlocks`、`chainDoors`、`chainAttachables` 三个独立开关；
  * 新增 `groupContainersVertically` 等 6 个容器分组选项；
  * 方块列表支持 `#minecraft:标签` 写法；
  * 更新检查默认关闭。
* **方块**：新增铜箱、铜门、铜活板门、木架、合成器、龙蛋防重力等支持。
* **保护逻辑**：门支持整组连锁（原版仅单门/双开门）；活板门支持成对联动；容器改为递归扩散连锁并可控制竖向堆叠。
* **API**：原版 `BlockLockerAPI`（已弃用）与 `BlockLockerAPIv2` 合并为单一的 `BlockBoltAPI`，返回 `java.util.Optional`。
* **音效**：原版为少量材质硬编码音效，本插件按材质自动匹配（铜、樱花、竹、下界木等），低版本自动回退。

鸣谢与许可
----------

* 上游项目：[BlockLocker](https://github.com/rutgerkok/BlockLocker)，作者 Rutger Kok。
* 本项目采用 [MIT 许可协议](LICENCE.txt)。