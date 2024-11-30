package dev.jsinco.luma.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import org.bukkit.Material

class Config : OkaeriConfig() {

    @JvmField
    @Comment("Materials mapped to the amount of points they give when mined.")
    var blockValues: Map<Material, Int> = mapOf(
        Material.DEEPSLATE to 1,
        Material.BLACKSTONE to 3,
        Material.AMETHYST_BLOCK to 8,

        Material.DEEPSLATE_COAL_ORE to 2,
        Material.DEEPSLATE_IRON_ORE to 3,
        Material.DEEPSLATE_LAPIS_ORE to 4,
        Material.DEEPSLATE_REDSTONE_ORE to 5,
        Material.DEEPSLATE_GOLD_ORE to 6,
        Material.NETHER_QUARTZ_ORE to 7,
        Material.DEEPSLATE_DIAMOND_ORE to 8,
        Material.DEEPSLATE_EMERALD_ORE to 9,
        Material.ANCIENT_DEBRIS to 10,

        Material.COAL_BLOCK to 9 * 2,
        Material.IRON_BLOCK to 9 * 3,
        Material.LAPIS_BLOCK to 9 * 4,
        Material.REDSTONE_BLOCK to 9 * 5,
        Material.GOLD_BLOCK to 9 * 6,
        Material.DIAMOND_BLOCK to 9 * 7,
        Material.EMERALD_BLOCK to 9 * 8,
        Material.NETHERITE_BLOCK to 9 * 10,

        Material.RAW_IRON_BLOCK to 6 * 3,
        Material.RAW_COPPER_BLOCK to 6 * 4,
        Material.RAW_GOLD_BLOCK to 6 * 5,
    )

    @JvmField
    @Comment("The mine name and the cost to upgrade from it.")
    var mines: Map<String, Int> = mapOf(
        "A" to 2500,
        "B" to 5000,
        "C" to 10000,
        "D" to 15000,
        "E" to 25000,
        "F" to 75000,
        "G" to 100000,
        "H" to 150000,
        "I" to 250000,
        "J" to 500000,
        "K" to 700000,
        "L" to 800000
    )

    @JvmField
    @Comment("The prefix for all messages.")
    var prefix: String = "<b><#536C40>E<#D4AE6D>v<#CF8741>e<#B85C40>n<#996236>t</b> <dark_gray>»<white> "
}
