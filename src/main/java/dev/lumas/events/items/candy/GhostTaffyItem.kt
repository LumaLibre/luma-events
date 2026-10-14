package dev.lumas.events.items.candy

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class GhostTaffyItem : CandyItem() {

    override val key = "ghost-taffy"
    override val name = "<b><gradient:#E8E4DC:#FFFFFF:#B9B3A8>Ghost Taffy</gradient></b>"
    override val lore = listOf(
        "Pale, chewy taffy wrapped",
        "in wax paper. It tastes",
        "faintly of mist and also",
        "something you can't place.",
    )
    override val material = Material.PALE_OAK_BUTTON

    override fun effects() = listOf(
        CandyEffect(0.2, "You feel light as a ghost.") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 20, 0))
        },
        CandyEffect(0.2, "You fade a little... spooky!") { player: Player ->
            player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 20 * 20, 0))
        },
    )
}
