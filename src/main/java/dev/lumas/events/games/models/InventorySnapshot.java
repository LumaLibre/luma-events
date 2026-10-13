package dev.lumas.events.games.models;

import dev.lumas.core.util.Logging;
import dev.lumas.events.utility.Executors;
import lombok.Getter;
import me.danjono.inventoryrollback.data.LogType;
import me.danjono.inventoryrollback.inventory.SaveInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class InventorySnapshot {

    private final UUID owner;
    private final ItemStack[] contents;
    private boolean backedUp;


    public InventorySnapshot(UUID owner, ItemStack[] contents) {
        this.owner = owner;
        this.contents = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            this.contents[i] = contents[i] == null ? null : contents[i].clone();
        }
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
        Runnable runnable = () -> player.getInventory().setContents(contents);
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            runnable.run();
        } else {
            Executors.runSync(player, runnable);
        }
    }

    public CompletableFuture<Boolean> backup() {
        Player player = getPlayer();
        if (player == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Executors.runSync(player, () -> {
            try {
                SaveInventory saveInventory = new SaveInventory(player, LogType.FORCE, null, null);
                SaveInventory.PlayerDataSnapshot data = saveInventory.createSnapshot(player.getInventory(), player.getEnderChest());
                String name = player.getName();
                Executors.runAsync(() -> {
                    try {
                        saveInventory.save(data, false);
                    } catch (Throwable throwable) {
                        Logging.errorLog("InventoryRollbackPlus could not save the backup for " + name, throwable);
                    }
                });
            } catch (Throwable throwable) {
                Logging.errorLog("InventoryRollbackPlus could not back up " + player.getName() + ", keeping the in-memory snapshot only", throwable);
            }
            this.backedUp = true;
            future.complete(true);
        });
        return future;
    }
}
