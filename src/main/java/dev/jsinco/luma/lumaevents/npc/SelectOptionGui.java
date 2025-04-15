package dev.jsinco.luma.lumaevents.npc;

import dev.jsinco.luma.lumacore.manager.guis.AbstractGui;
import dev.jsinco.luma.lumacore.manager.guis.GuiItem;
import dev.jsinco.luma.lumaevents.explorer.gui.ExplorerMilesGui;
import dev.jsinco.luma.lumaevents.obj.DialogueText;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.gui.GuiUtil;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class SelectOptionGui extends AbstractGui {

    @Getter
    private final Inventory inventory = GuiUtil.getBaseInv(this, 45, "Select an option");
    private EventPlayer eventPlayer;
    private DialogueText dialogueText;

    private final GuiItem stalkMarket = new GuiItem(
            11,
            GuiUtil.item(Material.CARROT, true, "<b>What's the price of carrots?"),
            (event, a) -> {
                dialogueText.queueText("The current price of carrots is $2.50.");
                dialogueText.queueText("How else can I help?");
                HumanEntity h = event.getWhoClicked();
                h.closeInventory();
                dialogueText.sendQueuedText(NamedTextColor.GREEN, null, () -> {
                    this.open(h);
                });
            }
    );

    private final GuiItem barter = new GuiItem(
            12,
            GuiUtil.item(Material.GOLDEN_CARROT, true, "<b>Trade carrots for candies"),
            (event, a) -> {
                dialogueText.queueText("Oooh, want to trade some carrots for candies?");
                dialogueText.queueText("I'm currently buying carrots for X candies.");
                dialogueText.queueText("So,");
                dialogueText.queueText("how many carrots do you want to trade?");
                HumanEntity h = event.getWhoClicked();
                h.closeInventory();
                dialogueText.sendQueuedText(NamedTextColor.GREEN, null, () -> {
                    this.open(h);
                });
            }
    );

    public final GuiItem explorerMiles = new GuiItem(
            13,
            GuiUtil.item(Material.PAPER, true, "<b>Explorer miles"),
            (event, guiItem) -> {
                HumanEntity h = event.getWhoClicked();
                new ExplorerMilesGui(eventPlayer).open(h);
            }
    );


    public SelectOptionGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.dialogueText = new DialogueText(eventPlayer);
        this.autoRegister();
    }

    @Override
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInventoryClose(@NotNull InventoryCloseEvent inventoryCloseEvent) {

    }

}
