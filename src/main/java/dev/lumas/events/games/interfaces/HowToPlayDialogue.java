package dev.lumas.events.games.interfaces;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.lumas.events.utility.Util;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.Builder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Builder(builderClassName = "Builder")
public class HowToPlayDialogue {

    private final String title;
    private final List<Section> sections;


    public Dialog dialog() {
        List<DialogBody> body = new ArrayList<>();
        if (sections != null && !sections.isEmpty()) {
            body.addAll(sections.stream()
                .map(Section::toDialogBody)
                .flatMap(Collection::stream)
                .toList()
            );
        }
        return Dialog.create(builder ->
            builder.empty()
                .base(
                    DialogBase.builder(Util.color(title))
                        .body(body)
                        .build()
                ).type(DialogType.notice())
        );
    }

    public interface Section {
        List<DialogBody> toDialogBody();
    }

    public record HeadSection(String base64, String title, String body) implements Section {

        public List<DialogBody> toDialogBody() {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.getProperties().add(new ProfileProperty("textures", base64));
            ItemStack itemStack = ItemType.PLAYER_HEAD.createItemStack();
            itemStack.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(profile));
            return List.of(
                DialogBody.item(itemStack, DialogBody.plainMessage(Util.color(title)), false, false, 1, 16),
                DialogBody.plainMessage(Util.color(body.stripIndent().replace("\n", " "), Util.TEXT_COLOR))
            );
        }
    }

    public record ItemSection(ItemStack item, String title, String body) implements Section {

        public List<DialogBody> toDialogBody() {
            return List.of(
                DialogBody.item(item, DialogBody.plainMessage(Util.color(title)), false, false, 1, 16),
                DialogBody.plainMessage(Util.color(body.stripIndent().replace("\n", " "), Util.TEXT_COLOR))
            );
        }
    }

    public record TextSection(String body) implements Section {

        public List<DialogBody> toDialogBody() {
            return List.of(DialogBody.plainMessage(Util.color(body.stripIndent().replace("\n", " "), Util.TEXT_COLOR)));
        }
    }
}
