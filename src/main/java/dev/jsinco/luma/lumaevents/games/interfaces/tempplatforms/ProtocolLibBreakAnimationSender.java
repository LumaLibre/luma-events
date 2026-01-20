package dev.jsinco.luma.lumaevents.games.interfaces.tempplatforms;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.entity.Player;
import org.checkerframework.common.value.qual.IntRange;

public final class ProtocolLibBreakAnimationSender implements BreakAnimationSender {

    @Override
    public void sendBreakAnimation(Player viewer, int breakerId, int x, int y, int z, @IntRange(from = -1, to = 9) int stage) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        packet.getIntegers().write(0, breakerId);
        packet.getBlockPositionModifier().write(0, new BlockPosition(x, y, z));
        packet.getIntegers().write(1, stage);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        } catch (Exception ignored) {}
    }
}
