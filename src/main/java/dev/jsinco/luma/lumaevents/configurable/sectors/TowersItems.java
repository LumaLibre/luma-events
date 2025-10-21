package dev.jsinco.luma.lumaevents.configurable.sectors;

import dev.jsinco.luma.lumaevents.configurable.MaterialCount;
import eu.okaeri.configs.OkaeriConfig;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static dev.jsinco.luma.lumaevents.configurable.MaterialCount.of;
import static dev.jsinco.luma.lumaevents.configurable.sectors.TowersItems.Entry.with;

public class TowersItems extends OkaeriConfig {

    private Map<String, List<MaterialCount>> materialPackages = Map.ofEntries(
            with("tnt", of(Material.TNT, 3), of(Material.REDSTONE_TORCH)),
            with("food", of(Material.ENCHANTED_GOLDEN_APPLE), of(Material.COOKED_BEEF, 6), of(Material.CAKE, 3)),
            with("bow", of(Material.BOW), of(Material.ARROW, 3)),
            with("wind_charge", of(Material.WIND_CHARGE, 3)),
            with("end_gear", of(Material.ENDER_PEARL), of(Material.END_CRYSTAL)),
            with("web", of(Material.COBWEB, 3)),
            with("eggs", of(Material.EGG, 5), of(Material.BLUE_EGG, 5), of(Material.BROWN_EGG, 5)),
            with("diamond_sword_kit", of(Material.DIAMOND, 3), of(Material.STICK), of(Material.OAK_LOG)),
            with("happy_ghast", of(Material.HAPPY_GHAST_SPAWN_EGG), of(Material.GREEN_HARNESS)),
            with("warden", of(Material.WARDEN_SPAWN_EGG), of(Material.SCULK_CATALYST))
    );

    private List<String> materialRandoms = List.of(
            ".*_SWORD", ".*_AXE", ".*_PICKAXE", ".*_SHOVEL", ".*_HOE", ".*_HELMET", ".*_CHESTPLATE", ".*_LEGGINGS", ".*_BOOTS",
            "TRIDENT", "SHIELD", "FISHING_ROD", "TOTEM_OF_UNDYING", "ELYTRA",
            ".*_SPAWN_EGG", "BOW", "CROSSBOW", "ARROW", "SPECTRAL_ARROW", "TIPPED_ARROW",
            "WATER", "LAVA", "FLINT", ".*_BED"
    );


    public Collection<List<MaterialCount>> getAllMaterialPackages() {
        return materialPackages.values();
    }

    public List<Material> getRandomMaterials() {
        List<Pattern> patterns =  materialRandoms.stream()
                .map(Pattern::compile)
                .toList();
        List<Material> matchedMaterials = new ArrayList<>();

        for (Pattern pattern : patterns) {
            for (Material material : Material.values()) {
                if (pattern.matcher(material.name()).matches()) {
                    matchedMaterials.add(material);
                }
            }
        }
        return matchedMaterials;
    }


    public static class Entry implements Map.Entry<String, List<MaterialCount>> {

        private final String name;
        private List<MaterialCount> materials;

        public Entry(String name, MaterialCount[] materials) {
            this.name = name;
            this.materials = List.of(materials);
        }

        public static Entry with(String name, MaterialCount... materials) {
            return new Entry(name, materials);
        }

        @Override
        public String getKey() {
            return name;
        }

        @Override
        public List<MaterialCount> getValue() {
            return materials;
        }

        @Override
        public List<MaterialCount> setValue(List<MaterialCount> value) {
            return this.materials = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return name.equals(entry.name) && materials.equals(entry.materials);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + materials.hashCode();
        }
    }
}
