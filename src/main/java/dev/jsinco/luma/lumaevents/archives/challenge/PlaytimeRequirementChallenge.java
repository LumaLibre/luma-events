package dev.jsinco.luma.lumaevents.archives.challenge;

import dev.lumas.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.archives.Challenge;
import dev.jsinco.luma.lumaevents.archives.ChallengeType;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaytimeRequirementChallenge extends Challenge {

    public static final int STAGES = 1;

    public PlaytimeRequirementChallenge(int currentStage) {
        super(ChallengeType.PLAYTIME, STAGES);
        this.currentStage = currentStage;
    }

    public PlaytimeRequirementChallenge(int currentStage, boolean assigned) {
        super(ChallengeType.PLAYTIME, STAGES);
        this.currentStage = currentStage;
        this.assigned = assigned;
    }

    @Override
    public void passiveImpl(EventPlayer who) {
        Player player = who.getPlayer();
        if (player == null || this.currentStage >= 1) return;

        // should have chosen a better anti-afk plugin -- dev note:
        // JetsAntiAFK doesn't have an actual API so this is the best I can do is this unless It's direct database calls
        int playtimeDays = Util.getInt(PlaceholderAPI.setPlaceholders(player, "%jetsantiafkpro_timeplayed_days%"), -1);
        if (playtimeDays < 0) {
            Logging.errorLog("Bad playtime days for " + player.getName() + ": " + playtimeDays);
        }

        if (playtimeDays >= 1) {
            this.addStage(1);
        }
    }
}
