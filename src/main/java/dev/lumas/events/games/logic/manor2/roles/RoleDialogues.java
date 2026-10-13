package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.utility.Util;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

import java.util.List;
import java.util.UUID;

public class RoleDialogues {

    private static final UUID THORIN = UUID.fromString("d2b440c3-edde-4443-899e-6825c31d0919");
    private static final UUID JSINCO = UUID.fromString("144ce39d-301b-40a9-9788-0ca8cb23daf4");
    private static final UUID MITALITY = UUID.fromString("ec568111-1f7f-4446-90e5-095eac5cc9cb");

    private RoleDialogues() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Dialog dialog() {
        return Dialog.create(builder ->
            builder.empty()
                .base(
                    DialogBase.builder(Util.color("Manor 2"))
                        .body(List.of(
                            DialogBody.plainMessage(Util.color("""
                                <gray><bold>How to play</bold></gray>
                                This game uses light mechanics. It's recommended
                                that you turn <bold>off</bold> any type of <bold>night vision</bold>
                                to be able to notice.
                                
                                <gray><bold>Ghost</bold></gray>
                                The ghost kill ghost hunters, this is done by going into range and attacking.
                                You have to be strategic to stay out of light.
                                
                                <gray><bold>Hunters</bold></gray>
                                The hunters collect artifact and kills ghosts. This is done by shining on things with
                                your flashlight.
                                """.stripIndent(), Util.TEXT_COLOR)),
                            DialogBody.plainMessage(Util.color("<bold><gradient:red:#8a5500>Spooky times brought to you by<gradient>")),
                            DialogBody.item(head(THORIN), DialogBody.plainMessage(Util.color("Thorinwasher")), false, false, 1, 16),
                            DialogBody.item(head(JSINCO), DialogBody.plainMessage(Util.color("Jsinco")), false, false, 1, 16),
                            DialogBody.item(head(MITALITY), DialogBody.plainMessage(Util.color("Mitality")), false, false, 1, 16)
                        ))
                        .build()
                ).type(DialogType.notice())
        );
    }

    private static ItemStack head(UUID playerUuid) {
        ItemStack itemStack = ItemType.PLAYER_HEAD.createItemStack();
        itemStack.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(Bukkit.createProfile(playerUuid)));
        return itemStack;
    }

}
