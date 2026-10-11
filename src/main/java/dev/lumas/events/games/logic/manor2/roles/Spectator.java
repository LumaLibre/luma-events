package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.light.Visibility;
import dev.lumas.events.games.logic.manor2.roles.predicate.IsRolePredicate;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.glowapi.model.GlowColorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

public class Spectator extends Manor2Role {

    public static final String SPECTATOR_INIT_MESSAGE = "<red>You are now a spectator";
    public static final String SPECTATOR_MESSAGE = "<gray>Spectating...";

    private final Manor2 manor2;
    private final Visibility visibility;

    protected Spectator(Manor2 manor2, EventPlayer eventPlayer) {
        super(eventPlayer);
        this.manor2 = manor2;
        this.visibility = new Visibility(manor2, eventPlayer);
    }

    @Override
    public void tick() {
        this.operatePlayer(player -> {
            visibility.setVanished(new IsRolePredicate(Manor2Role.class));
            player.sendActionBar(Util.color(SPECTATOR_MESSAGE));
        });
    }

    @Override
    public void die() {
        // NO-OP
    }

    @Override
    public void initiatePlay() {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        player.teleportAsync(Util.getRandom(manor2.getDefinition().getHunterStartLocations()));
        GlowColorManager.getInstance().update(player);
        manor2.getPacketGlow().removeTarget(player);
        manor2.getPacketGlow().addViewer(player);
        player.showTitle(Title.title(Component.empty(), Util.color(SPECTATOR_INIT_MESSAGE)));
    }

    @Override
    public void retire(Manor2 manor2) {
        Player player = eventPlayer.getPlayer();
        if (player == null) {
            return;
        }
        super.retire(manor2);
    }
}
