package dev.jsinco.luma.lumaevents.commands.modules

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister
import dev.jsinco.luma.lumacore.manager.modules.RegisterType
import dev.jsinco.luma.lumaevents.EventMain
import dev.jsinco.luma.lumaevents.challenges.ChallengeType
import dev.jsinco.luma.lumaevents.commands.CommandManager
import dev.jsinco.luma.lumaevents.commands.CommandModule
import dev.jsinco.luma.lumaevents.obj.EventPlayerManager
import dev.jsinco.luma.lumaevents.utility.Util
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.TimeUnit
import dev.jsinco.luma.lumaitems.util.Util as LumaItemsUtil

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
    parent = CommandManager::class,
    permission = "lumaevent.admin",
    name = "bringItemsChallenge",
    usage = "/<command> bringItemsChallenge <player!>"
)
class BringItemsChallenge : CommandModule {
    // fuck this

    override fun execute(eventMain: EventMain, sender: CommandSender, s: String, strings: Array<String>): Boolean {
        val player = Bukkit.getPlayerExact(strings[0]) ?: return false
        val eventPlayer = EventPlayerManager.getByUUID(player.uniqueId)

        val challenge = eventPlayer.getChallenge(ChallengeType.BRING_ITEMS, true)
        if (challenge.isCompleted) {
            Util.sendMsg(player, "You have already completed this challenge!")
            return true
        }

        val stageNarrative = StageNarrative.fromStageNumber(challenge.currentStage)

        if (stageNarrative.tryTakeItemStack(player)) {
            challenge.currentStage = (challenge.currentStage + 1).coerceAtMost(challenge.stages)
            if (!challenge.isCompleted){
                StageNarrative.fromStageNumber(challenge.currentStage).playDialogue(player)
            }
        }
        return true
    }

    override fun tabComplete(eventMain: EventMain, commandSender: CommandSender, strings: Array<String>): List<String>? {
        return null
    }


    enum class StageNarrative(val dialogue: List<String>, val simpleDialogue: String, val itemStacks: Set<ItemStack>) {
        STAGE_0(
            listOf(
                "Hi, I'm <aqua><b>Frosty</b></aqua>! It's a pleasure to meet you.",
                "My dream this winter is to build the biggest snowman possible!",
                "Though, I can't exactly do it on my own. I'll probably need help from someone",
                "...",
                "<dark_gray>(I wonder if someone would offer to help me...)",
                "...",
                "Oh! You'll help me? Wonderful!",
                "To get started, I'll need lots of food to keep me going while I'm building.",
                "I'll need...",
                "• <gold>128x Cooked Beef</gold>",
                "• <gold>128x Cooked Porkchop</gold>",
                "• <gold>72x Cooked Mutton</gold>",
                "• <gold>72x Cooked Chicken</gold>",
                "Annnnddd...!",
                "• <gold>384x Cooked Cod</gold>",
            ),
            "Bring me X item!",
            setOf(ItemStack(Material.COOKED_BEEF, 128), ItemStack(Material.COOKED_PORKCHOP, 128), ItemStack(Material.COOKED_MUTTON, 72), ItemStack(Material.COOKED_CHICKEN, 72), ItemStack(Material.COOKED_COD, 384))),
        STAGE_1(
            listOf(
                "Next, I'll need tons of snow for my project.",
                "I'm not a huge fan of that artificial stuff so I'll need to make myself some fresh <aqua>ice</aqua>.",
                "I'm going to need <gold>36</gold> water buckets. And make sure it's the fresh kind please!"),
            "Bring me <gold>36</gold> water buckets!",
            setOf(ItemStack(Material.WATER_BUCKET, 36))),
        STAGE_2(
            listOf(
                "Next, I'll need lots of <blue>blue</blue> and <red>red</red> flowers so that my snowman can be pretty!",
                "Bring me about... <gold>24</gold> of all the <blue>blue</blue> and <red>red</red> flowers you can find!",
                "<dark_gray>(Cornflowers, Blue Orchids, Poppies, Red Tulips, and Rose Bushes)</dark_gray>"),
            "Bring me <blue>Cornflowers</blue>, <blue>Blue Orchids</blue>, <red>Poppies</red>, <red>Red Tulips</red>, and <red>Rose Bushes</red>",
            setOf(ItemStack(Material.CORNFLOWER, 24), ItemStack(Material.BLUE_ORCHID, 24), ItemStack(Material.POPPY, 24), ItemStack(Material.RED_TULIP, 24), ItemStack(Material.ROSE_BUSH, 24))),
        STAGE_3(
            listOf(
                "I'm also going to need some expensive gemstones...",
                "Why?",
                "So my snowman can be um... <gray>fabulous..?</gray>",
                "I'll need...",
                "• <green>48 Emeralds</green>",
                "• <aqua>48 Diamond</aqua>",
                "• <gold>38 Gold Ingots</gold>",
                "• <#e5c2ab>38 Iron Ingots</#e5c2ab>",
                "• <blue>24 Lapis Lazuli</blue>",
                "• <red>12 Redstone</red>",
                "• <dark_gray>12 Coal</dark_gray>",
            ),
            "Bring me: \n• 48 Emeralds\n• 48 Diamond\n• 38 Gold Ingots\n• 38 Iron Ingots\n• 24 Lapis Lazuli\n• 12 Redstone\n• 12 Coal",
            setOf(ItemStack(Material.EMERALD, 48), ItemStack(Material.DIAMOND, 48), ItemStack(Material.GOLD_INGOT, 38), ItemStack(Material.IRON_INGOT, 38), ItemStack(Material.LAPIS_LAZULI, 24), ItemStack(Material.REDSTONE, 12), ItemStack(Material.COAL, 12))),
        STAGE_4(
            listOf( // TODO: Finish me
                "Dialogue"),
            "Bring me X item!",
            setOf(ItemStack(Material.BARRIER, 128))),;


        private val storedInteraction: MutableList<UUID> = mutableListOf()

        fun playDialogue(player: Player) {
            if (!storedInteraction.contains(player.uniqueId)) {
                val spacing = 3
                dialogue.forEachIndexed { index, s ->
                    Bukkit.getAsyncScheduler().runDelayed(EventMain.getInstance(), {
                        player.sendMessage(Util.color(s).colorIfAbsent(TextColor.fromHexString("#C0D6F0")))
                    }, spacing * index.toLong(), TimeUnit.SECONDS)
                }
                storedInteraction.add(player.uniqueId)
            }
        }

        fun tryTakeItemStack(player: Player): Boolean {
            if (!storedInteraction.contains(player.uniqueId)) {
                playDialogue(player)
                return false
            } // check if inventory contains all itemStacks
            for (itemStack in itemStacks) {
                if (!player.inventory.containsAtLeast(itemStack, itemStack.amount)) {
                    Util.sendMsg(player, "<reset>You don't have enough</reset> <gold>${LumaItemsUtil.formatMaterialName(itemStack.type.name)}</gold> <reset>for</reset> <aqua><b>Frosty</b></aqua>'s <reset>request.")
                    return false
                }
            }

            for (itemStack in itemStacks) {
                player.inventory.removeItem(itemStack)
            }
            storedInteraction.remove(player.uniqueId)
            player.sendMessage(Util.color("<gold><b>Thank you!</b></gold> <dark_gray>(Completed stage ${ordinal + 1}/${entries.size})</dark_gray>"))
            return true
        }

        companion object {
            fun fromStageNumber(stage: Int): StageNarrative {
                return entries.find { it.ordinal == stage } ?: error("Invalid stage number")
            }
        }
    }
}
