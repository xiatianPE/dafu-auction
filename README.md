# 达芙拍卖估价器

Android + Kotlin + Jetpack Compose 的拍卖藏品估价 / 大金反推工具。

## 功能

- **普通估价**：金红数（紫金红总数 − 紫件数）× 倍率（×8 / ×10）+ 已选大红/紫件加成
- **七档建议出价**：保守 / 稳妥 / 按估价 / 激进 / 高竞价 / 冲刺 / 极限，浮动范围可调
- **金均反推**：输入金均读数，反推可能的金品组合（件数、总价、均价、误差、具体大金明细）
- **所见大金**：勾选确认过的大金，作为固定项参与反推
- **自定义藏品**：可加大红加成、大金价格，内置数据不可删除
- **设置持久化**：主题、倍率、浮动、误差范围、限时、条目上限、狂暴模式等自动保存
- 数据全部保存在本机（Room + DataStore），无网络依赖

## 模块结构

```
app/src/main/java/com/dafu/auction/
├── AuctionApp.kt                  # Application，启动 Koin
├── di/AppModule.kt                # 依赖注入
├── data/
│   ├── local/                     # Room 实体 / DAO / 数据库 + DataStore 设置
│   └── repository/                # 内置 + 自定义藏品合并
├── domain/
│   ├── model/                     # CatalogItem / AuctionState / InferResult ...
│   ├── repository/                # CatalogRepository 接口
│   └── usecase/                   # EvaluateUseCase（估价）、InferEngine（反推）
└── presentation/
    ├── MainActivity.kt
    ├── viewmodel/AuctionViewModel.kt
    ├── theme/                     # 紫金配色 + Material3
    ├── components/                # 通用卡片 / 输入 / 计数控件
    ├── screens/                   # 主界面 + 结果抽屉（4 个 Tab）
    └── dialogs/                   # 设置 / 公告 / 藏品管理
```

## 反推算法

1. 大金按价格分组，同价合并，把 `4^N` 的组合爆炸压缩成「每组取几件」的解空间；
2. DFS 枚举 + 上下界剪枝：
   - 当前总价 + 剩余可加最大总价 < 最低件数的下界 → 剪枝
   - 当前总价 > 最大件数对应的上界 → 剪枝
3. 命中组合按件数上限展开成具体大金明细；
4. 用大顶堆（按绝对误差）只保留最优的 N 条，零误差优先展示；
5. 带节点数 / 时间上限，超限时返回已有结果并提示。

## 构建

需要 JDK 17、Android SDK 34：

```bash
./gradlew :app:assembleDebug
```

若本机没有 Gradle Wrapper 的 `gradle-wrapper.jar`，用 Android Studio 打开工程一次即可自动生成：

```bash
gradle wrapper --gradle-version 8.2
```

## 已知限制

- 反推件数上限 90，每种大金最多 3 件（按游戏规则设定，可在 `InferEngine` 中调整）
- 内置价格表为初始数据，实际价格请在「藏品管理」中补充/修正
