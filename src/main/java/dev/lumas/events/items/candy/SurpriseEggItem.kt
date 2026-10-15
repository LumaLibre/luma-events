package dev.lumas.events.items.candy

import dev.lumas.events.EventMain
import dev.lumas.events.items.LocalCustomItemManager
import dev.lumas.events.utility.Util
import net.kyori.adventure.title.Title
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Bat
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Turtle
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

class SurpriseEggItem : CandyItem() {

    override val key = "surprise-egg"
    override val name = "<b><gradient:#9ED9A0:#F7F3D9:#F7B6D9:#8FD3F4>Surprise Egg</gradient></b>"
    override val lore = listOf(
        "A speckled chocolate egg that",
        "rattles when you shake it.",
        "Nobody knows what's inside,",
        "not even whoever made it.",
    )
    override val material = Material.TURTLE_EGG

    override fun effects() = listOf(
        CandyEffect(0.07, "Something wriggles out of the shell... it hatched!") { player: Player ->
            player.playSound(player.location, Sound.ENTITY_TURTLE_EGG_HATCH, 1f, 1f)
            val turtle = player.world.spawn(player.location, Turtle::class.java) {
                it.setBaby()
                it.ageLock = true
                it.isInvulnerable = true
                it.isPersistent = false
            }
            removeLater(turtle, 20 * 10L)
        },
        CandyEffect(0.06, "A flock of bats bursts out of the egg!") { player: Player ->
            player.playSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1f, 1f)
            repeat(6) {
                val bat = player.world.spawn(player.eyeLocation, Bat::class.java) {
                    it.isAwake = true
                    it.isInvulnerable = true
                    it.isPersistent = false
                }
                removeLater(bat, 20 * 5L)
            }
        },
        CandyEffect(0.08, "Confetti! The egg was full of it.") { player: Player ->
            val random = ThreadLocalRandom.current()
            repeat(5) {
                val color = Color.fromRGB(random.nextInt(0xFFFFFF))
                player.world.spawnParticle(
                    Particle.DUST, player.eyeLocation, 15, 0.6, 0.6, 0.6, Particle.DustOptions(color, 1.2f)
                )
            }
            player.world.spawnParticle(Particle.TOTEM_OF_UNDYING, player.eyeLocation, 40, 0.4, 0.4, 0.4, 0.3)
            player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1.2f)
        },
        CandyEffect(0.05, "Something was staring back at you from inside...") { player: Player ->
            player.spawnParticle(Particle.ELDER_GUARDIAN, player.location, 1)
            player.playSound(player.location, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1f)
            player.showTitle(
                Title.title(
                    Util.color("<b><#9f2e51>BOO!</#9f2e51></b>"),
                    Util.color("<gray>Happy Halloween</gray>"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
                )
            )
        },
        CandyEffect(0.06, "The egg was spring-loaded! Up you go...") { player: Player ->
            player.velocity = player.velocity.add(Vector(0.0, 1.2, 0.0))
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 8, 0))
            player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f)
        },
        CandyEffect(0.06, "The filling was ice cold! You start shivering...") { player: Player ->
            player.freezeTicks = 120
            player.world.spawnParticle(Particle.SNOWFLAKE, player.eyeLocation, 30, 0.4, 0.4, 0.4, 0.02)
            player.playSound(player.location, Sound.BLOCK_POWDER_SNOW_STEP, 1f, 0.8f)
        },
        CandyEffect(0.06, "A tiny music box inside plays a tune!") { player: Player ->
            // semitones above F#3, played one after another
            listOf(6, 10, 13, 18, 13, 18).forEachIndexed { i, note ->
                player.scheduler.runDelayed(EventMain.getInstance(), {
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2.0.pow((note - 12) / 12.0).toFloat())
                    player.world.spawnParticle(Particle.NOTE, player.eyeLocation.add(0.0, 0.6, 0.0), 1)
                }, null, 1L + i * 4L)
            }
        },
        CandyEffect(0.07, "There was a glowing marble inside! You feel wiser.") { player: Player ->
            player.giveExp(ThreadLocalRandom.current().nextInt(5, 16))
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        },
        CandyEffect(0.06, "Another candy was hidden inside the egg!") { player: Player ->
            val candy = LocalCustomItemManager.getCustomItems()
                .filter { it is CandyItem && it !is SurpriseEggItem }
                .randomOrNull() ?: return@CandyEffect
            player.inventory.addItem(candy.createItem().second).values.forEach {
                player.world.dropItemNaturally(player.location, it)
            }
        },
        CandyEffect(0.06, "Surprisingly filling! You feel refreshed.") { player: Player ->
            player.health = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: player.health
            player.foodLevel = 20
            player.saturation = 20f
            player.world.spawnParticle(Particle.HEART, player.eyeLocation.add(0.0, 0.5, 0.0), 5, 0.4, 0.2, 0.4)
            player.playSound(player.location, Sound.ENTITY_PLAYER_BURP, 1f, 1f)
        },
    )

    private fun removeLater(entity: Entity, ticks: Long) {
        entity.scheduler.runDelayed(EventMain.getInstance(), { entity.remove() }, null, ticks)
    }
}
