package dev.lumas.events.games.interfaces.packet;


import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Externals;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class BlockAnimationPlatform {

    private final BlockAnimationPlatformConfig config;
    private final Predicate<Block> replacePredicate;
    private final BreakAnimationSender breakSender;
    private final Supplier<List<Player>> viewersSupplier;
    private final Supplier<Boolean> isActiveSupplier; // e.g. decayArmed / gameRunning

    private final Map<UUID, PlatformInstance> platformById = new HashMap<>();
    private final Map<BlockPos, Deque<UUID>> platformStackByBlock = new HashMap<>();
    private final Map<BlockPos, BlockData> trueOriginalByBlock = new HashMap<>();

    public BlockAnimationPlatform(
            BlockAnimationPlatformConfig config,
            Predicate<Block> replacePredicate,
            BreakAnimationSender breakSender,
            Supplier<List<Player>> viewersSupplier,
            Supplier<Boolean> isActiveSupplier
    ) {
        this.config = config;
        this.replacePredicate = replacePredicate;
        this.breakSender = breakSender;
        this.viewersSupplier = viewersSupplier;
        this.isActiveSupplier = isActiveSupplier;
    }

    public void cleanup() {
        for (UUID id : new ArrayList<>(platformById.keySet())) {
            restore(id);
        }
        platformById.clear();
        platformStackByBlock.clear();
        trueOriginalByBlock.clear();
    }

    public UUID spawnUnderPlayer(Player player) {
        World w = player.getWorld();
        int baseY = player.getLocation().getBlockY() - 1;
        int cx = player.getLocation().getBlockX();
        int cz = player.getLocation().getBlockZ();
        return spawnAt(w, cx, baseY, cz);
    }

    public UUID spawnAt(World w, int cx, int y, int cz) {
        if (!isActiveSupplier.get()) return null;

        UUID platformId = UUID.randomUUID();
        Set<BlockPos> blocks = new HashSet<>();

        for (int dx = -config.radiusX(); dx <= config.radiusX(); dx++) {
            for (int dz = -config.radiusZ(); dz <= config.radiusZ(); dz++) {
                int x = cx + dx;
                int z = cz + dz;

                Block b = w.getBlockAt(x, y, z);
                if (!replacePredicate.test(b)) continue;

                BlockPos pos = new BlockPos(x, y, z);
                blocks.add(pos);

                Deque<UUID> stack = platformStackByBlock.computeIfAbsent(pos, k -> new ArrayDeque<>());
                if (stack.isEmpty()) trueOriginalByBlock.put(pos, b.getBlockData().clone());
                stack.push(platformId);

                b.setType(config.material(), false);
            }
        }

        if (blocks.isEmpty()) return null;

        PlatformInstance inst = new PlatformInstance(platformId, w, blocks);
        platformById.put(platformId, inst);

        int warnTicks = Math.max(0, Math.min(config.warnTicks(), config.lifetimeTicks()));
        if (warnTicks > 0 && config.sendBreakAnimation()) {
            Executors.delayedSync(Math.max(1, config.lifetimeTicks() - warnTicks), () -> {
                if (!isActiveSupplier.get()) return;
                animateBreaking(platformId, blocks, warnTicks);
            });
        }

        Executors.delayedSync(config.lifetimeTicks(), () -> restore(platformId));
        return platformId;
    }

    public void restore(UUID platformId) {
        PlatformInstance inst = platformById.remove(platformId);
        if (inst == null) return;

        List<Player> viewers = viewersSupplier.get();
        clearBreakAnim(platformId, inst.blocks(), viewers);

        for (BlockPos pos : inst.blocks()) {
            Deque<UUID> st = platformStackByBlock.get(pos);
            if (st == null || st.isEmpty()) continue;

            if (platformId.equals(st.peek())) st.pop();
            else st.remove(platformId);

            Block b = inst.world().getBlockAt(pos.x(), pos.y(), pos.z());

            if (st.isEmpty()) {
                BlockData original = trueOriginalByBlock.remove(pos);
                if (original != null) b.setBlockData(original, false);
                platformStackByBlock.remove(pos);
            } else {
                b.setType(config.material(), false);
            }
        }
    }

    private void animateBreaking(UUID platformId, Set<BlockPos> blocks, int durationTicks) {
        Set<BlockPos> topOwned = new HashSet<>();
        for (BlockPos pos : blocks) {
            Deque<UUID> st = platformStackByBlock.get(pos);
            if (st != null && !st.isEmpty() && platformId.equals(st.peek())) topOwned.add(pos);
        }
        if (topOwned.isEmpty()) return;

        List<Player> viewers = viewersSupplier.get();
        AtomicInteger ticks = new AtomicInteger();

        Executors.repeatingSync(1, task -> {
            if (!isActiveSupplier.get() || ticks.getAndIncrement() >= durationTicks) {
                clearBreakAnim(platformId, topOwned, viewers);
                task.cancel();
                return;
            }

            int stage = Math.min(9, (int) Math.floor((ticks.get() / (double) durationTicks) * 10.0));
            for (BlockPos pos : topOwned) {
                Deque<UUID> st = platformStackByBlock.get(pos);
                if (st == null || st.isEmpty() || !platformId.equals(st.peek())) continue;

                int breakerId = Objects.hash(platformId, pos.x(), pos.y(), pos.z());
                for (Player viewer : viewers) {
                    breakSender.sendBreakAnimation(viewer, breakerId, pos.x(), pos.y(), pos.z(), stage);
                }
            }
        });
    }

    private void clearBreakAnim(UUID platformId, Set<BlockPos> blocks, List<Player> viewers) {
        if (!config.sendBreakAnimation()) return;

        for (BlockPos pos : blocks) {
            int breakerId = Objects.hash(platformId, pos.x(), pos.y(), pos.z());
            for (Player viewer : viewers) {
                breakSender.clearBreakAnimation(viewer, breakerId, pos.x(), pos.y(), pos.z());
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private record BlockPos(int x, int y, int z) {}
    private record PlatformInstance(UUID id, World world, Set<BlockPos> blocks) {}

    public static class Builder {

        private BlockAnimationPlatformConfig config = BlockAnimationPlatformConfig.defaultConfig();
        private Predicate<Block> replacePredicate = block -> true;
        private BreakAnimationSender breakSender = Externals.pluginExists("ProtocolLib") ? new ProtocolLibBreakAnimationSender() : BreakAnimationSender.noop();
        private Supplier<List<Player>> viewersSupplier = () -> new ArrayList<>(Bukkit.getOnlinePlayers());
        private Supplier<Boolean> isActiveSupplier = () -> true;

        public Builder config(BlockAnimationPlatformConfig config) {
            this.config = config;
            return this;
        }

        public Builder replacePredicate(Predicate<Block> replacePredicate) {
            this.replacePredicate = replacePredicate;
            return this;
        }

        public Builder breakSender(BreakAnimationSender breakSender) {
            this.breakSender = breakSender;
            return this;
        }

        public Builder viewersSupplier(Supplier<List<Player>> viewersSupplier) {
            this.viewersSupplier = viewersSupplier;
            return this;
        }

        public Builder isActiveSupplier(Supplier<Boolean> isActiveSupplier) {
            this.isActiveSupplier = isActiveSupplier;
            return this;
        }

        public BlockAnimationPlatform build() {
            return new BlockAnimationPlatform(
                    config,
                    replacePredicate,
                    breakSender,
                    viewersSupplier,
                    isActiveSupplier
            );
        }
    }
}
