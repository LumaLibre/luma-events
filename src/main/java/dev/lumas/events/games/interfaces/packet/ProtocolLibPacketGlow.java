package dev.lumas.events.games.interfaces.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import dev.lumas.events.EventMain;
import dev.lumas.lumacore.utility.Logging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public final class ProtocolLibPacketGlow implements PacketGlow {

    private static final int SHARED_FLAGS_INDEX = 0;
    private static final byte GLOWING_FLAG = 0x40;
    private static final WrappedDataWatcher.Serializer BYTE_SERIALIZER = WrappedDataWatcher.Registry.get((Type) Byte.class);

    private final BiPredicate<Player, Entity> filter;
    private final Map<UUID, Set<Integer>> viewers = new ConcurrentHashMap<>(); // viewer -> ids of targets currently glowing for them
    private final Map<Integer, Entity> targetsById = new ConcurrentHashMap<>();
    private final PacketAdapter listener = new PacketAdapter(EventMain.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
        @Override
        public void onPacketSending(PacketEvent event) {
            if (event.isPlayerTemporary()) {
                return;
            }
            Set<Integer> glowing = viewers.get(event.getPlayer().getUniqueId());
            if (glowing == null) {
                return;
            }

            PacketContainer packet = event.getPacket();
            int targetId = packet.getIntegers().read(0);
            Entity target = targetsById.get(targetId);
            if (target != null && glowing.contains(targetId)) {
                event.setPacket(withGlowing(packet, target));
            }
        }
    };
    private volatile boolean started = false;

    public ProtocolLibPacketGlow(BiPredicate<Player, Entity> filter) {
        this.filter = filter;
    }

    @Override
    public synchronized void start() {
        if (started) return;
        started = true;
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
        refreshAll();
    }

    @Override
    public synchronized void stop() {
        if (!started) return;
        started = false;
        ProtocolLibrary.getProtocolManager().removePacketListener(listener);
        viewers.values().forEach(Set::clear);
        refreshAll();
        viewers.clear();
        targetsById.clear();
    }

    @Override
    public void tick() {
        if (!started) return;
        viewers.forEach((viewerId, glowing) -> {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) return;
            targetsById.forEach((targetId, target) -> {
                boolean changed = filter.test(viewer, target) ? glowing.add(targetId) : glowing.remove(targetId);
                if (changed) {
                    refresh(viewerId, target);
                }
            });
        });
    }

    @Override
    public void addViewer(Player viewer) {
        Set<Integer> glowing = ConcurrentHashMap.newKeySet();
        if (viewers.putIfAbsent(viewer.getUniqueId(), glowing) != null || !started) return;
        targetsById.forEach((targetId, target) -> {
            if (filter.test(viewer, target)) {
                glowing.add(targetId);
                refresh(viewer.getUniqueId(), target);
            }
        });
    }

    @Override
    public void removeViewer(Player viewer) {
        Set<Integer> glowing = viewers.remove(viewer.getUniqueId());
        if (glowing == null || !started) return;
        targetsById.forEach((targetId, target) -> {
            if (glowing.contains(targetId)) {
                refresh(viewer.getUniqueId(), target);
            }
        });
    }

    @Override
    public void addTarget(Entity target) {
        if (targetsById.put(target.getEntityId(), target) != null || !started) return;
        viewers.forEach((viewerId, glowing) -> {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && filter.test(viewer, target)) {
                glowing.add(target.getEntityId());
                refresh(viewerId, target);
            }
        });
    }

    @Override
    public void removeTarget(Entity target) {
        if (targetsById.remove(target.getEntityId()) == null || !started) return;
        viewers.forEach((viewerId, glowing) -> {
            if (glowing.remove(target.getEntityId())) {
                refresh(viewerId, target);
            }
        });
    }

    private static PacketContainer withGlowing(PacketContainer packet, Entity target) {
        List<WrappedDataValue> values = packet.getDataValueCollectionModifier().read(0);
        List<WrappedDataValue> glowing = new ArrayList<>(values == null ? 1 : values.size() + 1);
        boolean hasFlags = false;
        if (values != null) {
            for (WrappedDataValue value : values) {
                if (value.getIndex() == SHARED_FLAGS_INDEX && value.getValue() instanceof Byte flags) {
                    glowing.add(flagsValue((byte) (flags | GLOWING_FLAG)));
                    hasFlags = true;
                } else {
                    glowing.add(value);
                }
            }
        }

        if (!hasFlags) {
            glowing.add(flagsValue((byte) (currentFlags(target) | GLOWING_FLAG)));
        }

        PacketContainer copy = packet.shallowClone();
        copy.getDataValueCollectionModifier().write(0, glowing);
        return copy;
    }

    private void refreshAll() {
        for (UUID viewerId : viewers.keySet()) {
            targetsById.values().forEach(target -> refresh(viewerId, target));
        }
    }

    private static void refresh(UUID viewerId, Entity target) {
        Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer == null || viewer.equals(target)) return;

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());
        packet.getDataValueCollectionModifier().write(0, List.of(flagsValue(currentFlags(target))));
        try {
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception ex) {
            Logging.errorLog("Failed to refresh glow of " + target.getName() + " for " + viewer.getName(), ex);
        }
    }

    private static byte currentFlags(Entity target) {
        Byte flags = WrappedDataWatcher.getEntityWatcher(target).getByte(SHARED_FLAGS_INDEX);
        return flags == null ? 0 : flags;
    }

    private static WrappedDataValue flagsValue(byte flags) {
        return new WrappedDataValue(SHARED_FLAGS_INDEX, BYTE_SERIALIZER, flags);
    }
}
