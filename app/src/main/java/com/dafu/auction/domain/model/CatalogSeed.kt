package com.dafu.auction.domain.model

/**
 * 内置藏品数据
 *
 * - 大红：bonus 单位“万”
 * - 大金：price 单位“元”
 */
object CatalogSeed {

    /** 内置大红清单 */
    fun redItems(): List<CatalogItem> = listOf(
        red("red_eternal", "永恒之心", 120.0),
        red("red_suitcase", "古旧手提箱", 40.0),
        red("red_piggy", "储钱小哼", 40.0),
        red("red_smoke", "烟云盏", 40.0),
        red("red_doorcard", "未知门禁卡", 20.0),
        red("red_dragon", "白龙王", 20.0),
        red("red_eyedom", "曜目权柄", 10.0),
        red("red_cond", "酷辣辣辣条", 10.0),
        red("red_fire", "一簇幽火", 10.0),
        red("red_food9", "九格小食", 10.0),
        red("red_fold", "便携式折叠屏", 10.0),
        red("red_wreath", "永生花环", 10.0),
        red("red_statue", "乔望尼金雕像", 10.0)
    )

    /** 内置大金清单 */
    fun goldItems(): List<CatalogItem> = listOf(
        gold("gold_eternal", "永恒之心", 120_000L),
        gold("gold_suitcase", "古旧手提箱", 40_000L),
        gold("gold_piggy", "储钱小哼", 40_000L),
        gold("gold_smoke", "烟云盏", 40_000L),
        gold("gold_doorcard", "未知门禁卡", 20_000L),
        gold("gold_dragon", "白龙王", 20_000L),
        gold("gold_eyedom", "曜目权柄", 10_000L),
        gold("gold_cond", "酷辣辣辣条", 10_000L),
        gold("gold_fire", "一簇幽火", 10_000L),
        gold("gold_food9", "九格小食", 10_000L),
        gold("gold_fold", "便携式折叠屏", 10_000L),
        gold("gold_wreath", "永生花环", 10_000L),
        gold("gold_statue", "乔望尼金雕像", 10_000L),
        gold("gold_dapian", "大饼", 202_798L)
    )

    /** 默认倍率档位 */
    val multipliers: List<Int> = listOf(8, 10)

    private fun red(id: String, name: String, bonus: Double) =
        CatalogItem(id = id, name = name, category = ItemCategory.RED, bonus = bonus, builtIn = true)

    private fun gold(id: String, name: String, price: Long) =
        CatalogItem(id = id, name = name, category = ItemCategory.GOLD, price = price.toDouble(), builtIn = true)
}
