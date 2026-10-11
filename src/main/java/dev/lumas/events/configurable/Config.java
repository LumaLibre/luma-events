package dev.lumas.events.configurable;

import dev.lumas.events.games.constants.MinigameConstant;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@Getter
@Setter
@ConfigSerializable
public class Config {

    @Comment("Enable or disable automatic minigames")
    private boolean automaticMinigames = false;

    @Comment("Enable or disable job token payouts")
    private boolean jobTokenPayouts = false;

    @Comment("Enable or disable town token invite payouts")
    private boolean townTokenInvitePayouts = false;

    private boolean charmClaimable = false;

    @Comment("Automatic minigame cooldown in milliseconds")
    private long automaticMinigameCooldown = 7200000L;

    @Comment("Token multiplier")
    private double tokenMultiplier = 1.0;

    @Comment("Location for /event")
    private Location eventSpawnLocation = null;

    @Comment("Game drop-off location for minigames")
    private Location gameDropOffLocation = null;

    @Comment("Enabled minigames for automatic selection")
    private List<MinigameConstant> enabledAutomaticMinigames = List.of(
            MinigameConstant.BOMBERMAN,
            MinigameConstant.INCURSION,
            MinigameConstant.SOCCER
    );

    @Comment("Commands allowed while participating in a game")
    private List<String> commandWhitelist = List.of(
            "g", "l", "lc", "pc", "p", "sc", "msg", "r", "w", "whisper", "tell", "partychat", "staffchat",
            "event"
    );
}
