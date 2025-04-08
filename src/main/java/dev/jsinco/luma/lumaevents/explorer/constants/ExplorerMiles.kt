package dev.jsinco.luma.lumaevents.explorer.constants

import com.gamingmesh.jobs.api.JobsPaymentEvent
import com.gamingmesh.jobs.container.CurrencyType
import com.ghostchu.quickshop.api.event.economy.ShopSuccessPurchaseEvent
import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoTownBorderEvent
import dev.jsinco.luma.lumaevents.explorer.BlockClone
import dev.jsinco.luma.lumaevents.explorer.ExplorerMile
import dev.jsinco.luma.lumaevents.utility.Util
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material

object ExplorerMiles {


    val SELL_TO_SHOP = ExplorerMile<ShopSuccessPurchaseEvent>(
        title = "Quick Seller",
        desc = "Sell to a chest shop",
        quantity = 500,
        levels = 2,
        levelMultiplier = 1.5,
        eventClass = ShopSuccessPurchaseEvent::class.java
    ) { event, levelSnapShot, _ ->
        val shopOwner = event.shop.owner
        if (shopOwner == event.purchaser) {
            return@ExplorerMile
        }

        levelSnapShot.currentQuantity += event.amount
    }

    val DISCUSS_EASTER_RELATED_TOPICS = ExplorerMile(
        title = "Discuss Easter Related Topics",
        desc = "Sell to a chest shop",
        quantity = 1,
        levels = 5,
        eventClass = AsyncChatEvent::class.java,
    ) { event, levelSnapShot, data ->
        // TODO
        val wordList: MutableList<String> = data["wordList"] as? MutableList<String> ?: mutableListOf()

        val plainText = PlainTextComponentSerializer.plainText().serialize(event.message())
        plainText.split(" ").forEach { word ->
            levelSnapShot.currentQuantity += 1
            wordList.add(word)
        }

        data["wordList"] = wordList
    }

    val EARN_MONEY_FROM_JOBS = ExplorerMile<JobsPaymentEvent>(
        title = "Earn Money From Jobs",
        desc = "TODO",
        quantity = 100000,
        levels = 15,
        eventClass = JobsPaymentEvent::class.java
    ) { event, levelSnapShot, _ ->
        val moneyEarned = event.payment[CurrencyType.MONEY] ?: return@ExplorerMile
        levelSnapShot.currentQuantity += moneyEarned.toInt()
    }

    val FARM_CARROTS = ExplorerMile<BlockClone>(
        title = "Farm Carrots",
        desc = "TODO",
        quantity = 350,
        levels = 2,
        levelMultiplier = 2.0,
        eventClass = BlockClone::class.java,
    ) { event, levelSnapShot, _ ->
        if (event.type == Material.CARROTS) {
            levelSnapShot.currentQuantity += 1
        }
    }

    val VISIT_TOWNS = ExplorerMile<PlayerEntersIntoTownBorderEvent>(
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

    //val DIE = ExplorerMile<PlayerDeathEvent>()



    private val KEYS: MutableMap<String, ExplorerMile<*>> = mutableMapOf()
    @JvmStatic fun asMap() = KEYS
    init {
        try {
            ExplorerMiles::class.java.declaredFields.forEach { field ->
                if (field.type == ExplorerMile::class.java) {
                    try {
                        // Use reflection to get the field value, safely check for null
                        val explorerMile = field.get(null) as? ExplorerMile<*>
                        if (explorerMile != null) {
                            val fieldName = field.name
                            explorerMile.FIELD_NAME = fieldName
                            KEYS[fieldName] = explorerMile
                        } else {
                            // Log a warning if the field is null
                            Util.log("Warning: Field '${field.name}' in ExplorerMiles is null!")
                        }
                    } catch (e: Throwable) {
                        // Handle any reflection or casting issues
                        Util.log("Error: Could not access or cast field '${field.name}' in ExplorerMiles: ${e.message}")
                    }
                }
            }
        } catch (e: NoClassDefFoundError) {
            e.printStackTrace()
        }
    }
}
