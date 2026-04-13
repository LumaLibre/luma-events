package dev.lumas.events.hooks;

import dev.jsinco.chatheads.Handler;
import dev.jsinco.chatheads.api.ChatHeadsAPI;
import dev.jsinco.chatheads.obj.CachedPlayer;
import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.Service;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
@Register(value = Autowire.SERVICE, requires = "ChatHeads")
public class ChatHeadsService implements Service {

    @Nullable
    private static ChatHeadsService instance;

    @Override
    public void register() {
        instance = this;
    }

    @Override
    public void unregister() {
        instance = null;
    }

    public static Optional<ChatHeadsService> getInstance() {
        return Optional.ofNullable(instance);
    }

    public Component getChatHead(Player player) {
        return ChatHeadsAPI.getChatHead(player);
    }

    public boolean isDisabled(Player player) {
        CachedPlayer cachedPlayer = Handler.getCachedPlayer(player);
        return cachedPlayer.isDisabled() || cachedPlayer.isNoResourcePack();
    }
}
