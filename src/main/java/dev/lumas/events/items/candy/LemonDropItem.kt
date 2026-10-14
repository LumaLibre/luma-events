package dev.lumas.events.items.candy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class LemonDropItem : CandyItem() {

    override val key = "lemon-drop"
    override val name = "<b><gradient:#F7E46B:#FFF6A8:#C9D43F>Lemon Drop</gradient></b>"
    override val lore = listOf(
        "A glassy little sugar shard,",
        "dusted and impossibly sour.",
        "Your whole face puckers",
        "just looking at it.",
    )
    override val material = Material.PRISMARINE_CRYSTALS

    override fun effects() = listOf(
        CandyEffect(0.15, "Way too sour! Your vision swims...") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.NAUSEA, 20 * 8, 0))
        },
        CandyEffect(0.2, "The zest wakes you right up!") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 20 * 30, 1))
        },
    )
}
