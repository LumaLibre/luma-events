package dev.lumas.events.jobtokens;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import com.gmail.nossr50.api.AbilityAPI;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.items.TokenExchanging;
import dev.lumas.events.utility.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Random;

@AutoRegister(RegisterType.LISTENER)
public class JobsListener implements Listener {

    private static final Random RANDOM = new Random(); // intentionally new random seed for this

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJobsPrePayment(JobsPrePaymentEvent event) {
        String jobName = event.getJob().getName();
        EventJobValue jobConstant = Util.getEnumFromString(EventJobValue.class, jobName);
        if (jobConstant == null) {
            return;
        }

        Config cfg = EventMain.getOkaeriConfig();

        int bound = jobName.equalsIgnoreCase("Lumberjack") && isInTreeFeller(event.getPlayer().getPlayer())
                ? jobConstant.getBound() * 3
                : jobConstant.getBound();

        if (cfg.isJobTokenPayouts() && RANDOM.nextInt(bound) < jobConstant.getChance()) {
            Player player = event.getPlayer().getPlayer();
            if (player == null) {
                return;
            }
            TokenExchanging.give(player, TokenExchanging.TokenType.CANDIED_APPLE, RANDOM.nextInt(1, 3), jobName);
        }
    }

    private boolean isInTreeFeller(Player player) {
        return EventMain.isWithMcMMO() && AbilityAPI.treeFellerEnabled(player);
    }
}
