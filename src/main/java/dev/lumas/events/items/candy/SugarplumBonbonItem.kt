package dev.lumas.events.items.candy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class SugarplumBonbonItem : CandyItem() {

    override val key = "sugarplum-bonbon"
    override val name = "<b><gradient:#7B3FA0:#C38BE8:#5A2A7E>Sugarplum Bonbon</gradient></b>"
    override val lore = listOf(
        "A bonbon twisted up in shiny",
        "violet foil. It crinkles",
        "loudly no matter how",
        "carefully you unwrap it.",
    )
    override val material = Material.SMALL_AMETHYST_BUD

    override fun effects() = listOf(
        CandyEffect(0.25, "The foil's shimmer rubs off on you!") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 20 * 25, 0))
        },
        CandyEffect(0.15, "The foil's shimmer dazzles you!") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 15, 0))
        },
    )
}
