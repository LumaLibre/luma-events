package dev.lumas.events.games.models;

import dev.lumas.events.utility.Executors;
import lombok.Getter;
import me.danjono.inventoryrollback.data.LogType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import me.danjono.inventoryrollback.inventory.SaveInventory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class InventorySnapshot {

    private final UUID owner;
    private final ItemStack[] contents;
    private boolean backedUp;


    public InventorySnapshot(UUID owner, ItemStack[] contents) {
        this.owner = owner;
        this.contents = contents;
        this.backedUp = false;
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(owner);
    }

    public boolean restore() {
        Player player = getPlayer();
        if (player != null) {
            restore(player);
            return true;
        }
        return false;
    }

    public void restore(Player player) {
        Executors.runSync(player, () -> {
            player.getInventory().clear();
            player.getInventory().setContents(contents);
        });
    }

    public CompletableFuture<Boolean> backup() {
        Player player = getPlayer();
        if (player == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Executors.runSync(player, () -> {
            SaveInventory saveInventory = new SaveInventory(player, LogType.FORCE, null, null);
            saveInventory.snapshotAndSave(player.getInventory(), player.getEnderChest(), true);
            this.backedUp = true;
            future.complete(true);
        });
        return future;
    }
}
