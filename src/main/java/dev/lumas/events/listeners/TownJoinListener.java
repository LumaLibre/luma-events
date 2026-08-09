package dev.lumas.events.listeners;

import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.object.inviteobjects.PlayerJoinTownInvite;
import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.items.TokenExchanging;
import dev.lumas.events.items.TokenSource;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Register(value = Autowire.LISTENER, requires = "Towny")
public final class TownJoinListener implements Listener {

    private static final long MIN_ELIGIBLE_PLAYTIME_TICKS = TimeUnit.MINUTES.toSeconds(10) * 20L;
    private static final long MAX_ELIGIBLE_PLAYTIME_TICKS = TimeUnit.HOURS.toSeconds(3) * 20L;
    private static final long MIN_INVITER_PLAYTIME_TICKS = TimeUnit.HOURS.toSeconds(24) * 20L;
    private static final long INVITER_REWARD_COOLDOWN_NANOS = TimeUnit.MINUTES.toNanos(25);
    private static final int MIN_INVITER_REWARD = 1;
    private static final int MAX_INVITER_REWARD = 15;

    private final Map<UUID, Long> inviterRewardCooldowns = new ConcurrentHashMap<>();

    @EventHandler
    public void onTownJoin(TownAddResidentEvent event) {
        if (!EventMain.getOkaeriConfig().isTownTokenInvitePayouts()) {
            return;
        }

        Player invitedPlayer = event.getResident().getPlayer();
        if (invitedPlayer == null) {
            return;
        }

        PlayerJoinTownInvite invite = event.getResident()
                .getReceivedInvites()
                .stream()
                .filter(PlayerJoinTownInvite.class::isInstance)
                .map(PlayerJoinTownInvite.class::cast)
                .filter(i -> i.getSender().equals(event.getTown()))
                .findFirst()
                .orElse(null);

        if (invite == null) {
            if (event.getTown().isOpen() && event.getMayor() != null) {
                invalidateTokenClaimability(invitedPlayer);
            }
            return;
        }

        EventPlayer invitedEventPlayer = EventPlayerManager.getByUUID(invitedPlayer.getUniqueId());
        long playtimeTicks = invitedPlayer.getStatistic(Statistic.PLAY_ONE_MINUTE);
        if (playtimeTicks >= MAX_ELIGIBLE_PLAYTIME_TICKS || !invitedEventPlayer.claimTownInviteReward()) {
            return;
        }

        // Save the claim before checking the inviter so cooldowns cannot be bypassed by rejoining.
        EventPlayerManager.save(invitedEventPlayer);

        if (playtimeTicks < MIN_ELIGIBLE_PLAYTIME_TICKS) {
            return;
        }

        UUID inviterUuid = invite.getSenderUUID();
        if (inviterUuid == null || inviterUuid.equals(invitedPlayer.getUniqueId())) {
            return;
        }

        Player inviter = Bukkit.getPlayer(inviterUuid);
        if (inviter == null || !inviter.isOnline()) {
            return;
        }

        // Checked before the cooldown so an ineligible inviter does not burn their next window.
        if (inviter.getStatistic(Statistic.PLAY_ONE_MINUTE) < MIN_INVITER_PLAYTIME_TICKS) {
            return;
        }

        if (!tryStartInviterRewardCooldown(inviterUuid)) {
            return;
        }

        TokenSource source = TokenSource.townInvite(event.getTown().getName() + " -> " + invitedPlayer.getName());

        int inviterReward = ThreadLocalRandom.current().nextInt(
                MIN_INVITER_REWARD,
                MAX_INVITER_REWARD + 1
        );
        TokenExchanging.give(
                inviter,
                TokenExchanging.TokenType.SUMMER_DOLLOP,
                inviterReward,
                source
        );
    }

    private boolean tryStartInviterRewardCooldown(UUID inviterUuid) {
        long now = System.nanoTime();
        boolean[] started = {false}; // TODO: use atomic boolean

        this.inviterRewardCooldowns.compute(inviterUuid, (uuid, expiresAt) -> {
            if (expiresAt == null || now - expiresAt >= 0) {
                started[0] = true;
                return now + INVITER_REWARD_COOLDOWN_NANOS;
            }
            return expiresAt;
        });

        return started[0];
    }

    private void invalidateTokenClaimability(Player player) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (eventPlayer.claimTownInviteReward()) {
            EventPlayerManager.save(eventPlayer);
        }
    }
}
