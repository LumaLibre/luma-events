package dev.jsinco.luma.lumaevents.explorer.gui;

import dev.jsinco.luma.lumacore.LumaCore;
import dev.jsinco.luma.lumacore.manager.guis.AbstractGui;
import dev.jsinco.luma.lumacore.manager.guis.GuiItem;
import dev.jsinco.luma.lumaevents.explorer.ActiveExplorerMile;
import dev.jsinco.luma.lumaevents.explorer.ExplorerMile;
import dev.jsinco.luma.lumaevents.explorer.ExplorerMileLevelSnapshot;
import dev.jsinco.luma.lumaevents.explorer.constants.ExplorerMiles;
import dev.jsinco.luma.lumaevents.explorer.events.IAItemStacksListener;
import dev.jsinco.luma.lumaevents.explorer.events.IAItemStacksListener.CustomStackNameSpace;
import dev.jsinco.luma.lumaevents.obj.DialogueText;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import dev.jsinco.luma.lumaevents.utility.gui.GuiUtil;
import dev.jsinco.luma.lumaevents.utility.gui.GuiUtil.GuiArrow;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaevents.utility.gui.PaginatedGui;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExplorerMilesGui extends AbstractGui {

    //private static final String EXPLORER_POSTCARD_KEY = "explorer_mile";

    private final Inventory baseInv = GuiUtil.getBaseInv(this, 54, "Explorer Miles");
    private PaginatedGui paginatedGui;
    private EventPlayer eventPlayer;
    private DialogueText dialogueText;

    private final GuiItem unlockMore = new GuiItem(
            53,
            Util.editMeta(Objects.requireNonNull(IAItemStacksListener.getCachedIAStack(CustomStackNameSpace.POSTCARD_PILE), "ItemsAdder is missing custom item POSTCAR_PILE"), (meta -> {
                meta.displayName(Util.color("<gold><b>Unlock more explorer miles"));
                //int unlocked = -1; //eventPlayer.getUnlockedExplorerMiles().size();
                //int total = ExplorerMiles.values().size();
                meta.lore(Util.color(
                        NamedTextColor.WHITE,
                        //"You have unlocked <gold>" + unlocked + "/" + total + " explorer miles.",
                        "Click to unlock more",
                        "explorer miles for <aqua>40 Carrots</aqua> each."
                ));
            })),
            (event, guiItem) -> {
                Player bukkitPlayer = (Player) event.getWhoClicked();
                bukkitPlayer.closeInventory();
                int amount = TokenExchanging.getAmount(bukkitPlayer, TokenExchanging.TokenType.CARROT);
                if (amount < 40) {
                    dialogueText.queueText(
                            "Hey hey,",
                            "we don't do handouts around here.",
                            "If you want to pay to unlock more explorer miles,",
                            "we'll need <aqua>40 Carrots</aqua> up front first..."
                    );
                    dialogueText.sendQueuedText(() -> this.open(bukkitPlayer));
                    return;
                }

                if (!TokenExchanging.take(bukkitPlayer, TokenExchanging.TokenType.CARROT, 40)) {
                    return;
                }

                Collection<ExplorerMile<?>> explorerMiles = ExplorerMiles.values();
                for (ExplorerMile<?> explorerMile : explorerMiles) {
                    if (!eventPlayer.hasUnlockedExplorerMile(explorerMile)) {
                        bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 1f);
                        eventPlayer.unlockExplorerMile(explorerMile);
                        this.refreshGui();
                        dialogueText.queueText(
                                "You unlocked a new Explorer Mile! (" + explorerMile.getTitle() + ")",
                                "...",
                                "Congratulations!"
                        );
                        dialogueText.sendQueuedText(() -> this.open(bukkitPlayer));
                        return;
                    }
                }
            }
    );

    private final GuiItem previousPage = new GuiItem(
            48,
            GuiUtil.guiArrow(GuiArrow.LEFT),
            (event, guiItem) -> {
                Inventory prev = this.paginatedGui.getPrevious(event.getInventory());
                if (prev == null) {
                    return;
                }
                event.getWhoClicked().openInventory(prev);
            }
    );

    private final GuiItem nextPage = new GuiItem(
            50,
            GuiUtil.guiArrow(GuiArrow.RIGHT),
            (event, guiItem) -> {
                Inventory next = this.paginatedGui.getNext(event.getInventory());
                if (next == null) {
                    return;
                }
                event.getWhoClicked().openInventory(next);
            }
    );

    public ExplorerMilesGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.dialogueText = new DialogueText(eventPlayer, NamedTextColor.YELLOW, 0.4f);
        this.autoRegister();
        this.refreshGui();
    }

    private void refreshGui() {
        List<ItemStack> items = new ArrayList<>();

        for (ExplorerMile<?> explorerMile : ExplorerMiles.values()) {
            boolean hasUnlocked = eventPlayer.hasUnlockedExplorerMile(explorerMile);

            CustomStackNameSpace[] type = hasUnlocked ? CustomStackNameSpace.POSTCARDS : CustomStackNameSpace.POSTCARDS_NO_ART;
            ItemStack explorerMilePostCard = IAItemStacksListener.getCachedIAStack(Util.getRandom(type));

            if (explorerMilePostCard == null) {
                continue;
            }

            explorerMilePostCard.editMeta(meta -> {
                String displayName;
                List<String> lore;
                if (hasUnlocked) {
                    ActiveExplorerMile activeExplorerMile = eventPlayer.getActiveExplorerMile(explorerMile);
                    ExplorerMileLevelSnapshot snapshot = activeExplorerMile.getUnchangeableLevelSnapshot();
                    displayName = "<gold><b>" + explorerMile.getTitle();
                    lore = this.createExplorerMileLore(activeExplorerMile.getMile().getDesc(), snapshot);
                    if (snapshot.isCompleted()) meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                } else {
                    displayName ="<white><b>???";
                    lore = List.of(
                            "You haven't unlocked this",
                            "explorer mile yet.",
                            "",
                            "<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st></#EEE1D5>",
                            "<b><red>❗</red></b> You don't know this mile. :<",
                            "<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st></#EEE1D5>"
                    );
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.displayName(Util.color(displayName));
                meta.lore(Util.color(lore, NamedTextColor.WHITE));
            });
            items.add(explorerMilePostCard);
        }

        this.paginatedGui = new PaginatedGui.Builder()
                .name("Explorer Miles")
                .base(baseInv)
                .items(items)
                .startEndSlots(20, 34)
                .ignoredSlots(25, 26, 27, 28)
                .build();
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {

    }

    @Override
    public @NotNull Inventory getInventory() {
        return baseInv;
    }

    @Override
    public void open(HumanEntity humanEntity) {
        Inventory first = this.paginatedGui.getFirst();
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(LumaCore.getInstance(), () -> humanEntity.openInventory(first));
        } else {
            humanEntity.openInventory(first);
        }
    }


    private List<String> createExplorerMileLore(String desc, ExplorerMileLevelSnapshot snapshot) {
        List<String> lore = new ArrayList<>();

        lore.addAll(GuiUtil.formatLore(desc.split("\n")));
        lore.add("");
        lore.add("<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st></#EEE1D5>");
        lore.add("Stars<gray>:</gray> " + this.createLevelProgressBar(snapshot));
        lore.add("Progress(" + snapshot.getCurrentQuantity() + "/" + snapshot.getMaxQuantityForCurrentLevel() +")<gray>:</gray> " + this.createQuantityProgressBar(snapshot));
        lore.add("<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st></#EEE1D5>");
        return lore;
    }

    private String createLevelProgressBar(ExplorerMileLevelSnapshot snapshot) {
        String string = createProgressBar("<gold>★→", "<white>★→", snapshot.getCurrentLevel(), snapshot.getMaxLevels(), snapshot.getMaxLevels(), "\uD83C\uDF1F", 2);
        //String append = (snapshot.isCompleted() ? "<gold>" : "<white>") + "\uD83C\uDF1F";
        return string;// + append;
    }
    

    private String createQuantityProgressBar(ExplorerMileLevelSnapshot snapshot) {
        int x = !snapshot.isCompleted() ? snapshot.getCurrentQuantity() : 1;
        int y = !snapshot.isCompleted() ? snapshot.getMaxQuantityForCurrentLevel() : 1;
        return createProgressBar("<green>|", "<white>|", x, y, 25);
    }

    private String createProgressBar(String completed, String remaining, int progress, int total, int amount) {
        return createProgressBar(completed, remaining, progress, total, amount, null, -1);
    }

    private String createProgressBar(String completed, String remaining, int progress, int total, int amount, String lastChar, int backspace) {
        int completedRounded = (int) (((double) progress) / total * amount);
        String completedStr = completed.repeat(completedRounded);
        int count = amount - completedRounded;
        if (count < 0) {
            count = 0;
        }
        String remainingStr = remaining.repeat(count);
        if (lastChar != null) {
            if (!remainingStr.isEmpty()) {
                remainingStr = remainingStr.substring(0, remainingStr.length() - backspace) + lastChar;
            } else {
                completedStr = completedStr.substring(0, completedStr.length() - backspace) + lastChar;
            }
        }
        return completedStr + remainingStr;
    }

}
