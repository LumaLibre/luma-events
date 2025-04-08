package dev.jsinco.luma.lumaevents.explorer.events;

import com.gamingmesh.jobs.api.JobsPaymentEvent;
import com.ghostchu.quickshop.api.event.economy.ShopSuccessPurchaseEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.explorer.BlockClone;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.UUID;

@AutoRegister(RegisterType.LISTENER)
public class ExplorerListeners implements Listener {

    public static void fire(Object event, Player player) {
        fire(event, player.getUniqueId());
    }

    public static void fire(Object event, UUID player) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player);

        Bukkit.getScheduler().runTaskAsynchronously(EventMain.getInstance(), () -> {
            eventPlayer.fireForExplorerMiles(event);
        });
    }

    @EventHandler
    public void quickShopSuccessfulTransaction(ShopSuccessPurchaseEvent event) {
        fire(event, event.getPurchaser().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onJobsPayment(JobsPaymentEvent event) {
        fire(event, event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        fire(new BlockClone(event.getBlock()), event.getPlayer());
    }
}
