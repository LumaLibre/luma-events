package dev.lumas.events.utility;

import lombok.Getter;

@Getter
public class Couple<F, S> {
    private final F first;
    private final S second;

    public Couple(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F a() {
        return first;
    }

    public S b() {
        return second;
    }

    public static <T, Y> Couple<T, Y> of(T first, Y second) {
        return new Couple<>(first, second);
    }
}
