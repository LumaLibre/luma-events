package dev.jsinco.luma.lumaevents.explorer.constants

import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.dre.brewery.api.events.IngedientAddEvent
import com.dre.brewery.api.events.PlayerChatDistortEvent
import com.dre.brewery.api.events.PlayerPukeEvent
import com.dre.brewery.api.events.PlayerPushEvent
import com.dre.brewery.api.events.brew.BrewDrinkEvent
import com.gamingmesh.jobs.api.JobsPaymentEvent
import com.gamingmesh.jobs.container.CurrencyType
import com.ghostchu.quickshop.api.event.economy.ShopSuccessPurchaseEvent
import com.gmail.nossr50.api.TreeFellerBlockBreakEvent
import com.oheers.fish.api.EMFFishEvent
import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent
import com.olziedev.playerwarps.api.events.warp.PlayerWarpSponsorEvent
import com.olziedev.playerwarps.api.events.warp.PlayerWarpTeleportEvent
import com.palmergames.bukkit.towny.event.TownClaimEvent
import com.palmergames.bukkit.towny.event.TownInvitePlayerEvent
import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoTownBorderEvent
import dev.jsinco.luma.lumaevents.EventMain
import dev.jsinco.luma.lumaevents.explorer.ExplorerMile
import dev.jsinco.luma.lumaevents.explorer.custom.BlockBrokenExplorerEvent
import dev.jsinco.luma.lumaevents.explorer.custom.BlockPlacedExplorerEvent
import dev.jsinco.luma.lumaevents.explorer.custom.NabbitChangeRole
import dev.jsinco.luma.lumaevents.explorer.custom.NabbitPickupCarrot
import dev.jsinco.luma.lumaevents.explorer.custom.NabbitSurviveExtendedTimePeriod
import dev.jsinco.luma.lumaevents.games.obj.NabbitPlayer
import dev.jsinco.luma.lumaevents.utility.Util
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent
import io.papermc.paper.event.block.PlayerShearBlockEvent
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent
import io.papermc.paper.event.player.PlayerFailMoveEvent
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerNameEntityEvent
import io.papermc.paper.event.player.PlayerShieldDisableEvent
import me.SuperRonanCraft.BetterRTP.references.customEvents.RTP_TeleportEvent
import me.hexedhero.pp.api.PinataHitEvent
import me.hexedhero.pp.api.VoteReceivedEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Ageable
import org.bukkit.entity.Animals
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.event.command.UnknownCommandEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerEggThrowEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRiptideEvent
import java.util.logging.Level

object ExplorerMiles {

    // 71 miles done so far-ish...
    // <aqua></aqua> tags should surround descriptive words for miles
    // that aren't super obvious as to what they are


    val CHAT = ExplorerMile<AsyncChatEvent>(
        title = "Chatterbox",
        desc = """
            Talkative much? We get it, you love to chat.
            But don't worry, we won't judge you for it.
        """.trimIndent(),
        quantity = 100,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = AsyncChatEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val DISCUSS_EASTER_RELATED_TOPICS = ExplorerMile<AsyncChatEvent>(
        title = "Discuss Easter Related Topics",
        desc = "TODO",
        quantity = 1,
        levels = 5,
        eventClass = AsyncChatEvent::class.java,
    ) { event, levelSnapShot, data ->
        val easterTopicWords = listOf("egg", "bunny", "chocolate", "hunt", "basket", "spring", "candy", "rabbit", "easter", "event")
        val wordList: MutableList<String> = data["wordList"] as? MutableList<String> ?: mutableListOf()

        val plainText = PlainTextComponentSerializer.plainText().serialize(event.message())
        for (word in easterTopicWords) {
            if (plainText.contains(word) && !wordList.contains(word)) {
                wordList.add(word)
                levelSnapShot.currentQuantity += 1
            }
        }

        data["wordList"] = wordList
    }

    // PlayerHarvestBlockEvent?
    val FARM_CARROTS = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Farm Carrots",
        desc = "TODO",
        quantity = 350,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockBrokenExplorerEvent::class.java,
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.CARROTS) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val PLANT_CARROTS = ExplorerMile<BlockPlacedExplorerEvent>(
        title = "Plant Carrots",
        // todo: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 350,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockPlacedExplorerEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.CARROTS) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_DIAMOND_ORES = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Diamond Jeweler",
        desc = """
            You didn’t come all this way for coal. 
            Smash those sparkly suckers and get rich—or at least <i>look</i> rich!
        """.trimIndent(),
        quantity = 100,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockBrokenExplorerEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.DIAMOND_ORE) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_EMERALD_ORES = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Emerald Jeweler",
        // Todo: add a better description
        desc = """
            Trade your way to riches by breaking emerald ores! 
            Remember, villagers might offer you deals, but they sure love crushing loaf.
        """.trimIndent(),
        quantity = 50,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockBrokenExplorerEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.EMERALD_ORE) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_OBSIDIAN = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Obsidian Crusher",
        desc = """
            The hardest block in all of Minecraft! Right?
            Well, at least I <i>think</i> it is....
        """.trimIndent(),
        quantity = 100,
        levels = 4,
        levelMultiplier = 1.5,
        eventClass = BlockBrokenExplorerEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.OBSIDIAN) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_ANCIENT_DEBRIS = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Ancient Debris Crusher",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 100,
        levels = 4,
        levelMultiplier = 1.5,
        eventClass = BlockBrokenExplorerEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.ANCIENT_DEBRIS) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_BLOCKS = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Break Blocks",
        desc = """
            Break blocks? That's a little vague, don't you think?
            But hey, we'll reward you for it anyway!
        """.trimIndent(),
        quantity = 50000,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockBrokenExplorerEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val KILL_BUNNIES = ExplorerMile<EntityDeathEvent>(
        title = "Kill Bunnies",
        desc = """
            For this mile, you'll need to kill a few poor bunnies.
            And don't worry, we're only asking you to take care of the baby ones!
        """.trimIndent(),
        quantity = 20,
        levels = 3,
        levelMultiplier = 1.5,
        eventClass = EntityDeathEvent::class.java
    ) { event, levelSnapshot, _ ->
        val entity = event.entity as? Ageable ?: return@ExplorerMile
        if (entity.type != EntityType.RABBIT || entity.isAdult) {
            return@ExplorerMile
        }

        levelSnapshot.currentQuantity += 1
    }

    val KILL_CHICKENS = ExplorerMile<EntityDeathEvent>(
        title = "Kill Chickens",
        desc = """
            Welcome to the slaughterhouse... Just kidding!
            Although, you will have to catch and take care of business
            with a few chickens...
        """.trimIndent(),
        quantity = 30,
        levels = 3,
        levelMultiplier = 1.5,
        eventClass = EntityDeathEvent::class.java
    ) { event, levelSnapshot, _ ->
        if (event.entity.type == EntityType.CHICKEN) {
            levelSnapshot.currentQuantity += 1
        }
    }

    val KILL_MONSTERS = ExplorerMile<EntityDeathEvent>(
        title = "Kill Monsters",
        desc = """
            Kill a few monsters, will ya?
            We promise it won't hurt... much.
        """.trimIndent(),
        quantity = 50,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = EntityDeathEvent::class.java
    ) { event, levelSnapshot, _ ->
        if (event.entity !is Monster) return@ExplorerMile
        levelSnapshot.currentQuantity += 1
    }

    val KILL_ANIMALS = ExplorerMile<EntityDeathEvent>(
        title = "Kill Animals",
        // todo: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 50,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = EntityDeathEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.entity is Animals) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val DIE = ExplorerMile<PlayerDeathEvent>(
        title = "Die",
        desc = """
            Oops! Looks like you took a little tumble.
            Don't worry, it happens to the best of us.
            Just try not to make it a habit, okay?
        """.trimIndent(),
        quantity = 1,
        levels = 2,
        eventClass = PlayerDeathEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val KILL_ANOTHER_PLAYER = ExplorerMile<EntityDamageByEntityEvent>(
        title = "Kill Players",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 4,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = EntityDamageByEntityEvent::class.java
    ) { event, levelSnapshot, _ ->
        val entity = event.entity as? LivingEntity ?: return@ExplorerMile
        if (event.finalDamage > entity.getAttribute(Attribute.MAX_HEALTH)!!.value) {
            levelSnapshot.currentQuantity += 1
        }
    }

    val MOVE_WRONGLY = ExplorerMile<PlayerFailMoveEvent>(
        title = "Move Wrongly",
        desc = """
            Yikes! The server just did a backflip and blamed you.
            Must be some next-level rubberbanding gymnastics!
        """.trimIndent(),
        quantity = 1,
        levels = 2,
        eventClass = PlayerFailMoveEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val EXPLORE = ExplorerMile<PlayerMoveEvent>(
        title = "Explore",
        desc = """
            Well aren't you just an explorer? We knew you had it in you!
            We'll reward you for every couple thousand blocks you walk.
            Just don't get lost, okay?
        """.trimIndent(),
        quantity = 5000,
        levels = 7,
        levelMultiplier = 3.0,
        eventClass = PlayerMoveEvent::class.java
    ) { event, levelSnapShot, _ ->
        // TODO: Make sure it does a proper check in the EventHandler
        val player = event.player
        if (player.isGliding || player.isSwimming) {
            return@ExplorerMile
        }
        levelSnapShot.currentQuantity += 1
    }

    val EXPLORE_ELYTRA = ExplorerMile<PlayerMoveEvent>(
        title = "Take to the Skies",
        desc = """
            Have fun <aqua>gliding</aqua> around!
            Just don't look down!
        """.trimIndent(),
        quantity = 5000,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerMoveEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.player.isGliding) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val JUMP = ExplorerMile<PlayerJumpEvent>(
        title = "Jump",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 100,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerJumpEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val CRAFT_GOLDEN_CARROTS = ExplorerMile<CraftItemEvent>(
        title = "Craft: Golden Carrots",
        desc = """
            A bunny after my own heart! Keep on crafting those golden carrots.
            Maybe even try eating one? I hear they're quite delicious!
        """.trimIndent(),
        quantity = 64,
        levels = 4,
        levelMultiplier = 2.0,
        eventClass = CraftItemEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.currentItem?.type == Material.GOLDEN_CARROT) {
            val amount = event.currentItem?.amount ?: 0
            levelSnapShot.currentQuantity += amount
        }
    }

    val CRAFT_BEACONS = ExplorerMile<CraftItemEvent>(
        title = "Craft: Beacons",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 72,
        eventClass = CraftItemEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.currentItem?.type == Material.BEACON) {
            val amount = event.currentItem?.amount ?: 0
            levelSnapShot.currentQuantity += amount
        }
    }

    val CRAFT_NETHERITE_BLOCKS = ExplorerMile<CraftItemEvent>(
        title = "Craft: Netherite Blocks",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 72,
        eventClass = CraftItemEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.currentItem?.type == Material.NETHERITE_BLOCK) {
            val amount = event.currentItem?.amount ?: 0
            levelSnapShot.currentQuantity += amount
        }
    }

    val EAT_GOLDEN_CARROTS = ExplorerMile<PlayerItemConsumeEvent>(
        title = "Eat: Golden Carrots",
        desc = """
            Tasty? Delicious? Scrumptious? Exquisite?
            Whatever you want to call it, these golden carrots are certainly a treat!
        """.trimIndent(),
        quantity = 16,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerItemConsumeEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.item.type == Material.GOLDEN_CARROT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val EAT_CARROTS = ExplorerMile<PlayerItemConsumeEvent>(
        title = "Eat: Carrots",
        desc = """
            Nature’s orange snack sticks— great for your eyes and even 
            better for pretending you're a rabbit in disguise.
        """.trimIndent(),
        quantity = 16,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerItemConsumeEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.item.type == Material.CARROT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val RUN_COMMANDS = ExplorerMile<PlayerCommandPreprocessEvent>(
        title = "Use Commands",
        // TODO: add a better description
        desc = """
            Using the Minecraft CLI?
            ... A better description would be nice here.
        """.trimIndent(),
        quantity = 100,
        eventClass = PlayerCommandPreprocessEvent::class.java
    ) { _, levelSnapShot, _ ->
        // TODO: record the commands used to ensure unique commands?
        levelSnapShot.currentQuantity += 1
    }

    val RUN_UNKNOWN_COMMANDS = ExplorerMile<UnknownCommandEvent>(
        title = "Run Commands That Don't Exist",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        levels = 2,
        eventClass = UnknownCommandEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val PICKUP_ITEMS = ExplorerMile<PlayerAttemptPickupItemEvent>(
        title = "PickerUpper",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 5000,
        levels = 5,
        levelMultiplier = 3.0,
        eventClass = PlayerAttemptPickupItemEvent::class.java
    ) { _, levelSnapShot, _ ->

        levelSnapShot.currentQuantity += 1
    }

    val SLEEP_IN_BED = ExplorerMile<PlayerBedEnterEvent>(
        title = "Sleep in a Bed",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        levels = 2,
        levelMultiplier = 15.0,
        eventClass = PlayerBedEnterEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.bedEnterResult == PlayerBedEnterEvent.BedEnterResult.OK) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val CHANGE_LOCALE = ExplorerMile<PlayerClientOptionsChangeEvent>(
        title = "Try a New Language",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerClientOptionsChangeEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.hasLocaleChanged()) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val SWAP_MAIN_HAND = ExplorerMile<PlayerClientOptionsChangeEvent>(
        title = "Swap Main Hand",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerClientOptionsChangeEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.hasMainHandChanged()) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val PUT_FLOWER_IN_FLOWERPOT = ExplorerMile<PlayerFlowerPotManipulateEvent>(
        title = "Put a Flower in a Flowerpot",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 3,
        levels = 2,
        levelMultiplier = 3.0,
        eventClass = PlayerFlowerPotManipulateEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.isPlacing) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_A_TOOL = ExplorerMile<PlayerItemBreakEvent>(
        title = "Break a Tool",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        levels = 3,
        levelMultiplier = 2.0,
        eventClass = PlayerItemBreakEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val NAME_AN_ENTITY = ExplorerMile<PlayerNameEntityEvent>(
        title = "Name an Entity",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerNameEntityEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val SIGN_A_BOOK = ExplorerMile<PlayerEditBookEvent>(
        title = "Sign a Book",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerEditBookEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.isSigning) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BOOST_ELYTRA_USING_ROCKET = ExplorerMile<PlayerElytraBoostEvent>(
        title = "Boost Your Flight with Fireworks",
        desc = """
            Fireworks are a great way to boost your flight!
            Make sure not to crash into anything while you're flying around!
        """.trimIndent(),
        quantity = 64,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerElytraBoostEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val USE_A_RIPTIDE_TRIDENT = ExplorerMile<PlayerRiptideEvent>(
        title = "Use a Riptide Trident",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 10,
        levels = 3,
        levelMultiplier = 3.5,
        eventClass = PlayerRiptideEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val DISABLE_ANOTHER_PLAYERS_SHIELD = ExplorerMile<PlayerShieldDisableEvent>(
        title = "Disable Another Player's Shield",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerShieldDisableEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val SHEAR_BLOCK = ExplorerMile<PlayerShearBlockEvent>(
        title = "Shear a Block",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 10,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerShearBlockEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val CHANGE_A_BEACONS_EFFECTS = ExplorerMile<PlayerChangeBeaconEffectEvent>(
        title = "Change a Beacon's Effects",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 20,
        eventClass = PlayerChangeBeaconEffectEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val THROW_EGGS = ExplorerMile<PlayerEggThrowEvent>(
        title = "Throw Eggs",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 128,
        levels = 2,
        levelMultiplier = 1.1,
        eventClass = PlayerEggThrowEvent::class.java
    ) { event, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val THROW_EGGS_THAT_HATCHED = ExplorerMile<PlayerEggThrowEvent>(
        title = "Throw Hatching Eggs",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 10,
        levels = 2,
        levelMultiplier = 1.5,
        eventClass = PlayerEggThrowEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.isHatching) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val EDIT_ITEM_FRAMES = ExplorerMile<PlayerItemFrameChangeEvent>(
        title = "Showoff Items with Item Frames",
        desc = """
            Show off your most valuable items with item frames!
            Hang up a few of these neat decorations and show off your collection!
        """.trimIndent(),
        quantity = 10,
        eventClass = PlayerItemFrameChangeEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.action == PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val REPAIR_ITEMS_USING_MENDING = ExplorerMile<PlayerItemMendEvent>(
        title = "Repair Items Using Mending",
        // TODO: add a better description
        desc = """
            A better description would be nice here.
        """.trimIndent(),
        quantity = 100,
        levels = 3,
        levelMultiplier = 2.0,
        eventClass = PlayerItemMendEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }


    val JOBS_EARN_MONEY_FROM_JOBS by safeLazy {
        ExplorerMile<JobsPaymentEvent>(
            title = "Earn Money From Jobs",
            desc = "TODO",
            quantity = 100000,
            levels = 15,
            eventClass = JobsPaymentEvent::class.java
        ) { event, levelSnapShot, _ ->
            val moneyEarned = event.payment[CurrencyType.MONEY] ?: return@ExplorerMile
            levelSnapShot.currentQuantity += moneyEarned.toInt()
        }
    }

    val QUICKSHOP_SELL_TO_SHOP by safeLazy {
        ExplorerMile<ShopSuccessPurchaseEvent>(
            title = "Quick Seller",
            desc = "Sell to a chest shop",
            quantity = 500,
            levels = 2,
            levelMultiplier = 1.5,
            eventClass = ShopSuccessPurchaseEvent::class.java
        ) { event, levelSnapShot, _ ->
            val shopOwner = event.shop.owner
            if (shopOwner != event.purchaser) {
                levelSnapShot.currentQuantity += event.amount
            }
        }
    }

    // oops? need to go to breweryx and fix the typo on this event name
    val BREWERYX_ADD_INGREDIENTS_TO_CAULDRON by safeLazy {
        ExplorerMile<IngedientAddEvent>(
            title = "Add Ingredients to Cauldron",
            desc = """
            Are you a fan of brewing? Try adding some ingredients to boiling cauldrons!
            Just don't forget to stir it up a bit!
        """.trimIndent(),
            quantity = 10,
            levels = 2,
            levelMultiplier = 1.5,
            eventClass = IngedientAddEvent::class.java
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREWERYX_PUKE by safeLazy {
        ExplorerMile<PlayerPukeEvent>(
            title = "Puke",
            desc = """
            Oops! Looks like you had a little too much to drink.
            Don't worry, it happens to the best of us. (Not really, but anywhoo...)
        """.trimIndent(),
            eventClass = PlayerPukeEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREWERYX_STUMBLE by safeLazy {
        ExplorerMile<PlayerPushEvent>(
            title = "Horizontal Ambitions",
            desc = """
            Cat got your foot? Or maybe you just had one too many drinks?
            Either way, it looks like you're <aqua>stumbling</aqua> around a bit.
        """.trimIndent(),
            quantity = 20,
            eventClass = PlayerPushEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREWERYX_DRUNK_TALK by safeLazy {
        ExplorerMile<PlayerChatDistortEvent>(
            title = "Wobbly Words",
            desc = """
            WOW, you must be really drunk!
            Even your <aqua>words</aqua> look super funny...
        """.trimIndent(),
            quantity = 10,
            eventClass = PlayerChatDistortEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREWERYX_DRINK_BREW by safeLazy {
        ExplorerMile<BrewDrinkEvent>(
            title = "Bottoms Up!",
            desc = """
            Having a few <aqua>brews</aqua> to drink? Well cheers to you!
            Just make sure not to get too tipsy!
        """.trimIndent(),
            quantity = 5,
            levels = 2,
            levelMultiplier = 2.0,
            eventClass = BrewDrinkEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val BETTERRTP_RTP by safeLazy {
        ExplorerMile<RTP_TeleportEvent>(
            title = "RTP",
            desc = """
            You just teleported to a new location using the RTP command.
            Now go explore and have fun!
        """.trimIndent(),
            quantity = 1,
            levels = 2,
            levelMultiplier = 2.0,
            eventClass = RTP_TeleportEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val DISCORDSRV_MESSAGE_FROM_DISCORD by safeLazy {
        ExplorerMile<DiscordGuildMessageReceivedEvent>(
            title = "Send a Message from Discord",
            desc = """
            Never miss out! Go ahead and send a few
            messages from Luma's #in-game chat channel.
        """.trimIndent(),
            quantity = 50,
            eventClass = DiscordGuildMessageReceivedEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val EMF_FISH by safeLazy {
        ExplorerMile<EMFFishEvent>(
            title = "Custom Fishing",
            desc = """
            You just caught a custom fish! 
            Keep fishing and see what else you can reel in!
        """.trimIndent(),
            quantity = 40,
            levels = 2,
            levelMultiplier = 1.5,
            eventClass = EMFFishEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val EMF_CATCH_LEGENDARY_FISH by safeLazy {
        ExplorerMile<EMFFishEvent>(
            title = "Catch Legendary Fish",
            // todo: add a better description
            desc = """
            You just caught a legendary fish! 
            Keep fishing and see what else you can reel in!
        """.trimIndent(),
            quantity = 10,
            eventClass = EMFFishEvent::class.java,
        ) { event, levelSnapShot, _ ->
            if (event.fish.rarity.id.uppercase() == "LEGENDARY") {
                levelSnapShot.currentQuantity += 1
            }
        }
    }

    val EMF_CATCH_JUNK_FISH by safeLazy {
        ExplorerMile<EMFFishEvent>(
            title = "Catch Junk",
            desc = """
            Eugh, who wants to catch junk?
            But hey, at least you can say you caught something!
        """.trimIndent(),
            quantity = 20,
            eventClass = EMFFishEvent::class.java,
        ) { event, levelSnapShot, _ ->
            if (event.fish.rarity.id.uppercase() == "JUNK") {
                levelSnapShot.currentQuantity += 1
            }
        }
    }

    val EMF_CATCH_COMMON_FISH by safeLazy {
        ExplorerMile<EMFFishEvent>(
            title = "Catch Common Fish",
            desc = """
            A common fish! While they aren't special,
            they're certainly abundant in the waters.
            At least you can say you caught something!
        """.trimIndent(),
            quantity = 80,
            eventClass = EMFFishEvent::class.java,
        ) { event, levelSnapShot, _ ->
            if (event.fish.rarity.id.uppercase() == "COMMON") {
                levelSnapShot.currentQuantity += 1
            }
        }
    }

    val MCMMO_TREEFELLER_BREAK_BLOCK by safeLazy {
        ExplorerMile<TreeFellerBlockBreakEvent>(
            title = "Break Blocks with TreeFeller",
            desc = """
            One swing and a forest wondering what just happened.
            Timber efficiency: 100%.
        """.trimIndent(),
            quantity = 1000,
            levels = 3,
            levelMultiplier = 4.0,
            eventClass = TreeFellerBlockBreakEvent::class.java,
        ) { event, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val TOWNY_VISIT_TOWNS by safeLazy {
        ExplorerMile<PlayerEntersIntoTownBorderEvent>(
            title = "Visit Various Towns",
            desc = "Explore towns around Luma!",
            quantity = 5,
            levels = 3,
            eventClass = PlayerEntersIntoTownBorderEvent::class.java
        ) { event, levelSnapShot, data ->
            val townId = event.enteredTown.uuid.toString()

            @Suppress("UNCHECKED_CAST")
            val visitedTowns: MutableList<String> = data["visitedTowns"] as? MutableList<String> ?: mutableListOf()

            if (visitedTowns.contains(townId)) {
                return@ExplorerMile
            }

            visitedTowns.add(townId)
            levelSnapShot.currentQuantity += 1
            data["visitedTowns"] = visitedTowns
        }
    }

    val TOWNY_INVITE_TO_TOWN by safeLazy {
        ExplorerMile<TownInvitePlayerEvent>(
            title = "Another Brick in the Neighborhood",
            // TODO: add a better description
            desc = """
            A better description would be nice here.
        """.trimIndent(),
            quantity = 1,
            levels = 2,
            eventClass = TownInvitePlayerEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val TOWNY_CLAIM_LAND by safeLazy {
        ExplorerMile<TownClaimEvent>(
            title = "Expanding the Borders",
            // todo: add a better description
            desc = """
            A better description would be nice here.
        """.trimIndent(),
            quantity = 30,
            levels = 2,
            eventClass = TownClaimEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val PINATAPARTY_VOTE_RECEIVED by safeLazy {
        ExplorerMile<VoteReceivedEvent>(
            title = "Vote for Luma!",
            // todo: Add a better description
            desc = """
            A better description would be nice here.
        """.trimIndent(),
            quantity = 8,
            levels = 10,
            eventClass = VoteReceivedEvent::class.java,
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val PINATAPARTY_HIT_PINATA by safeLazy {
        ExplorerMile<PinataHitEvent>(
            title = "Hit a Pinata",
            // TODO: add a better description
            desc = """
            A better description would be nice here.
        """.trimIndent(),
            quantity = 20,
            levels = 3,
            levelMultiplier = 1.5,
            eventClass = PinataHitEvent::class.java
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val PLAYERWARPS_CREATE_WARP by safeLazy {
        ExplorerMile<PlayerWarpCreateEvent>(
            title = "Create Player Warps",
            // TODO: add a better description
            desc = """
            A better description would be nice here.
        """.trimIndent(),
            quantity = 1,
            eventClass = PlayerWarpCreateEvent::class.java
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val PLAYERWARPS_TELEPORT_TO_WARPS by safeLazy {
        ExplorerMile<PlayerWarpTeleportEvent>(
            title = "Use Player Warps",
            // TODO: add a better description
            desc = """
            A better description would be nice here.
        """.trimIndent(),
            quantity = 2,
            levels = 2,
            levelMultiplier = 1.5,
            eventClass = PlayerWarpTeleportEvent::class.java
        ) { _, levelSnapShot, _ ->
            levelSnapShot.currentQuantity += 1
        }
    }

    val PLAYERWARPS_SPONSOR_SLOT by safeLazy {
        ExplorerMile<PlayerWarpSponsorEvent>(
            title = "Create a Player Warp Sponsor Slot",
            // TODO: add a better description
            desc = """
            A better description would be nice here.
        """.trimIndent(),
            quantity = 1,
            eventClass = PlayerWarpSponsorEvent::class.java
        ) { _, levelSnapshot, _ ->
            levelSnapshot.currentQuantity += 1
        }
    }


    // TODO: HoarderSellEvent
    // TODO: SimpleQuests

    val NABBIT_PICKUP_CARROTS = ExplorerMile<NabbitPickupCarrot>(
        title = "Pickup Carrots in 'The Nabbits'",
        desc = """
            Keep collecting those carrots, you'll earn plenty of 
            rewards for picking these bad boys up!
        """.trimIndent(),
        quantity = 300,
        levels = 2,
        levelMultiplier = 1.4,
        eventClass = NabbitPickupCarrot::class.java,
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val NABBIT_BECOME_NABBIT = ExplorerMile<NabbitChangeRole>(
        title = "Become a Nabbit in 'The Nabbits'",
        desc = """
            Aw, what a shame! You were caught by the Nabbit...
            But don't worry, you get a second chance to have some fun.
            Now go and catch some players!
        """.trimIndent(),
        quantity = 1,
        levels = 3,
        eventClass = NabbitChangeRole::class.java,
    ) { event, levelSnapShot, _ ->
        if (event.role == NabbitPlayer.Role.NABBIT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val NABBIT_BECOME_RABBIT = ExplorerMile<NabbitChangeRole>(
        title = "Become a Rabbit in 'The Nabbits'",
        desc = """
            Well, now you're just a bunny... But at least you 
            can hop around and collect some carrots!
        """.trimIndent(),
        quantity = 1,
        levels = 7,
        eventClass = NabbitChangeRole::class.java
    ) { event, levelSnapShot, _ ->
        if (event.role == NabbitPlayer.Role.RABBIT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val NABBIT_BE_THE_CHOSEN_NABBIT = ExplorerMile<NabbitChangeRole>(
        title = "Be the Chosen Nabbit in 'The Nabbits'",
        desc = """
            Look at that! The game chose you to be a Nabbit.
            Now it's your duty to go catch some players. Enjoy
            the extra rewards that come with it!
        """.trimIndent(),
        quantity = 1,
        levels = 2,
        eventClass = NabbitChangeRole::class.java
    ) { event, levelSnapShot, _ ->
        if (event.role == NabbitPlayer.Role.NABBIT_BOOTSTRAP) {
            levelSnapShot.currentQuantity += 1
        }
    }


    val NABBIT_SURVIVE_EXTENDED_TIME_PERIOD = ExplorerMile<NabbitSurviveExtendedTimePeriod>(
        title = "Survive for 1 Minute in 'The Nabbits'",
        // TODO: add a better description
        desc = """
            Surviving for 1 minute in 'The Nabbits' is no easy feat.
            But if you can do it, you're a true champion!
        """.trimIndent(),
        quantity = 1,
        levels = 3,
        eventClass = NabbitSurviveExtendedTimePeriod::class.java
    ) { event, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }



    // Lazy init for external plugins and problematic ExplorerMiles
    private inline fun <T> safeLazy(crossinline block: () -> T?): Lazy<T?> {
        return lazy {
            try {
                block()
            } catch (_: ClassNotFoundException) {
                null
            } catch (_: NoClassDefFoundError) {
                null
            } catch (e: Exception) {
                EventMain.getInstance().logger.log(Level.WARNING, "Error while loading ExplorerMile!", e)
                null
            }
        }
    }

    // Reflect
    private val KEYS: MutableMap<String, ExplorerMile<*>> = mutableMapOf()
    @JvmStatic fun asMap() = KEYS
    @JvmStatic fun values() = KEYS.values
    init {
        ExplorerMiles::class.java.declaredFields.forEach { field ->
            if (field.type == ExplorerMile::class.java) {
                try {
                    val explorerMile = field.get(null) as? ExplorerMile<*>
                    if (explorerMile != null) {
                        val fieldName = field.name
                        explorerMile.FIELD_NAME = fieldName
                        KEYS[fieldName] = explorerMile
                    } else {
                        Util.log("Warning: Field '${field.name}' in ExplorerMiles is null!")
                    }
                } catch (e: Throwable) {
                    Util.log("Error: Could not access or cast field '${field.name}' in ExplorerMiles: ${e.message}")
                }
            }
        }
    }
}
