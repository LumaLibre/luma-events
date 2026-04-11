package dev.lumas.events.shop;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumas.events.EventMain;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.lumaitems.api.LumaItemsAPI;
import dev.lumas.lumaitems.model.CustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ShopManager {

    public static final NamespacedKey DISPLAY_ITEM_KEY =
            new NamespacedKey(EventMain.getInstance(), "shop_display");

    private static ShopManager instance;

    private final Logger logger = EventMain.getInstance().getLogger();
    private final File shopDir;
    private final File shopConfigFile;
    private final File shopStateFile;
    private final ShopLang lang;

    private String guiTitle = "<gradient:#954381:#ee78c0:#ec6e95:#cb354e>Shop</gradient>";
    private int guiSize = 54;
    private final List<String> defaultLore = new ArrayList<>();
    private final Map<Integer, ShopEntry> slotMap = new LinkedHashMap<>();
    private final Map<String, Integer> remainingStock = new HashMap<>();
    private final Map<String, Map<UUID, Integer>> purchasers = new HashMap<>();

    private ShopManager() {
        File dataFolder = EventMain.getInstance().getDataFolder();
        this.shopDir = new File(dataFolder, "shop");
        this.shopConfigFile = new File(shopDir, "shop.json");
        this.shopStateFile = new File(shopDir, "shop-state.json");
        this.lang = new ShopLang(shopDir);
    }

    public static ShopManager getInstance() {
        if (instance == null) {
            instance = new ShopManager();
        }
        return instance;
    }

    public ShopLang lang() {
        return lang;
    }

    public void load() {
        shopDir.mkdirs();
        saveDefaultConfig();
        lang.load();
        loadConfig();
        loadState();
    }

    public void reload() {
        slotMap.clear();
        defaultLore.clear();
        remainingStock.clear();
        purchasers.clear();
        lang.load();
        loadConfig();
        loadState();
        logger.info("[Shop] Reloaded.");
    }

    private void saveDefaultConfig() {
        if (shopConfigFile.exists()) return;
        try (InputStream in = EventMain.getInstance().getResource("shop/shop.json")) {
            if (in == null) {
                logger.warning("[Shop] Default shop/shop.json not found in plugin resources.");
                return;
            }
            try (FileWriter fw = new FileWriter(shopConfigFile, StandardCharsets.UTF_8)) {
                fw.write(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.warning("[Shop] Failed to write default shop.json: " + e.getMessage());
        }
    }

    private void loadConfig() {
        if (!shopConfigFile.exists()) {
            logger.warning("[Shop] shop.json not found — shop will be empty.");
            return;
        }
        try (FileReader reader = new FileReader(shopConfigFile, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                logger.warning("[Shop] shop.json root must be a JSON object.");
                return;
            }
            JsonObject obj = root.getAsJsonObject();

            if (obj.has("title")) guiTitle = obj.get("title").getAsString();
            if (obj.has("size")) guiSize = obj.get("size").getAsInt();

            if (obj.has("default_lore") && obj.get("default_lore").isJsonArray()) {
                for (JsonElement line : obj.getAsJsonArray("default_lore")) {
                    defaultLore.add(line.getAsString());
                }
            }

            if (obj.has("slots") && obj.get("slots").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("slots").entrySet()) {
                    if (!entry.getValue().isJsonObject()) continue;
                    ShopEntry shopEntry = parseEntry(entry.getValue().getAsJsonObject());
                    if (shopEntry == null) continue;
                    for (int slot : parseSlotKey(entry.getKey())) {
                        slotMap.put(slot, shopEntry);
                    }
                }
            }

            logger.info("[Shop] Loaded shop.json (" + slotMap.size() + " slot(s)).");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Shop] Failed to load shop.json", e);
        }
    }

    @Nullable
    private ShopEntry parseEntry(JsonObject obj) {
        if (obj.has("luma_item")) {
            String lumaItemId = obj.get("luma_item").getAsString();
            int price = obj.has("price") ? obj.get("price").getAsInt() : 0;
            int globalStock = obj.has("global_stock") ? obj.get("global_stock").getAsInt() : 1;
            int maxPerPlayer = obj.has("max_per_player") ? obj.get("max_per_player").getAsInt() : 1;
            String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : null;
            List<String> lore = new ArrayList<>();
            if (obj.has("lore") && obj.get("lore").isJsonArray()) {
                for (JsonElement line : obj.getAsJsonArray("lore")) {
                    lore.add(line.getAsString());
                }
            }
            return new ShopEntry.Item(lumaItemId, price, globalStock, maxPerPlayer, displayName, lore);
        }

        if (obj.has("material")) {
            String matStr = obj.get("material").getAsString().toUpperCase(Locale.ROOT);
            Material material;
            try {
                material = Material.valueOf(matStr);
            } catch (IllegalArgumentException e) {
                logger.warning("[Shop] Unknown material in decoration: " + matStr);
                return null;
            }
            String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : null;
            boolean hideTooltip = obj.has("hide_tooltip") && obj.get("hide_tooltip").getAsBoolean();
            return new ShopEntry.Decoration(material, displayName, hideTooltip);
        }

        logger.warning("[Shop] Slot entry missing both 'luma_item' and 'material' — skipping.");
        return null;
    }

    private List<Integer> parseSlotKey(String key) {
        List<Integer> slots = new ArrayList<>();
        for (String part : key.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            try {
                slots.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                logger.warning("[Shop] Invalid slot number: '" + s + "'");
            }
        }
        return slots;
    }

    private void loadState() {
        if (shopStateFile.exists()) {
            try (FileReader reader = new FileReader(shopStateFile, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();

                    if (obj.has("remainingStock") && obj.get("remainingStock").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("remainingStock").entrySet()) {
                            remainingStock.put(e.getKey(), e.getValue().getAsInt());
                        }
                    }

                    if (obj.has("purchasers") && obj.get("purchasers").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("purchasers").entrySet()) {
                            Map<UUID, Integer> counts = new HashMap<>();
                            JsonElement val = e.getValue();

                            for (Map.Entry<String, JsonElement> pe : val.getAsJsonObject().entrySet()) {
                                try {
                                    counts.put(UUID.fromString(pe.getKey()), pe.getValue().getAsInt());
                                } catch (IllegalArgumentException ignored) {}
                            }

                            purchasers.put(e.getKey(), counts);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warning("[Shop] Failed to load shop-state.json: " + e.getMessage());
            }
        }
        initNewItemStock();
    }

    private void initNewItemStock() {
        for (ShopEntry entry : slotMap.values()) {
            if (entry instanceof ShopEntry.Item item) {
                remainingStock.putIfAbsent(item.lumaItemId(), item.globalStock());
            }
        }
    }

    private void saveState() {
        JsonObject obj = new JsonObject();

        JsonObject stockObj = new JsonObject();
        remainingStock.forEach(stockObj::addProperty);
        obj.add("remainingStock", stockObj);

        JsonObject purchasersObj = new JsonObject();
        purchasers.forEach((itemId, counts) -> {
            JsonObject countObj = new JsonObject();
            counts.forEach((uuid, count) -> countObj.addProperty(uuid.toString(), count));
            purchasersObj.add(itemId, countObj);
        });
        obj.add("purchasers", purchasersObj);

        try (FileWriter fw = new FileWriter(shopStateFile, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, fw);
        } catch (IOException e) {
            logger.warning("[Shop] Failed to save shop-state.json: " + e.getMessage());
        }
    }

    public void openShop(Player player) {
        Executors.runSync(player, () -> {
            Inventory inv = buildInventory(player.getUniqueId());
            player.openInventory(inv);
        });
    }

    private Inventory buildInventory(UUID viewerUuid) {
        Component title = Util.color(guiTitle).decoration(TextDecoration.ITALIC, false);
        ShopHolder holder = new ShopHolder();
        Inventory inv = Bukkit.createInventory(holder, guiSize, title);
        holder.setInventory(inv);

        for (Map.Entry<Integer, ShopEntry> e : slotMap.entrySet()) {
            int slot = e.getKey();
            if (slot < 0 || slot >= guiSize) continue;
            ItemStack item = buildDisplayItem(e.getValue(), viewerUuid);
            if (item != null) inv.setItem(slot, item);
        }

        return inv;
    }

    public void refreshInventory(Inventory inventory, UUID viewerUuid) {
        for (Map.Entry<Integer, ShopEntry> e : slotMap.entrySet()) {
            int slot = e.getKey();
            if (slot < 0 || slot >= inventory.getSize()) continue;
            ItemStack item = buildDisplayItem(e.getValue(), viewerUuid);
            if (item != null) inventory.setItem(slot, item);
        }
    }

    @Nullable
    private ItemStack buildDisplayItem(ShopEntry entry, UUID viewerUuid) {
        return switch (entry) {
            case ShopEntry.Item item -> buildShopItem(item, viewerUuid);
            case ShopEntry.Decoration deco -> buildDecorationItem(deco);
            default -> throw new IllegalStateException("Unexpected ShopEntry: " + entry);
        };
    }

    @Nullable
    private ItemStack buildShopItem(ShopEntry.Item shopItem, UUID viewerUuid) {
        CustomItem customItem = LumaItemsAPI.getInstance().getCustomItem(shopItem.lumaItemId());
        if (customItem == null) {
            logger.warning("[Shop] LumaItem not found: '" + shopItem.lumaItemId() + "'");
            return null;
        }

        ItemStack base = customItem.createItem().getSecond().clone();
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        if (shopItem.displayName() != null) {
            meta.displayName(Util.color(shopItem.displayName()).decoration(TextDecoration.ITALIC, false));
        }

        int remaining = remainingStock.getOrDefault(shopItem.lumaItemId(), shopItem.globalStock());
        int playerPurchases = viewerUuid != null
                ? purchasers.getOrDefault(shopItem.lumaItemId(), Map.of()).getOrDefault(viewerUuid, 0)
                : 0;
        boolean canBuyMore = shopItem.maxPerPlayer() == 0 || playerPurchases < shopItem.maxPerPlayer();

        List<String> combinedShopLore = new ArrayList<>();
        combinedShopLore.addAll(defaultLore);
        if (!defaultLore.isEmpty() && !shopItem.lore().isEmpty()) {
            combinedShopLore.add(""); // blank separator between default and item-specific lore
        }
        combinedShopLore.addAll(shopItem.lore());

        if (!combinedShopLore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();

            List<Component> existingLore = meta.lore();
            if (existingLore != null && !existingLore.isEmpty()) {
                loreComponents.addAll(existingLore);
                loreComponents.add(Component.empty());
            }

            for (String line : combinedShopLore) {
                String resolved = resolvePlaceholders(line, shopItem, remaining, playerPurchases, canBuyMore);
                loreComponents.add(Util.color(resolved).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(loreComponents);
        }

        meta.getPersistentDataContainer().set(
                DISPLAY_ITEM_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        base.setItemMeta(meta);
        return base;
    }

    private String resolvePlaceholders(String line, ShopEntry.Item shopItem,
                                       int remaining, int playerPurchases, boolean canBuyMore) {
        String purchaseStatus;
        if (playerPurchases == 0) {
            purchaseStatus = lang.get("purchase_status_none");
        } else if (canBuyMore) {
            purchaseStatus = lang.get("purchase_status_partial")
                    .replace("{count}", String.valueOf(playerPurchases));
        } else if (playerPurchases == 1) {
            purchaseStatus = lang.get("purchase_status_done_single");
        } else {
            purchaseStatus = lang.get("purchase_status_done_multi")
                    .replace("{count}", String.valueOf(playerPurchases));
        }

        return line
                .replace("{price}", String.valueOf(shopItem.price()))
                .replace("{stock}", String.valueOf(remaining))
                .replace("{max_stock}", String.valueOf(shopItem.globalStock()))
                .replace("{player_purchases}", String.valueOf(playerPurchases))
                .replace("{max_per_player}", shopItem.maxPerPlayer() == 0 ? "\u221e" : String.valueOf(shopItem.maxPerPlayer()))
                .replace("{purchase_status}", purchaseStatus);
    }

    private ItemStack buildDecorationItem(ShopEntry.Decoration deco) {
        ItemStack item = new ItemStack(deco.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (deco.displayName() != null) {
            meta.displayName(Util.color(deco.displayName()).decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(Component.empty());
        }

        if (deco.hideTooltip()) {
            meta.setHideTooltip(true);
        }

        meta.getPersistentDataContainer().set(
                DISPLAY_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isDisplayItem(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer()
                .has(DISPLAY_ITEM_KEY, PersistentDataType.BYTE);
    }

    public enum PurchaseResult {
        SUCCESS,
        LIMIT_REACHED,
        OUT_OF_STOCK,
        NOT_ENOUGH_SOULS,
        INVALID_SLOT
    }

    public synchronized PurchaseResult tryPurchase(Player player, EventPlayer eventPlayer, int slot) {
        ShopEntry entry = slotMap.get(slot);
        if (!(entry instanceof ShopEntry.Item shopItem)) return PurchaseResult.INVALID_SLOT;

        String itemId = shopItem.lumaItemId();
        UUID uuid = player.getUniqueId();

        int playerCount = purchasers.getOrDefault(itemId, Map.of()).getOrDefault(uuid, 0);
        if (shopItem.maxPerPlayer() > 0 && playerCount >= shopItem.maxPerPlayer()) {
            return PurchaseResult.LIMIT_REACHED;
        }

        int stock = remainingStock.getOrDefault(itemId, shopItem.globalStock());
        if (stock <= 0) return PurchaseResult.OUT_OF_STOCK;

        if (eventPlayer.getSouls() < shopItem.price()) return PurchaseResult.NOT_ENOUGH_SOULS;

        eventPlayer.setSouls(eventPlayer.getSouls() - shopItem.price());
        purchasers.computeIfAbsent(itemId, k -> new HashMap<>()).merge(uuid, 1, Integer::sum);
        remainingStock.put(itemId, stock - 1);

        Executors.runAsync(() -> {
            saveState();
            EventPlayerManager.save(eventPlayer);
        });

        CustomItem customItem = LumaItemsAPI.getInstance().getCustomItem(itemId);
        if (customItem != null) {
            ItemStack reward = customItem.createItem().getSecond();
            Executors.runSync(player, () -> Util.giveItem(player, reward));
        }

        return PurchaseResult.SUCCESS;
    }

    @Nullable
    public ShopEntry getEntry(int slot) {
        return slotMap.get(slot);
    }

    public static final class ShopHolder implements InventoryHolder {
        private Inventory inventory;

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @NotNull @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
