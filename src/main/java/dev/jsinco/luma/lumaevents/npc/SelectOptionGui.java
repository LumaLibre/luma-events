package dev.jsinco.luma.lumaevents.npc;

/*
import dev.jsinco.luma.lumacore.manager.guis.AbstractGui;
import dev.jsinco.luma.lumacore.manager.guis.GuiItem;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.explorer.events.IAItemStacksListener.CustomStackNameSpace;
import dev.jsinco.luma.lumaevents.explorer.gui.ExplorerMilesGui;
import dev.jsinco.luma.lumaevents.npc.constants.StalkMarketDays;
import dev.jsinco.luma.lumaevents.npc.constants.TutorialSection;
import dev.jsinco.luma.lumaevents.npc.events.ChatPromptInputListener.ChatInputCallback;
import dev.jsinco.luma.lumaevents.npc.events.ChatPromptInputListener.ChatInputCallback.ChatInputCallbackHandler;
import dev.jsinco.luma.lumaevents.npc.obj.StalkMarketDay;
import dev.jsinco.luma.lumaevents.obj.DialogueText;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging.TokenType;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaevents.utility.gui.GuiUtil;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
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
            21,
            GuiUtil.item(Material.CARROT, true, "<b><yellow>What's the price of Baskets?"),
            (event, a) -> {
                StalkMarketDay stalkMarketDay = StalkMarketDays.forToday();
                TutorialSection tutorialSection = TutorialSection.STALK_MARKET;
                HumanEntity h = event.getWhoClicked();
                h.closeInventory();

                Runnable runnable = () -> {
                    dialogueText.queueText("Today I'm selling baskets for <aqua>" + stalkMarketDay.getPrice() + " Carrots<green> each!");
                    dialogueText.queueText("Let me know if you'd like to buy some baskets! ♥(ˆ⌣ˆԅ)");
                    dialogueText.sendQueuedText(() -> {
                        this.open(h);
                    });
                };

                if (!eventPlayer.hasCompletedTutorialSection(tutorialSection)) {
                    tutorialSection.completeTutorial(eventPlayer, dialogueText, runnable);
                } else {
                    runnable.run();
                }
            }
    );

    private final GuiItem barter = new GuiItem(
            22,
            GuiUtil.item(Material.GOLDEN_CARROT, true, "<b><light_purple>Let's trade!"),
            (event, a) -> {
                StalkMarketDay stalkMarket = StalkMarketDays.forToday();
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();

                dialogueText.queueText("Oooh, want to trade some carrots for baskets?");
                dialogueText.queueText("Tell ya what, I'll give you <aqua>1 Basket <green>for <aqua>" + stalkMarket.getPrice() + " Carrots<green>.");
                dialogueText.queueText("So,");
                dialogueText.queueText("how many baskets do you want to buy?");


                ChatInputCallbackHandler handler = (input) -> {
                    int amount = input.equalsIgnoreCase("max")
                            ? TokenExchanging.getAmount(player, TokenType.CARROT)
                            : (Util.getInt(input, -1) * stalkMarket.getPrice()); // ex: 1 basket = 4 carrots
                    if (amount < 1) {
                        dialogueText.queueText("Oh, so you don't want to trade?");
                        dialogueText.queueText("No worries!");
                    } else {
                        if (stalkMarket.trade(player, amount)){
                            dialogueText.queueText("Pleasure doing business with ya!");
                        } else {
                            dialogueText.queueText("Oh, I don't think you have enough carrots for that...");
                            dialogueText.queueText("Let me know if you'd like to trade a smaller amount!");
                        }
                    }
                    dialogueText.sendQueuedText(() -> {
                        this.open(player);
                    });
                };

                dialogueText.sendQueuedText(() -> {
                    ChatInputCallback.of(
                            player,
                            Util.title("<red>Input", "Input a value for this NPC"),
                            "In chat, type how many <aqua>baskets</aqua> you would like to buy from Anais. Type <aqua>'cancel'</aqua> to return, or <aqua>'max'</aqua> to buy as many as possible.",
                            handler
                    );
                });
            }
    );

    private final GuiItem explorerMiles = new GuiItem(
            23,
            GuiUtil.item(CustomStackNameSpace.POSTCARD_CITY_NO_ART, true, "<b>Explorer Miles"),
            (event, guiItem) -> {
                HumanEntity h = event.getWhoClicked();
                h.closeInventory();

                ExplorerMilesGui gui = new ExplorerMilesGui(eventPlayer);
                if (!eventPlayer.hasCompletedTutorialSection(TutorialSection.EXPLORER_MILES)) {
                    TutorialSection.EXPLORER_MILES.completeTutorial(eventPlayer, dialogueText, () -> {
                        gui.open(h);
                    });
                } else {
                    dialogueText.queueText("Here ya go!");
                    dialogueText.sendQueuedText(() -> gui.open(h));
                }
            }
    );

    private final GuiItem exchangeOldBaskets = new GuiItem(
            44,
            GuiUtil.playerHead(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmU2ZDhjNjk4OGNkMGQxOTk5NzAzMDZhNGQ3NTY0NmQ5NzczZDcxMGViMjE5MzVkYjc3M2ViMjEyMTY3NjAyYiJ9fX0=",
                    "<red><b>Exchange Broken Baskets",
                    "<gray>Exchange your broken baskets for new ones!"
            ),
            (event, guiItem) -> {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();

                int basketsAmount = TokenExchanging.getAmount(player, TokenType.BASKET);
                if (basketsAmount > 0) {
                    TokenExchanging.take(player, TokenType.BASKET, basketsAmount);
                    Bukkit.getScheduler().runTaskLater(EventMain.getInstance(), () -> {
                        TokenExchanging.give(player, TokenType.BASKET, basketsAmount);
                    }, 1);
                    dialogueText.queueText("I exchanged your broken baskets for new ones!");
                } else {
                    dialogueText.queueText("Hmm", "I don't see any baskets in your inventory...");
                }
                dialogueText.sendQueuedText(() -> this.open(player));
            }
    );


    public SelectOptionGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.dialogueText = new DialogueText(eventPlayer);
        this.dialogueText.setIfAbsentColor(NamedTextColor.GREEN);
        this.autoRegister();
    }

    @Override
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
    }


}
*/