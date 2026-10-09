package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.interfaces.models.MinigameRole;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.attack.NiceRestraint;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.model.GlowColorManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class Manor2Role extends MinigameRole {


    protected Manor2Role(EventPlayer eventPlayer) {
        super(eventPlayer);
    }

    public void retire(Manor2 manor2) {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
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
        player.setFreezeTicks(0);
        manor2.getGhostAttacks().abort(player.getUniqueId());
        NiceRestraint.release(player); // In case a grab never got to free them
        manor2.getRecorder().clearTrail(player);
    }

    public abstract void tick();

    public abstract void die();

    public abstract void initiatePlay();
}
