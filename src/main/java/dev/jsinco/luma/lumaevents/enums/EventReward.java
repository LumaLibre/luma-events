package dev.jsinco.luma.lumaevents.enums;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Getter
public enum EventReward {

    // Tags
    HEARTBREAKERS_TAG(EventTeamType.HEARTBREAKERS, "lp user %player% permission set eternaltags.tag.valentide_heartbreaker"),
    ROSETHORN_TAG(EventTeamType.ROSETHORN, "lp user %player% permission set eternaltags.tag.valentide_rosethorn"),
    SWEETHEARTS_TAG(EventTeamType.SWEETHEARTS, "lp user %player% permission set eternaltags.tag.valentide_sweetheart"),
    HEARTBREAKERS_SET_TAG(EventTeamType.HEARTBREAKERS, "tags set valentide_heartbreaker %player%"),
    ROSETHORN_SET_TAG(EventTeamType.ROSETHORN, "tags set valentide_rosethorn %player%"),
    SWEETHEARTS_SET_TAG(EventTeamType.SWEETHEARTS, "tags set valentide_sweetheart %player%"),
    // Tokens
    TOKENS_24(EventTeamType.HEARTBREAKERS, "lumaitems give valentide_stamp %player% 24"),
    TOKENS_18(EventTeamType.ROSETHORN, "lumaitems give valentide_stamp %player% 18"),
    TOKENS_12(EventTeamType.SWEETHEARTS, "lumaitems give valentide_stamp %player% 12"),

    // TODO: Rewards #3
    ;

    @Nullable
    private final EventTeamType teamType;
    private final String command;


    EventReward(@Nullable EventTeamType teamType, String command) {
        this.teamType = teamType;
        this.command = command;
    }



    public boolean claim(EventPlayer eventPlayer) {
        EventTeamType playerTeam = eventPlayer.getTeamType();
        if (playerTeam == null) {
            return false;
        }
        if (this.teamType != null && this.teamType != playerTeam) {
            return false;
        }

        Player bukkitPlayer = eventPlayer.getPlayer();
        if (bukkitPlayer == null) {
            return false;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), this.command.replace("%player%", bukkitPlayer.getName()));
        return true;
    }
}
