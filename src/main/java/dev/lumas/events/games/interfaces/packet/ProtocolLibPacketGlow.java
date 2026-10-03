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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolLibPacketGlow implements PacketGlow {

    private static final int SHARED_FLAGS_INDEX = 0;
    private static final byte GLOWING_FLAG = 0x40;
    private static final WrappedDataWatcher.Serializer BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Entity> targetsById = new ConcurrentHashMap<>();
    private final PacketAdapter listener = new PacketAdapter(EventMain.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
        @Override
        public void onPacketSending(PacketEvent event) {
            if (event.isPlayerTemporary() || !viewers.contains(event.getPlayer().getUniqueId())) {
                return;
            }

            PacketContainer packet = event.getPacket();
            Entity target = targetsById.get(packet.getIntegers().read(0));
            if (target != null) {
                event.setPacket(withGlowing(packet, target));
            }
        }
    };
    private volatile boolean started = false;

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
        refreshAll();
        viewers.clear();
        targetsById.clear();
    }

    @Override
    public void addViewer(Player viewer) {
        if (!viewers.add(viewer.getUniqueId()) || !started) return;
        targetsById.values().forEach(target -> refresh(viewer.getUniqueId(), target));
    }

    @Override
    public void removeViewer(Player viewer) {
        if (!viewers.remove(viewer.getUniqueId()) || !started) return;
        targetsById.values().forEach(target -> refresh(viewer.getUniqueId(), target));
    }

    @Override
    public void addTarget(Entity target) {
        if (targetsById.put(target.getEntityId(), target) != null || !started) return;
        viewers.forEach(viewerId -> refresh(viewerId, target));
    }

    @Override
    public void removeTarget(Entity target) {
        if (targetsById.remove(target.getEntityId()) == null || !started) return;
        viewers.forEach(viewerId -> refresh(viewerId, target));
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
        for (UUID viewerId : viewers) {
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
