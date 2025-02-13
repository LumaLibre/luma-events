package dev.jsinco.luma.lumaevents.events;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.enums.EventJobValue;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Random;

@AutoRegister(RegisterType.LISTENER)
public class JobsListener implements Listener {

    private static final Random RANDOM = new Random();

    @EventHandler
    public void onJobsPrePayment(JobsPrePaymentEvent event) {
        EventJobValue jobConstant = Util.getEnumFromString(EventJobValue.class, event.getJob().getName().toUpperCase());
        if (jobConstant == null) {
            return;
        }

        Config cfg = EventMain.getOkaeriConfig();
        if (RANDOM.nextInt(jobConstant.getBound()) < jobConstant.getChance() && cfg.isJobTokenPayouts()) {
            Player player = event.getPlayer().getPlayer();
            if (player == null) {
                return;
            }
            Util.giveTokens(player, 1);
        }
    }
}
