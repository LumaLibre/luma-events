package dev.lumas.events.games.logic.copycat.roles;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.copycat.ArenaBlocks;
import dev.lumas.events.games.logic.copycat.BlockPos;
import dev.lumas.events.games.logic.copycat.BuildScore;
import dev.lumas.events.games.logic.copycat.Copycat;
import dev.lumas.events.games.logic.copycat.CopycatBuild;
import dev.lumas.events.games.logic.copycat.Placed;
import dev.lumas.events.games.logic.copycat.Plot;
import dev.lumas.events.games.logic.copycat.Round;
import dev.lumas.events.games.logic.copycat.TileDetail;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Builder extends CopycatRole {

    private static final NamespacedKey DONE_KEY = new NamespacedKey(EventMain.getInstance(), "copycat_done");
    public static final int DONE_SLOT = 8;
    private static final ItemStack DONE_ITEM = Util.createItem(Material.LIME_DYE, meta -> {
        meta.displayName(Util.color("<green><b>I'm done!"));
        meta.lore(Util.color(List.of("<gray>Right click to submit your build", "<gray>and spectate until the round ends"), NamedTextColor.GRAY));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.getPersistentDataContainer().set(DONE_KEY, PersistentDataType.BOOLEAN, true);
    });

    @Getter
    private final Plot plot;
    private final Set<BlockPos> unresolved = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> extras = ConcurrentHashMap.newKeySet();
    private volatile Map<BlockPos, Placed> targets = Map.of();
    private volatile Set<BlockPos> hidden = Set.of();
    @Getter
    private volatile boolean locked;
    @Getter
    private volatile long lockedAt;
    private volatile boolean returning;
    private volatile boolean deployed;

    public Builder(Copycat ctx, EventPlayer eventPlayer, Plot plot) {
        super(ctx, eventPlayer);
        this.plot = plot;
    }

    @Override
    public void play() {
        this.teleportAsync(plot.getSpawn()).thenAccept(success -> this.operatePlayer(player -> {
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.setFallDistance(0f);
            player.setAllowFlight(true);
            player.setFlying(true);
            deployed = true;
        }));
    }

    @Override
    public void tick() {
        if (!deployed || !ctx.confinesBuilders()) return;
        this.operatePlayer(player -> {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            if (player.getFoodLevel() < 20) {
                player.setFoodLevel(20);
            }
            if (player.getFireTicks() > 0) {
                player.setFireTicks(0);
            }
            if (!plot.getInterior().contains(player.getLocation())) {
                returnToPlot(player);
            }
        });
    }

    @Override
    public CompletableFuture<Void> cleanup() {
        CopycatRole replacement = ctx.getRoleMap().get(getUuid());
        //boolean swapped = replacement != null && replacement != this;
        return this.operatePlayer(player -> {
            player.setFlying(false);
            player.setAllowFlight(false);
        });
    }

    public CompletableFuture<Void> clearInventory() {
        return this.operatePlayer(player -> player.getInventory().clear());
    }

    public CompletableFuture<Void> assign(Round round) {
        CopycatBuild build = round.getBuild();
        Map<BlockPos, BlockData> placed = plot.place(build);
        BlockPos offset = plot.placementOffset(build);
        List<List<BlockPos>> units = new ArrayList<>(build.getRemovableUnits());
        Collections.shuffle(units, Util.RANDOM);
        Set<BlockPos> chosen = new HashSet<>();
        int wanted = round.hiddenCount();
        int keep = Math.max(1, placed.size() - wanted);
        for (List<BlockPos> unit : units) {
            if (chosen.size() >= wanted) break;
            if (chosen.size() + unit.size() > placed.size() - keep) continue;
            for (BlockPos pos : unit) {
                chosen.add(pos.add(offset));
            }
        }
        Map<BlockPos, TileDetail> details = new HashMap<>();
        build.getDetails().forEach((pos, detail) -> details.put(pos.add(offset), detail));
        Map<BlockPos, Placed> targets = new HashMap<>();
        placed.forEach((pos, data) -> targets.put(pos, new Placed(data, details.get(pos))));
        this.targets = Map.copyOf(targets);
        this.hidden = Set.copyOf(chosen);
        this.unresolved.clear();
        this.extras.clear();
        this.locked = false;
        this.lockedAt = 0;
        World world = plot.getWorld();
        Map<BlockPos, BlockData> writes = new HashMap<>(plot.activate(build));
        writes.putAll(placed);
        return ArenaBlocks.write(world, writes).thenCompose(ignored -> ArenaBlocks.decorate(world, details));
    }

    public CompletableFuture<Void> uncover() {
        Set<BlockPos> toRemove = this.hidden;
        this.unresolved.addAll(toRemove);

        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        List<ItemStack> special = new ArrayList<>();
        for (BlockPos blockPos : toRemove) {
            Placed target = targets.get(blockPos);
            BlockData data = target.data();
            int amount = CopycatBuild.itemCount(data);
            if (amount > 0 && target.detail() != null) {
                special.add(CopycatBuild.itemFor(data, target.detail()));
            } else if (amount > 0) {
                counts.merge(CopycatBuild.placementItem(data), amount, Integer::sum);
            }
            Material bucket = CopycatBuild.extraBucket(data);
            if (bucket != null) {
                special.add(new ItemStack(bucket));
            }
        }
        this.operatePlayer(player -> {
            player.getInventory().clear();
            player.getInventory().setItem(DONE_SLOT, DONE_ITEM.clone());
            counts.forEach((material, amount) -> player.getInventory().addItem(new ItemStack(material, amount)));
            for (ItemStack item : special) {
                player.getInventory().addItem(item);
            }
            player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.4f);
        });
        return ArenaBlocks.fill(plot.getWorld(), toRemove, ArenaBlocks.AIR);
    }

    public static boolean isDoneItem(@Nullable ItemStack item) {
        return item != null && Util.hasPersistentKey(item, DONE_KEY);
    }

    public int hiddenCount() {
        return hidden.size();
    }

    public void lock() {
        if (locked) return;
        locked = true;
        lockedAt = System.currentTimeMillis();
    }

    public void onPlaced(BlockPos blockPos, BlockData data) {
        onPlaced(blockPos, new Placed(data, null));
    }

    public void onPlaced(BlockPos blockPos, Placed placed) {
        Placed target = targets.get(blockPos);
        if (target == null) {
            extras.add(blockPos);
        } else if (CopycatBuild.matches(target, placed)) {
            unresolved.remove(blockPos);
        } else {
            unresolved.add(blockPos);
        }
        checkPerfect();
    }

    public void onBroken(BlockPos blockPos) {
        if (targets.containsKey(blockPos)) {
            unresolved.add(blockPos);
        } else {
            extras.remove(blockPos);
        }
    }

    public CompletableFuture<Map<BlockPos, Placed>> readBuildArea() {
        return ArenaBlocks.read(plot.getWorld(), plot.buildAreaPositions());
    }

    public CompletableFuture<Void> clearBuildArea() {
        return ArenaBlocks.fill(plot.getWorld(), plot.fullAreaPositions(), ArenaBlocks.AIR);
    }

    public BuildScore score(Map<BlockPos, Placed> actual, long deadline) {
        int correct = 0;
        int missing = 0;
        int wrong = 0;
        int removedGiven = 0;
        int extra = 0;
        for (BlockPos blockPos : plot.buildAreaPositions()) {
            Placed target = targets.get(blockPos);
            Placed found = actual.getOrDefault(blockPos, Placed.AIR);
            if (target == null) {
                if (!found.isAir()) extra++;
                continue;
            }
            boolean wasHidden = hidden.contains(blockPos);
            if (CopycatBuild.matches(target, found)) {
                if (wasHidden) correct++;
            } else if (found.isAir()) {
                if (wasHidden) missing++;
                else removedGiven++;
            } else {
                wrong++;
            }
        }

        // TODO: cleanup
        int points = correct * Copycat.CORRECT_POINTS
            - wrong * Copycat.WRONG_PENALTY
            - removedGiven * Copycat.REMOVED_GIVEN_PENALTY
            - extra * Copycat.EXTRA_PENALTY;
        boolean perfect = missing == 0 && wrong == 0 && removedGiven == 0 && extra == 0;
        if (perfect) {
            points += Copycat.PERFECT_BONUS;
            if (locked && lockedAt > 0 && deadline > lockedAt) {
                points += (int) ((deadline - lockedAt) / 1000L) * Copycat.SPEED_POINTS_PER_SECOND;
            }
        }
        return new BuildScore(correct, missing, wrong, removedGiven, extra, points, perfect);
    }

    private void checkPerfect() {
        if (locked || !unresolved.isEmpty() || !extras.isEmpty()) return;
        lock();
        this.operatePlayer(player -> {
            player.showTitle(Util.title("<green><b>Perfect!", "<gray>Your build is locked in"));
            player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        });
        ctx.sendAudienceMessage("<green>" + getName() + "</green> finished a perfect copy!");
    }

    private void returnToPlot(Player player) {
        if (returning || ctx.isStopping()) return;
        returning = true;
        player.setFallDistance(0f);
        this.teleportAsync(plot.clamp(player.getLocation())).whenComplete((success, throwable) -> returning = false);
    }
}
