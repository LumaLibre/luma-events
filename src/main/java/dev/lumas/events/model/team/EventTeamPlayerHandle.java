package dev.lumas.events.model.team;

import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.model.EventPlayer;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
public class EventTeamPlayerHandle {

    private final UUID uuid;
    private int points;
    private boolean disabledTeamChat;
    private String lastKnownName;
    private transient boolean persistentTeamChat;
    private transient boolean checkedLastKnownName;

    public EventTeamPlayerHandle(EventPlayer eventPlayer) {
        this.uuid = eventPlayer.getUuid();
        this.lastKnownName = eventPlayer.getName();
        this.points = 0;
        this.disabledTeamChat = false;
    }

    public EventPlayer getEventPlayer() {
        return EventPlayerManager.getByUUID(uuid);
    }

    @Nullable
    public EventTeam getEventTeam() {
        EventPlayer eventPlayer = getEventPlayer();
        return EventTeamManager.getByMember(eventPlayer);
    }

    public boolean isDisabledTeamChat() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void setDisabledTeamChat(boolean disabledTeamChat) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public boolean togglePersistentTeamChat() {
        this.persistentTeamChat = !this.persistentTeamChat;
        return this.persistentTeamChat;
    }
}
