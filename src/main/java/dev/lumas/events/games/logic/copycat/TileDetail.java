package dev.lumas.events.games.logic.copycat;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public sealed interface TileDetail permits TileDetail.HeadSkin, TileDetail.BannerPatterns {

    String key();

    void applyTo(BlockState state);

    void applyTo(ItemStack item);

    static boolean same(@Nullable TileDetail a, @Nullable TileDetail b) {
        return Objects.equals(a == null ? "" : a.key(), b == null ? "" : b.key());
    }

    @Nullable
    static TileDetail of(BlockState state) {
        if (state instanceof Skull skull) {
            PlayerProfile profile = skull.getPlayerProfile();
            if (profile == null) return null;
            for (ProfileProperty property : profile.getProperties()) {
                if ("textures".equals(property.getName())) {
                    return new HeadSkin(property.getValue(), property.getSignature(), profile.getId(), profile.getName());
                }
            }
            return null;
        }
        if (state instanceof Banner banner) {
            return banner.getPatterns().isEmpty() ? null : new BannerPatterns(List.copyOf(banner.getPatterns()));
        }
        return null;
    }

    @Nullable
    static TileDetail parse(String id, CompoundBinaryTag data) {
        String type = id.toLowerCase(Locale.ROOT);
        if (type.contains("skull") || type.contains("head")) {
            return HeadSkin.parse(data);
        }
        if (type.contains("banner")) {
            return BannerPatterns.parse(data);
        }
        return null;
    }

    record HeadSkin(String textures, @Nullable String signature, @Nullable UUID id, @Nullable String name) implements TileDetail {

        @Nullable
        static HeadSkin parse(CompoundBinaryTag data) {
            BinaryTag profile = data.get("profile");
            if (profile instanceof CompoundBinaryTag compound) {
                return fromProperties(compound.getList("properties"), uuidOf(compound.get("id")), stringOrNull(compound.get("name")));
            }
            CompoundBinaryTag owner = data.getCompound("SkullOwner");
            ListBinaryTag textures = owner.getCompound("Properties").getList("textures");
            List<CompoundBinaryTag> legacy = new ArrayList<>();
            for (BinaryTag texture : textures) {
                if (texture instanceof CompoundBinaryTag compound) {
                    legacy.add(CompoundBinaryTag.builder()
                        .putString("name", "textures")
                        .putString("value", compound.getString("Value"))
                        .putString("signature", compound.getString("Signature"))
                        .build());
                }
            }
            return fromProperties(ListBinaryTag.from(legacy), uuidOf(owner.get("Id")), stringOrNull(owner.get("Name")));
        }

        @Nullable
        private static HeadSkin fromProperties(ListBinaryTag properties, @Nullable UUID id, @Nullable String name) {
            for (BinaryTag property : properties) {
                if (!(property instanceof CompoundBinaryTag compound)) continue;
                if (!"textures".equals(compound.getString("name"))) continue;
                String value = compound.getString("value");
                if (value.isEmpty()) continue;
                String signature = compound.getString("signature");
                return new HeadSkin(value, signature.isEmpty() ? null : signature, id, name);
            }
            return null;
        }

        @Nullable
        private static UUID uuidOf(@Nullable BinaryTag tag) {
            if (tag instanceof IntArrayBinaryTag ints && ints.value().length == 4) {
                int[] parts = ints.value();
                return new UUID(((long) parts[0] << 32) | (parts[1] & 0xFFFFFFFFL), ((long) parts[2] << 32) | (parts[3] & 0xFFFFFFFFL));
            }
            if (tag instanceof StringBinaryTag string) {
                try {
                    return UUID.fromString(string.value());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            return null;
        }

        @Nullable
        private static String stringOrNull(@Nullable BinaryTag tag) {
            return tag instanceof StringBinaryTag string && !string.value().isEmpty() ? string.value() : null;
        }

        private PlayerProfile profile() {
            UUID uuid = id != null ? id : UUID.nameUUIDFromBytes(textures.getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createProfile(uuid, name);
            profile.setProperty(new ProfileProperty("textures", textures, signature));
            return profile;
        }

        @Override
        public String key() {
            return "head:" + textures;
        }

        @Override
        public void applyTo(BlockState state) {
            if (state instanceof Skull skull) {
                skull.setPlayerProfile(profile());
            }
        }

        @Override
        public void applyTo(ItemStack item) {
            item.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(profile()));
        }
    }

    record BannerPatterns(List<Pattern> patterns) implements TileDetail {

        @Nullable
        static BannerPatterns parse(CompoundBinaryTag data) {
            List<Pattern> patterns = new ArrayList<>();
            for (BinaryTag entry : data.getList("patterns", BinaryTagTypes.COMPOUND)) {
                CompoundBinaryTag compound = (CompoundBinaryTag) entry;
                NamespacedKey key = NamespacedKey.fromString(compound.getString("pattern"));
                PatternType type = key == null ? null : Registry.BANNER_PATTERN.get(key);
                DyeColor color = dyeByName(compound.getString("color"));
                if (type != null && color != null) {
                    patterns.add(new Pattern(color, type));
                }
            }
            return patterns.isEmpty() ? null : new BannerPatterns(List.copyOf(patterns));
        }

        @Nullable
        private static DyeColor dyeByName(String name) {
            try {
                return DyeColor.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        public String key() {
            StringBuilder key = new StringBuilder("banner:");
            for (Pattern pattern : patterns) {
                key.append(pattern.getColor().name()).append('/').append(Registry.BANNER_PATTERN.getKeyOrThrow(pattern.getPattern()).asString()).append(',');
            }
            return key.toString();
        }

        @Override
        public void applyTo(BlockState state) {
            if (state instanceof Banner banner) {
                banner.setPatterns(patterns);
            }
        }

        @Override
        public void applyTo(ItemStack item) {
            item.editMeta(BannerMeta.class, meta -> meta.setPatterns(patterns));
        }
    }
}
