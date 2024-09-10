package dev.jsinco.lumacarnival

import dev.jsinco.lumacarnival.obj.Cuboid
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object Util {

    val PREFIX: Component = mm("<b><#8EC4F7>C<#C7B0E1>a<#FF9CCB>r<#EBC9AC>n<#D7F58D>i<#FFFE8A>v<#FFE978>a<#FFD365>l</b> <dark_gray>»<white> ")

    @JvmStatic
    fun msg(player: CommandSender?, msg: String) {
        if (player == null) return
        player.sendMessage(PREFIX.append(mm(msg)))
    }

    fun mm(m: String): Component {
        return MiniMessage.miniMessage().deserialize("<!i>$m")
    }

    fun mml(m: String): List<Component> {
        return listOf(mm(m))
    }

    fun mml(vararg m: String): List<Component> {
        return m.map { mm(it) }
    }

    fun mml(m: List<String>): List<Component> {
        return m.map { mm(it) }
    }


    fun giveItem(player: Player, item: ItemStack) {
        for (i in 0..35) {
            if (player.inventory.getItem(i) == null || player.inventory.getItem(i)!!.isSimilar(item)) {
                player.inventory.addItem(item)
                break
            } else if (i == 35) {
                player.world.dropItem(player.location, item)
            }
        }
    }

    fun getLocation(locationString: String): Location?  {
        val split = locationString.split(",")
        if (split.size != 4) {
            return null
        }
        val world = Bukkit.getWorld(split[0]) ?: return null

        return Location(world, split[1].toDouble(), split[2].toDouble(), split[3].toDouble())
    }

    fun getArea(areaString: String): Cuboid? {
        val split = areaString.split(",")
        if (split.size != 7) {
            return null
        }
        val world = Bukkit.getWorld(split[0]) ?: return null

        return Cuboid(
            world, split[1].toInt(), split[2].toInt(), split[3].toInt(),
            split[4].toInt(), split[5].toInt(), split[6].toInt()
        )
    }
}