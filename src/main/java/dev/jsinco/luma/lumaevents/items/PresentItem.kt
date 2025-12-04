package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumacore.utility.Text
import dev.jsinco.luma.lumaevents.archives.ArchivePercentChance
import dev.jsinco.luma.lumaevents.utility.MonoUpperFont
import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.items.misc.jobs.ArchiveOfAstralisItemNest
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class PresentItem: CustomItemFunctions() {

    companion object {
        const val BASE_64_TEXTURE: String = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjliNTY0YTZmNzMyODMxMTJhNzBiOWNlN2UxNTc1M2ViODZiZDEyZTc2NTllYzRkMGRjMDg1NWM2YmVhNzZlIn19fQ=="
    }

    private val builder = LumaItemsAPI.getInstance()
        .factory()
        .name("<b><gold>Wrapped Present")
        .material(Material.PLAYER_HEAD)
        .b64PHead(BASE_64_TEXTURE)
        .persistentData("present")
        .tier(Tier.CHRISTMAS_2025)

    override fun createItem(): Pair<String, ItemStack> {
        val item = builder
            .lore(
                "<gray>A pretty little present",
                "<gray>for someone special.",
                "",
                "<dark_gray>ꜰʀᴏᴍ: ʟᴜᴍᴀ",
                "<dark_gray>ᴛᴏ: ꜱᴏᴍᴇᴏɴᴇ ꜱᴘᴇᴄɪᴀʟ")
            .build().createItem()
        return Pair("present", item)
    }

    fun getItemFormatted(sender: String, receiver: String): ItemStack {
        return builder
            .lore(
                "A pretty little wrapped",
                "present for someone special.",
                "",
                "<dark_gray>ꜰʀᴏᴍ: ${MonoUpperFont.toMonoupperText(sender)}",
                "<dark_gray>ᴛᴏ: ${MonoUpperFont.toMonoupperText(receiver)}")
            .build().createItem()
    }

    override fun onRightClick(player: Player, event: PlayerInteractEvent) {
        event.isCancelled = true
        val item = event.item ?: return


        val customItem: ArchiveOfAstralisItemNest = LumaItemsAPI.getInstance().getCustomItem(ArchiveOfAstralisItemNest.JobType.entries.random().key) as? ArchiveOfAstralisItemNest ?: return
        val percent = ArchivePercentChance.randomByTotalWeight().actualPercent
        val itemStack: ItemStack = customItem.createItem(percent).second

        val perm = "lumaevents.default"
        val message = Text.mm(Util.PREFIX)
            .append(Text.mm("<gold>${player.name}</gold> unwrapped a present and received an "))
            .append(itemStack.displayName().hoverEvent(itemStack.asHoverEvent()))
            .append(Component.text("!"))

        for (player in Bukkit.getOnlinePlayers().filter { it.hasPermission(perm) }) {
            player.playSound(player.location, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f)
        }
        Bukkit.broadcast(message, perm)

        item.amount -= 1
        Util.giveItem(player, itemStack)
    }

}