package dev.lumas.events.items.candy

import dev.lumas.events.EventMain
import dev.lumas.events.items.LocalCustomItemManager
import dev.lumas.events.items.SoulOrbItem
import dev.lumas.events.manager.EventPlayerManager
import dev.lumas.events.utility.Util
import dev.lumas.lumaitems.model.item.CustomItemFunctions
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.model.item.PaperDataComponent
import dev.lumas.lumaitems.util.Tier
import dev.lumas.lumaitems.util.extensions.QuickTasks
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

abstract class CandyItem : CustomItemFunctions() {

    data class CandyEffect(val chance: Double, val message: String, val action: (Player) -> Unit)

    abstract val key: String
    abstract val name: String
    abstract val lore: List<String>
    abstract val material: Material

    open val nutrition: Int = 1
    open val saturation: Float = 0.5f
    open val consumeSeconds: Float = 0.7f

    open fun effects(): List<CandyEffect> = emptyList()

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name(name)
            .lore(lore)
            .material(material)
            .tier(Tier.LUMARES_2026)
            .persistentData(key)
            .paperDataComponents(
                PaperDataComponent.valued(
                    DataComponentTypes.CONSUMABLE,
                    Consumable.consumable()
                        .consumeSeconds(consumeSeconds)
                        .animation(ItemUseAnimation.EAT)
                        .hasConsumeParticles(true)
                        .build()
                ),
                PaperDataComponent.valued(
                    DataComponentTypes.FOOD,
                    FoodProperties.food()
                        .nutrition(nutrition)
                        .saturation(saturation)
                        .canAlwaysEat(true)
                        .build()
                )
            )
            .buildPair()
    }

    override fun onConsumeItem(player: Player, event: PlayerItemConsumeEvent) {
        applyEffect(player)
    }

    // The consumable component doesn't seem to reliably work on bedrock
    override fun onRightClick(player: Player, event: PlayerInteractEvent) {
        if (!EventPlayerManager.getByUUID(player.uniqueId).isFloodgate) return
        val item = event.item ?: return
        event.isCancelled = true
        if (QuickTasks.isOnCooldown(this, player.uniqueId)) return
        QuickTasks.addCooldown(this, player.uniqueId, (consumeSeconds * 20).toLong().coerceAtLeast(1L))

        item.amount -= 1
        player.foodLevel = (player.foodLevel + nutrition).coerceAtMost(20)
        player.saturation = (player.saturation + saturation).coerceAtMost(player.foodLevel.toFloat())
        player.world.playSound(player.location, Sound.ENTITY_GENERIC_EAT, 1f, 1f)
        applyEffect(player)
    }

    private fun applyEffect(player: Player) {
        val effect = roll() ?: return
        effect.action(player)
        Util.sendMsg(player, effect.message)
    }

    private fun roll(): CandyEffect? {
        val pool = listOf(CandyEffect(SOUL_ORB_CHANCE, SOUL_ORB_MESSAGE, ::giveSoulOrb)) + effects()
        val total = pool.sumOf { it.chance }
        if (total > 1.0) {
            EventMain.getInstance().logger.warning("Candy '$key' has a total effect chance of $total (> 1.0)!")
        }

        var roll = ThreadLocalRandom.current().nextDouble()
        for (effect in pool) {
            roll -= effect.chance
            if (roll < 0) return effect
        }
        return null
    }

    private fun giveSoulOrb(player: Player) {
        val orb = LocalCustomItemManager.getCustomItemStack(SoulOrbItem::class.java) ?: return
        player.inventory.addItem(orb).values.forEach {
            player.world.dropItemNaturally(player.location, it)
        }
    }

    companion object {
        const val SOUL_ORB_CHANCE = 0.2
        const val SOUL_ORB_MESSAGE = "A faint glow lingers on your tongue... you found a <#9f2e51>Soul Orb</#9f2e51>!"
    }
}
