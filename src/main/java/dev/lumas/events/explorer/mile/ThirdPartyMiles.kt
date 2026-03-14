package dev.lumas.events.explorer.mile

//import com.dre.brewery.api.events.IngedientAddEvent
//import com.dre.brewery.api.events.PlayerChatDistortEvent
//import com.dre.brewery.api.events.PlayerPukeEvent
//import com.dre.brewery.api.events.PlayerPushEvent
//import com.dre.brewery.api.events.brew.BrewDrinkEvent
//import com.ghostchu.quickshop.api.event.economy.ShopSuccessPurchaseEvent
//import com.gmail.nossr50.api.TreeFellerBlockBreakEvent
//import com.oheers.fish.api.EMFFishEvent
//import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent
//import com.olziedev.playerwarps.api.events.warp.PlayerWarpSponsorEvent
//import com.olziedev.playerwarps.api.events.warp.PlayerWarpTeleportEvent
//import com.palmergames.bukkit.towny.event.TownClaimEvent
//import com.palmergames.bukkit.towny.event.TownInvitePlayerEvent
//import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoTownBorderEvent
//import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent
//import me.SuperRonanCraft.BetterRTP.references.customEvents.RTP_TeleportEvent
//import me.hexedhero.pp.api.PinataHitEvent
//import me.hexedhero.pp.api.VoteReceivedEvent

object ThirdPartyMiles : ExplorerMileContainer() {

//    val JOBS_EARN_MONEY_FROM_JOBS by safeLazy {
//        ExplorerMile<JobsPaymentEvent>(
//            title = "Earn Money From Jobs",
//            desc = """
//                Trade your irreplaceable and invaluable time in exchange for money, which you can use to buy necessary things like food, housing, and more!
//                What could be better than this?
//            """.trimIndent(),
//            quantity = 100000,
//            levelMultiplier = 1.5,
//            levels = 12,
//            eventClass = JobsPaymentEvent::class.java
//        ) { event, levelSnapShot, _ ->
//            val moneyEarned = event.payment[CurrencyType.MONEY] ?: return@ExplorerMile
//            levelSnapShot.currentQuantity += moneyEarned.toInt()
//        }
//    }

    /*val QUICKSHOP_SELL_TO_SHOP by safeLazy {
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
        desc = """
        All in all it's just another brick in the wall... ok, but seriously, you'll need to invite someone to your town for this one.
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
        desc = """
        Expand your town with none of the horrifying human rights violations intrinsic to real-world imperialism!
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
        desc = """
        Exercise your computer-bestowed right to type letters into your browser!
        Or skip straight to /vote! What fun!
    """.trimIndent(),
        quantity = 9,
        levels = 10,
        eventClass = VoteReceivedEvent::class.java,
    ) { _, levelSnapShot, _ ->
        levelSnapShot.currentQuantity += 1
    }
}

val PINATAPARTY_HIT_PINATA by safeLazy {
    ExplorerMile<PinataHitEvent>(
        title = "Hit a Pinata",
        desc = """
        No, no, we promise they're just pinatas, not llamas.
        You're <i>definitely</i> not punching a real llama.
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
        desc = """
        Show off by enabling others to visit your sick projects or farms. Better start asking for entry fees!
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
        desc = """
        See what others on Luma are up to or check out someones xp farm!
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
        desc = """
        Uh oh, seems like someone <i>really</i> wants others to see what they've made. Go ahead fancy pants.
    """.trimIndent(),
        quantity = 1,
        eventClass = PlayerWarpSponsorEvent::class.java
    ) { _, levelSnapshot, _ ->
        levelSnapshot.currentQuantity += 1
    }
}

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
    quantity = 2,
    levels = 3,
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
    desc = """
        Surviving for 1 minute in 'The Nabbits' is no easy feat.
        But if you can do it, you're a true champion!
    """.trimIndent(),
    quantity = 1,
    levels = 3,
    eventClass = NabbitSurviveExtendedTimePeriod::class.java
) { event, levelSnapShot, _ ->
    levelSnapShot.currentQuantity += 1
}*/
}