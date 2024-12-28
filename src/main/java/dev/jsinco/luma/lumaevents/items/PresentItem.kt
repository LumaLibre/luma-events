package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaevents.utility.MonoUpperFont
import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.LumaItems
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.items.nests.ArchiveOfAstralisItemNest
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class PresentItem: CustomItemFunctions() {

    private val builder = LumaItemsAPI.getInstance()
        .factory()
        .name("<b><gold>Wrapped Present")
        .material(Material.PLAYER_HEAD)
        .b64PHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTYyMzRhZTdkNTU5MDNlYThiYzM0NDEzY2Q1MmRlZDNiMzdjOTJlZWU1YWU1MzNmYzUxMjZhNjU0NjFmMTFmIn19fQ==")
        .persistentData("present")
        .tier(Tier.WINTER_2024)

    override fun createItem(): Pair<String, ItemStack> {
        return builder
            .lore(
                "A pretty little wrapped",
                "present for someone special.",
                "",
                "<dark_gray>ꜰʀᴏᴍ: ʟᴜᴍᴀ",
                "<dark_gray>ᴛᴏ: ꜱᴏᴍᴇᴏɴᴇ ꜱᴘᴇᴄɪᴀʟ")
            .buildPair()
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
        item.amount -= 1
        player.playSound(player.location, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f)

        val customItem = LumaItemsAPI.getInstance().getCustomItem(ArchiveOfAstralisItemNest.JobType.entries.random().key) ?: return
        val itemStack = customItem.createItem().second
        Util.giveItem(player, itemStack)
    }

}
