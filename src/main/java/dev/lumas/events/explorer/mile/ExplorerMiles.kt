package dev.lumas.events.explorer.mile

import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.explorer.custom.BlockBrokenExplorerEvent
import dev.lumas.events.explorer.custom.BlockPlacedExplorerEvent
import io.papermc.paper.event.block.PlayerShearBlockEvent
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PlayerArmSwingEvent
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerNameEntityEvent
import io.papermc.paper.event.player.PlayerShieldDisableEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Ageable
import org.bukkit.entity.Animals
import org.bukkit.entity.EntityType
import org.bukkit.entity.Monster
import org.bukkit.event.command.UnknownCommandEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerEggThrowEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.inventory.MainHand

@Register(Autowire.SERVICE)
@Suppress("UNUSED", "RemoveExplicitTypeArguments", "UNCHECKED_CAST")
object ExplorerMiles : ExplorerMileContainer() {

    val CHAT = ExplorerMile<AsyncChatEvent>(
        title = "Chatterbox",
        desc = """
            Talkative much? We get it, you love to chat.
            But don't worry, we won't judge you for it.
        """.trimIndent(),
        //objective = "Send messages in chat.",
        quantity = 100,
        levels = 2,
        levelMultiplier = 0.5,
        eventClass = AsyncChatEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val DISCUSS_EASTER_RELATED_TOPICS = ExplorerMile<AsyncChatEvent>(
        title = "Discuss Easter Related Topics",
        desc = "Say easter related words in chat.",
        //objective = "",
        quantity = 2,
        levels = 3,
        eventClass = AsyncChatEvent::class.java,
    ) { event, levelSnapShot, data ->
        val easterTopicWords =
            listOf("egg", "bunny", "chocolate", "hunt", "basket", "spring", "candy", "rabbit", "easter", "event")
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
        desc = """
            Channel your ophthalmologist's
            agrarian impulses!
        """.trimIndent(),
        //objective = "Break carrots.",
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
        desc = """
            To prepare for easter 2026 we need as many carrots as we can get our hands on. Better start planting!
        """.trimIndent(),
        //objective = "Place carrots.",
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
        //objective = "Break diamond ores.",
        quantity = 100,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockBrokenExplorerEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.DIAMOND_ORE || event.type == Material.DEEPSLATE_DIAMOND_ORE) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_EMERALD_ORES = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Emerald Jeweler",
        desc = """
            Trade your way to riches by breaking emerald ores! 
            Remember, villagers might offer you deals, but they sure love crushing loaf.
        """.trimIndent(),
        //objective = "Break emerald ores.",
        quantity = 50,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockBrokenExplorerEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.EMERALD_ORE || event.type == Material.DEEPSLATE_EMERALD_ORE) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val BREAK_OBSIDIAN = ExplorerMile<BlockBrokenExplorerEvent>(
        title = "Obsidian Crusher",
        desc = """
            The hardest block in all of Minecraft! Right?
            Well, at least I <i>think</i> it is....
        """.trimIndent(),
        //objective = "Break obsidian.",
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
        desc = """
            'I'm a valuable ore from the nether that's immune to explosions'.
            Do you know who I am? Better get to mining!
        """.trimIndent(),
        //objective = "Break ancient debris.",
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
        //objective = "Break any block.",
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
        //objective = "Kill baby rabbits.",
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
        desc = """
            Aww, poor animals :< (You monster!)
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

//    val KILL_ANOTHER_PLAYER = ExplorerMile<EntityDamageByEntityEvent>(
//        title = "Kill Players",
//        desc = """
//            Go get em. (With permission!)
//        """.trimIndent(),
//        quantity = 4,
//        levels = 2,
//        levelMultiplier = 2.0,
//        eventClass = EntityDamageByEntityEvent::class.java
//    ) { event, levelSnapshot, _ ->
//        val entity = event.entity as? LivingEntity ?: return@ExplorerMile
//        if (event.finalDamage > entity.getAttribute(Attribute.MAX_HEALTH)!!.value) {
//            levelSnapshot.currentQuantity += 1
//        }
//    }

//    val MOVE_WRONGLY = ExplorerMile<PlayerFailMoveEvent>(
//        title = "Move Wrongly",
//        desc = """
//            Yikes! The server just did a backflip and blamed you.
//            Must be some next-level rubberbanding gymnastics!
//        """.trimIndent(),
//        quantity = 1,
//        levels = 2,
//        eventClass = PlayerFailMoveEvent::class.java
//    ) { _, levelSnapShot, _ ->
//        levelSnapShot.currentQuantity += 1
//    }

    val EXPLORE = ExplorerMile<PlayerMoveEvent>(
        title = "Explore",
        desc = """
            Well aren't you just an explorer? We knew you had it in you!
            We'll reward you for every couple thousand blocks you walk.
            Just don't get lost, okay?
        """.trimIndent(),
        quantity = 50000,
        levels = 4,
        levelMultiplier = 3.0,
        eventClass = PlayerMoveEvent::class.java
    ) { event, levelSnapShot, _ ->
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
        quantity = 50000,
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
        desc = """
            Just put one foot in front of the other, and - wait, no, that's walking...
            Ok, ok, so you're going to want to bend your legs, and then lower yourself- 
            Wait, no, that's sitting... Does anyone have a handle on this 'jumping' stuff?
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
        if (event.recipe.result.type == Material.GOLDEN_CARROT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val CRAFT_BEACONS = ExplorerMile<CraftItemEvent>(
        title = "Craft: Beacons",
        desc = """
            Craft a beacon to flex your wealth and finally make that giant pyramid do something useful!
        """.trimIndent(),
        quantity = 72,
        eventClass = CraftItemEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.recipe.result.type == Material.BEACON) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val CRAFT_NETHERITE_BLOCKS = ExplorerMile<CraftItemEvent>(
        title = "Craft: Netherite Blocks",
        desc = """
            A little birdie told me this stuff is
            pretty rare...
        """.trimIndent(),
        quantity = 72,
        eventClass = CraftItemEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.recipe.result.type == Material.NETHERITE_BLOCK) {
            levelSnapShot.currentQuantity += 1
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
        desc = """
            Have you ever tried '/bellyflop' or any of the other fun commands on luma?
            I dare you to use as many as you can!
        """.trimIndent(),
        quantity = 100,
        eventClass = PlayerCommandPreprocessEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val RUN_UNKNOWN_COMMANDS = ExplorerMile<UnknownCommandEvent>(
        title = "Run Commands That Don't Exist",
        desc = """
            'HOUSTON WE HAVE A PROBLEM'. I don't believe we have that command on luma...
        """.trimIndent(),
        quantity = 10,
        levels = 2,
        eventClass = UnknownCommandEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }
// TODO: is recursive
//    val PICKUP_ITEMS = ExplorerMile<PlayerAttemptPickupItemEvent>(
//        title = "PickerUpper",
//        desc = """
//            You can never have enough items- Right?
//        """.trimIndent(),
//        quantity = 5000,
//        levels = 5,
//        levelMultiplier = 3.0,
//        eventClass = PlayerAttemptPickupItemEvent::class.java
//    ) { event, levelSnapShot, _ ->
//        levelSnapShot.currentQuantity += 1
//    }

    val SLEEP_IN_BED = ExplorerMile<PlayerBedEnterEvent>(
        title = "Sleep in a Bed",
        desc = """
            This is something normal people do, I'm told.
            You're normal, right?
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
        desc = """
            Wanna try out a new language? Give it a shot! Maybe try <aqua>LOLCAT</aqua>?
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerClientOptionsChangeEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.locale == "lol_us") {
            levelSnapShot.currentQuantity += 1
        }
    }

    val SWAP_MAIN_HAND = ExplorerMile<PlayerClientOptionsChangeEvent>(
        title = "Swap Main Hand",
        desc = """
            Hi, I noticed you've been using that main hand of yours for quite a while. Perhaps you'd like to... swap it? 
            How badly can one really need a specific hand anyway?
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerClientOptionsChangeEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.mainHand == MainHand.LEFT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val PUT_FLOWER_IN_FLOWERPOT = ExplorerMile<PlayerFlowerPotManipulateEvent>(
        title = "Put a Flower in a Flowerpot",
        desc = """
            Look at you go, interior designer and all that... Way to go!
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
        desc = """
            Uh oh! Maybe try repairing them instead?
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
        desc = """
            One time I tried to name a sheep 'jeb_' to honor the creator of minecraft, never saw a sheep overreact more...
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerNameEntityEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val SIGN_A_BOOK = ExplorerMile<PlayerEditBookEvent>(
        title = "Sign a Book",
        desc = """
            Your preeminent status as an author precedes you.
            Go on, Stardust... they're waiting for you.
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
        desc = """
            Yeet yourself through the sky like Poseidon's favorite javelin on a water slide!
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
        desc = """
            Politely ask your friend's shield to take a break- with an axe to the face...
        """.trimIndent(),
        quantity = 1,
        eventClass = PlayerShieldDisableEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val SHEAR_BLOCK = ExplorerMile<PlayerShearBlockEvent>(
        title = "Shear a Block",
        desc = """
            Huh? I didn't know you could do that!
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
        desc = """
            Wait, I was just using my beacon as a lamp... you're telling me it has effects to change, too?
        """.trimIndent(),
        quantity = 20,
        eventClass = PlayerChangeBeaconEffectEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val THROW_EGGS = ExplorerMile<PlayerEggThrowEvent>(
        title = "Throw Eggs",
        desc = """
            They say that throwing the first egg over the coop is good luck!
            Just do it... oh, <aqua>127+</aqua> more times and I'm sure luck will come!
        """.trimIndent(),
        quantity = 128,
        levels = 2,
        levelMultiplier = 1.1,
        eventClass = PlayerEggThrowEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val THROW_EGGS_THAT_HATCHED = ExplorerMile<PlayerEggThrowEvent>(
        title = "Throw Hatching Eggs",
        desc = """
            Your tenacity in the realm of egg-throwing is admirable.
            I'm not sure the nearby grocery stores can keep up with your demand, however...
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
        desc = """
            Apparently, in ancient times, people used to repair their items by using the material the original tool was made of!
            Then, Thomas Mending came along and invented mending. What a feat!
        """.trimIndent(),
        quantity = 100,
        levels = 3,
        levelMultiplier = 2.0,
        eventClass = PlayerItemMendEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val PREPARE_ANVIL = ExplorerMile<PrepareAnvilEvent>(
        title = "Prepare Anvil",
        desc = """
            You know, I heard that if you drop a few anvils on each other, they can make a really cool sound!
            But don't ask me how to do it, I'm not a blacksmith!
        """.trimIndent(),
        quantity = 1,
        eventClass = PrepareAnvilEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val DROP_CARROTS = ExplorerMile<PlayerDropItemEvent>(
        title = "Drop Carrots",
        desc = """
            Drop a few carrots on the ground. 
            I hear they make great decorations for your house!
        """.trimIndent(),
        quantity = 1000,
        levels = 3,
        levelMultiplier = 2.0,
        eventClass = PlayerDropItemEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.itemDrop.itemStack.type == Material.CARROT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val DROP_GOLD_INGOTS = ExplorerMile<PlayerDropItemEvent>(
        title = "Drop Gold Ingots",
        desc = """
            Drop a few gold ingots on the ground. 
            Make sure to pick them back up though, wouldn't want to lose 'em!
        """.trimIndent(),
        quantity = 1000,
        levels = 3,
        levelMultiplier = 2.0,
        eventClass = PlayerDropItemEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.itemDrop.itemStack.type == Material.GOLD_INGOT) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val LOGIN_DAILY = ExplorerMile<PlayerJoinEvent>(
        title = "Join Luma Daily",
        desc = """
            Join Luma every day to get a special reward!
        """.trimIndent(),
        quantity = 1,
        levels = 5,
        eventClass = PlayerJoinEvent::class.java
    ) { _, levelSnapshot, data ->
        val timeStamps = data["timeStamps"] as? MutableList<Long> ?: mutableListOf<Long>()
        val currentTime = System.currentTimeMillis()
        val ONE_DAY = 86400000L // 24 hours in milliseconds

        if (timeStamps.isEmpty() || currentTime - timeStamps.last() > ONE_DAY) {
            timeStamps.add(currentTime)
            levelSnapshot.currentQuantity += 1
            data["timeStamps"] = timeStamps
        }
    }

    val SWING_YOUR_OFFHAND = ExplorerMile<PlayerArmSwingEvent>(
        title = "Swing Your Offhand",
        desc = """
            Swing your offhand like a pro!
            Don't forget to put it back in your pocket when you're done!
        """.trimIndent(),
        quantity = 100,
        levels = 2,
        levelMultiplier = 3.0,
        eventClass = PlayerArmSwingEvent::class.java
    ) { event, levelSnapShot, _ ->
        if (event.animationType == PlayerAnimationType.OFF_ARM_SWING) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val INTERACT_WITH_ENTITIES = ExplorerMile<PlayerInteractEntityEvent>(
        title = "Interact with Entities",
        desc = """
            Interact with entities like a pro!
            Make sure to say hello first!
        """.trimIndent(),
        quantity = 100,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerInteractEntityEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }


    val SHEAR_ENTITIES = ExplorerMile<PlayerShearEntityEvent>(
        title = "Shear Entities",
        desc = """
            I wonder which entities are shearable?
        """.trimIndent(),
        quantity = 50,
        levels = 3,
        levelMultiplier = 1.5,
        eventClass = PlayerShearEntityEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val FILL_BUCKETS = ExplorerMile<PlayerBucketFillEvent>(
        title = "Fill Buckets",
        desc = """
            Fill your buckets with water, lava, or whatever else you can find!
            Also try emptying them out when you're done!
        """.trimIndent(),
        quantity = 100,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerBucketFillEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

    val EMPTY_BUCKETS = ExplorerMile<PlayerBucketEmptyEvent>(
        title = "Empty Buckets",
        desc = """
            Got some filled up buckets?
            Let's go ahead and empty those out!
        """.trimIndent(),
        quantity = 200,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = PlayerBucketEmptyEvent::class.java
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }

}