package dev.jsinco.luma.lumaevents.npc.constants;

import dev.jsinco.luma.lumaevents.npc.obj.StalkMarketDay;
import dev.jsinco.luma.lumaevents.npc.obj.StalkMarketDay.Month;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class StalkMarketDays {

    public static final StalkMarketDay APR_17 = new StalkMarketDay(17, Month.APR, 4);
    public static final StalkMarketDay APR_18 = new StalkMarketDay(18, Month.APR, 6);
    // TODO: Need to do days for rest of week




    private static final Map<String, StalkMarketDay> VALUES = new HashMap<>();

    private static Collection<StalkMarketDay> values() {
        return VALUES.values();
    }

    //@Nullable
    public static StalkMarketDay forToday() {
        return forDay(LocalDate.now());
    }

    @Nullable
    public static StalkMarketDay forDay(LocalDate date) {
        for (StalkMarketDay stalkMarketDay : values()) {
            if (stalkMarketDay.getDate().equals(date)) {
                return stalkMarketDay;
            }
        }
        return null;
    }

    public static StalkMarketDay random() {
        return Util.getRandom(values());
    }

    static {
        for (Field field : StalkMarketDays.class.getDeclaredFields()) {
            if (field.getType() != StalkMarketDay.class) {
                continue;
            }

            try {
                StalkMarketDay value = (StalkMarketDay) field.get(null);
                VALUES.put(field.getName(), value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
