package dev.lumas.events.games.interfaces;

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
import java.util.List;
import java.util.UUID;

@Builder(builderClassName = "Builder")
public class HowToPlayDialogue {

    private final String title;
    /**
     * Not that the '\n' character is replaced with the 'space' character. Use &ltbr&gt instead!
     */
    private final String textBody;
    private final String broughtToYouBy;
    private final List<AuthorInformation> authorInformation;


    public Dialog dialog() {
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Util.color(
            textBody.stripIndent().replace("\n", " "),
            Util.TEXT_COLOR
        )));
        if (authorInformation != null && !authorInformation.isEmpty()) {
            body.add(DialogBody.plainMessage(Util.color(broughtToYouBy)));
            body.addAll(authorInformation.stream()
                .map(AuthorInformation::toDialogBody)
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

    public record AuthorInformation(UUID playerUuid, String displayName) {

        private DialogBody toDialogBody() {
            ItemStack itemStack = ItemType.PLAYER_HEAD.createItemStack();
            itemStack.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(Bukkit.createProfile(playerUuid)));
            return DialogBody.item(itemStack, DialogBody.plainMessage(Util.color(displayName)), false, false, 1, 16);
        }
    }
}
