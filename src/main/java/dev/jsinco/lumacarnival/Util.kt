package dev.jsinco.lumacarnival

import dev.jsinco.lumacarnival.obj.Cuboid
import org.bukkit.Bukkit
import org.bukkit.Location

object Util {

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