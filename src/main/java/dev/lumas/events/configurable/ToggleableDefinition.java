package dev.lumas.events.configurable;

// A map definition that can be switched off, so random selection skips it
public interface ToggleableDefinition {

    boolean isEnabled();
}
