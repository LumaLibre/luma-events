package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.attack.NiceRestraint;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.model.GlowColorManager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

public abstract class Manor2Role extends MinigameRole {


    protected Manor2Role(EventPlayer eventPlayer) {
        super(eventPlayer);
    }

    public CompletableFuture<Void> retire(Manor2 manor2) {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        this.operatePlayer(player -> {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) instanceof ItemStack itemStack && Util.hasPersistentKey(itemStack, Manor2.ITEM_TAG)) {
                    inventory.setItem(i, null);
                }
            }
            player.clearActivePotionEffects();
            GlowColorManager.getInstance().update(player);
            manor2.getPacketGlow().removeTarget(player);
            manor2.getPacketGlow().removeViewer(player);
            completed.complete(null);
            player.setFreezeTicks(0);
            manor2.getGhostAttacks().abort(player.getUniqueId());
            NiceRestraint.release(player); // In case a grab never got to free them
        });
        return completed;
    }

    public abstract void tick();

    public abstract void die();

    public abstract CompletableFuture<Void> initiatePlay();
}
