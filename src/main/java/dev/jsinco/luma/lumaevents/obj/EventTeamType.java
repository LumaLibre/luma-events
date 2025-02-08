package dev.jsinco.luma.lumaevents.obj;

import lombok.Getter;

@Getter
public enum EventTeamType {
    ROSETHORN("Rosethorn", "<#FB124F>", "<gradient:#FB124F:#F97BA2:#E34949>"),
    SWEETHEARTS("SweetHearts", "<#ffa0bc>", "<gradient:#ffa0bc:#ffdaee:#ff9b9a:#ffe9e9>"),
    HEARTBREAKERS("HeartBreakers", "<gradient:#F33A71:#50B6FC>", "<gradient:#F33A71:#F8A5BE:#ABD9F8:#50B6FC>")
    ;

    private final String formatted;
    private final String color;
    private final String gradient;

    EventTeamType(String formatted, String color, String gradient) {
        this.formatted = formatted;
        this.color = color;
        this.gradient = gradient;
    }

    public String getTeamWithGradient() {
        return gradient + formatted;
    }

    public String getTeamWithColor() {
        return color + formatted;
    }
}
