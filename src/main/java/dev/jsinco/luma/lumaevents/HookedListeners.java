package dev.jsinco.luma.lumaevents;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import dev.jsinco.luma.lumaevents.items.CustomItemsManager;

import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class HookedListeners implements Listener {

    private static final Random RANDOM = new Random();
    private final ItemStack holidayCandle = CustomItemsManager.holidayCandleItem.createItem().component2();

    @EventHandler
    public void onJobsPrePayment(JobsPrePaymentEvent event) {
        EventJobConstants jobConstant = Util.getEnumFromString(EventJobConstants.class, event.getJob().getName().toUpperCase());
        if (jobConstant == null) {
            return;
        }

        if (RANDOM.nextInt(jobConstant.getBound()) < jobConstant.getChance()) {
            Player player = event.getPlayer().getPlayer();
            if (player == null) {
                return;
            }
            Util.giveItem(player, holidayCandle);
            Util.sendMsg(player, "You have been given <gold>1x <b><#F14452>H<#E95257>o<#E2605C>l<#DA6E61>i<#D27D66>d<#CB8B6B>a<#C39970>y <#B3B57B>C<#ACC380>a<#A4D285>n<#9CE08A>d<#95EE8F>l<#8DFC94>e");
        }
    }


}
