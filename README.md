# AE2 EMI Terminal

EMI × AE2 的附属模组，补全 Minecraft 26.3 / NeoForge 26.3.0.x 下 [EMI](https://modrinth.com/mod/emi) 对 AE2 终端的支持。纯客户端，服务端无需安装。

> **注意**：本模组由 AI 辅助编写。功能经过游戏内实测，但不保证稳定性，也不承诺持续维护。使用前请备份存档，风险自负。

> 兼容性只保证 AE2 `26.3.2-alpha`、ae2wtlib `26.3.2-beta`、EMI `2.x` 这一组合，其余版本未测试。官方 EMI 尚未发布 26.3 版本，26.3 下实际使用的是 [PF EMI](https://www.curseforge.com/minecraft/mc-mods/pf-emi) 的构建。

## 功能

EMI 认得 AE2 的物品，但不知道终端的合成格和样板编码页该怎么填。这个模组把这块补上。

### 合成终端

- 悬停配方点 `+`，把材料填进合成格。库存计算走 ME 网络，ME 和背包里都没有的材料也会参与缺料判断。
- 配方界面按实际缺额标红，沿用 AE2 原生的缺料提示文案（Ctrl 查看可替换项）。
- 支持 ME 合成终端、AE2WTLib 无线合成终端。

### 样板编码终端

- 在 EMI 的机器配方页点 `+`，把输入、输出和数量填进**处理样板**，可直接编码写入空白样板。物品和流体都支持。
- 压印器这类"常驻压模"配方不会把上下压模算进消耗材料；概率副产物（如锯木机副产物）不写入样板。
- Mekanism 化学品无法被 AE2 表示时，如果机器靠管道外部供料，按住 `Shift` 点 `+` 可只填其余材料。
- 支持普通样板编码终端和 AE2WTLib 通用终端的样板页。工作台类配方仍走原生合成样板逻辑。

### 终端内中键下单

- 打开任意 ME 终端时，对 EMI 侧栏（搜索、收藏、历史）或配方页的物品按中键，直接打开 AE2 原生的下单数量窗口。只要网络里有对应样板，库存为 0 也能下单。
- 不打开终端、直接在背包里下单，由 [AE2 EMI Stock](https://github.com/Pianzh/ae2-emi-stock) 负责。

## 安装

从 [Releases](https://github.com/Pianzh/ae2-emi-terminal/releases) 下载对应版本的 jar，放进客户端 `mods` 即可，服务端不需要安装。

## 致谢

- 样板填充的编码接口来自 AE2 自带的 `EncodingHelper`。
- 缺料提示复用 AE2 的 `TransferHelper`。
- 机器配方的槽位布局通过 EMI 的 JEMI 桥接读取。
- 交互设计参考了 [GTO 整合包](https://github.com/GregTech-Odyssey)。

## License

[MIT](LICENSE)
