package dev.jsinco.luma.lumaevents.games.interfaces.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import dev.lumas.lumacore.utility.Logging;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

public final class ProtocolLibBreakAnimationSender implements BreakAnimationSender {

    @Override
    public void sendBreakAnimation(Player viewer, int breakerId, int x, int y, int z, @Range(from = -1, to = 9) int stage) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        packet.getIntegers().write(0, breakerId);
        packet.getBlockPositionModifier().write(0, new BlockPosition(x, y, z));
        packet.getIntegers().write(1, stage);
        try {
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception ex) {
            Logging.errorLog("Failed to send block break animation packet to player " + viewer.getName(), ex);
        }
    }
}
