package dev.lumas.events.listeners;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import com.gamingmesh.jobs.container.ActionType;
import com.gmail.nossr50.api.AbilityAPI;
import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.items.TokenExchanging;
import dev.lumas.events.items.TokenSource;
import dev.lumas.events.utility.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Random;

@Register(value = Autowire.LISTENER, requires = "Jobs")
public class JobsListener implements Listener {

    private static final String LUMBERJACK = "Lumberjack";

    private static final Random RANDOM = new Random(); // intentionally new random seed for this

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJobsPrePayment(JobsPrePaymentEvent event) {
        String jobName = event.getJob().getName();
        EventJobValue jobConstant = Util.getEnumFromString(EventJobValue.class, jobName);
        if (jobConstant == null || event.getActionInfo().getType() == ActionType.TNTBREAK) {
            return;
        }

        Config cfg = EventMain.getOkaeriConfig();

        int bound = jobName.equalsIgnoreCase(LUMBERJACK) && isInTreeFeller(event.getPlayer().getPlayer())
                ? jobConstant.getBound() * 3
                : jobConstant.getBound();

        if (cfg.isJobTokenPayouts() && RANDOM.nextInt(bound) < jobConstant.getChance()) {
            Player player = event.getPlayer().getPlayer();
            if (player == null) {
                return;
            }
            TokenExchanging.give(player, TokenExchanging.TokenType.SUMMER_DOLLOP, RANDOM.nextInt(1, 3), TokenSource.job(jobName));
        }
    }

    private boolean isInTreeFeller(Player player) {
        return EventMain.isWithMcMMO() && AbilityAPI.treeFellerEnabled(player);
    }
}
