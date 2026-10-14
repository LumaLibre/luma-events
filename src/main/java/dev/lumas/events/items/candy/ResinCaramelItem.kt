package dev.lumas.events.items.candy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class ResinCaramelItem : CandyItem() {

    override val key = "resin-caramel"
    override val name = "<b><gradient:#C9762B:#F2A65A:#8A4B1C>Resin Caramel</gradient></b>"
    override val lore = listOf(
        "A sticky clump of amber caramel,",
        "still warm from the creaking woods.",
        "It clings to your teeth a little",
        "longer than it should.",
    )
    override val material = Material.RESIN_CLUMP

    override fun effects() = listOf(
        CandyEffect(0.2, "Sugar rush! Your feet feel lighter.") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 25, 1))
        },
        CandyEffect(0.15, "The caramel glues your boots to the ground!") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 2))
        },
    )
}
