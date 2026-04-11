package dev.lumas.events.shop;

import dev.lumas.events.EventMain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class ShopLang {

    static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("purchase_success", "<gradient:#F6C65B:#F3AA4C:#E884C9>Purchase successful!</gradient> <#A7957B>You now have <gradient:#FFD36B:#F3AA4C>{souls} souls</gradient><#A7957B>."),
            Map.entry("limit_reached_single", "<gradient:#8E6A93:#6B496B>Already purchased</gradient>"),
            Map.entry("limit_reached_multi", "<gradient:#8E6A93:#6B496B>Already purchased <gradient:#B08ACF:#CA51CB>{limit} times</gradient>"),
            Map.entry("out_of_stock", "<gradient:#E56A91:#CA51CB>This item is out of stock!</gradient>"),
            Map.entry("not_enough_souls", "<gradient:#E56A91:#CA51CB>Not enough souls!</gradient> <#A7957B>You need <gradient:#FFD36B:#F3AA4C>{price} souls</gradient><#A7957B> but only have <gradient:#FFD36B:#F3AA4C>{souls} souls</gradient><#A7957B>."),
            Map.entry("purchase_status_none", "<gradient:#C7B18A:#F3D08A>Click to purchase!</gradient>"),
            Map.entry("purchase_status_partial", "<gradient:#8FA8D8:#B08ACF>Purchased {count} times</gradient>"),
            Map.entry("purchase_status_done_single", "<gradient:#7A627A:#6B496B>Already purchased</gradient>"),
            Map.entry("purchase_status_done_multi", "<gradient:#7A627A:#6B496B>Already purchased {count} times</gradient>")
    );

    private final File langFile;
    private final Logger logger = EventMain.getInstance().getLogger();
    private Map<String, String> messages = new HashMap<>(DEFAULTS);

    ShopLang(File shopDir) {
        this.langFile = new File(shopDir, "shop.lang");
    }

    void load() {
        saveDefault();
        if (!langFile.exists()) return;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8))) {
            Map<String, String> loaded = new HashMap<>(DEFAULTS);
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int idx = trimmed.indexOf('=');
                if (idx < 0) continue;
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1);
                loaded.put(key, value);
            }
            this.messages = loaded;
        } catch (Exception e) {
            logger.warning("[Shop] Failed to load shop.lang: " + e.getMessage());
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, DEFAULTS.getOrDefault(key, key));
    }

    private void saveDefault() {
        if (langFile.exists()) return;
        try (InputStream in = EventMain.getInstance().getResource("shop/shop.lang")) {
            if (in == null) {
                logger.warning("[Shop] Default shop.lang not found in plugin resources.");
                return;
            }
            try (FileWriter fw = new FileWriter(langFile, StandardCharsets.UTF_8)) {
                fw.write(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.warning("[Shop] Failed to write default shop.lang: " + e.getMessage());
        }
    }
}
