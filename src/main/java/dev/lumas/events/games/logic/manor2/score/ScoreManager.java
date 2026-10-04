package dev.lumas.events.games.logic.manor2.score;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.model.EventPlayer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ScoreManager {

    List<ScoreRecord> scoreRecordList = Collections.synchronizedList(new ArrayList<>());

    private final Manor2 manor2;

    public ScoreManager(Manor2 manor2) {
        this.manor2 = manor2;
    }

    public void registerScore(ScoreRecord scoreRecord) {
        Optional<Component> message = scoreRecord.scoreRecordDisplay()
                .display(scoreRecord.attributionPercentage(), manor2.getDefinition().getMessages());
        if (message.isEmpty()) {
            return;
        }
        for (EventPlayer participant : manor2.getParticipants()) {
            participant.operatePlayer(player -> player.sendMessage(message.get()));
        }
        scoreRecordList.add(scoreRecord);
    }
}
