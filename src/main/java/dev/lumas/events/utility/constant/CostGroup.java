package dev.lumas.events.utility.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CostGroup {
    A(100_000, 15_000),
    B(500_000, 50_000);

    private final int entranceCost;
    private final int replayCost;
}
