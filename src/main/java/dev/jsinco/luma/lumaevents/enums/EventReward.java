package dev.jsinco.luma.lumaevents.enums;

import lombok.Getter;

@Getter
public enum EventReward {

    EVENT_8_TOKENS("lumaitems give valentide_token 8 %player%"),
    EVENT_16_TOKENS("lumaitems give valentide_token 16 %player%"),
    ;

    private final String command;

    EventReward(String command) {
        this.command = command;
    }
}
