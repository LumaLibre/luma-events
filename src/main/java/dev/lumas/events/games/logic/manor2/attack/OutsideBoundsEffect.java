package dev.lumas.events.games.logic.manor2.attack;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import dev.lumas.events.games.logic.manor2.roles.Spectator;
import dev.lumas.events.games.logic.manor2.roles.predicate.IsRolePredicate;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.effects.SoundDefinition;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public class OutsideBoundsEffect {

    private static final Title OUT_OF_BOUNDS = Util.title(
        "<red>Out of bounds",
        "<gradient:gray:white>Get back until it finds you...",
        Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(2), Duration.ofSeconds(0))
    );
    private static final String[] OUT_OF_BOUNDS_DEATH = {
        "<red><player></red> got discovered by a foreign entity..."
    };
    private static final int MAX_OUT_OF_BOUNDS_TICK = 20 * 5;

    private final Manor2 manor2;
    private final Map<UUID, Integer> outOfBounds = new ConcurrentHashMap<>();

    public OutsideBoundsEffect(Manor2 manor2) {
        this.manor2 = manor2;
    }

    public void tick() {
        manor2.getRoleMap().values()
            .stream()
            .filter(new IsRolePredicate(Spectator.class).negate())
            .forEach(this::outOfBoundsEffect);
    }

    private void outOfBoundsEffect(Manor2Role role) {
        role.operatePlayer(player -> {
            if (!isOutOfBounds(player)) {
                outOfBounds.remove(player.getUniqueId());
                return;
            }
            int outOfBoundsTicks = outOfBounds.merge(player.getUniqueId(), 1, Integer::sum);
            if (outOfBoundsTicks > MAX_OUT_OF_BOUNDS_TICK) {
                role.die();
                Util.sendMsg(manor2.getAudience(), Util.getRandom(OUT_OF_BOUNDS_DEATH), Placeholder.component("player", player.displayName()));
                return;
            }
            if (outOfBoundsTicks % 20 == 19) {
                new SoundDefinition(Sound.BLOCK_BELL_USE, 1F, 0.1F, null)
                    .playFor(player);
            }
            manor2.getDefinition()
                .getOutOfBoundsParticles()
                .forEach(particleDefinition -> particleDefinition.showAt(player));
            player.showTitle(OUT_OF_BOUNDS);
        });
    }

    private boolean isOutOfBounds(Player player) {
        return !manor2.getDefinition().getRegion().toWorldTiedBoundingBox()
            .contains(player);
    }
}
