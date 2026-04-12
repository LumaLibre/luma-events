package dev.lumas.events.hooks;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.Service;
import me.SuperRonanCraft.BetterRTP.player.rtp.RTP_TYPE;
import me.SuperRonanCraft.BetterRTP.references.helpers.HelperRTP;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@NullMarked
@Register(value = Autowire.SERVICE, requires = "BetterRTP")
public class BetterRTPService implements Service {

    @Nullable
    private static BetterRTPService instance;

    @Override
    public void register() {
        instance = this;
    }

    @Override
    public void unregister() {
        instance = null;
    }

    public static Optional<BetterRTPService> getInstance() {
        return Optional.ofNullable(instance);
    }

    public void rtp(Player player, World world) {
        HelperRTP.tp(player, Bukkit.getConsoleSender(), world, null, RTP_TYPE.ADDON, true, true);
    }
}
