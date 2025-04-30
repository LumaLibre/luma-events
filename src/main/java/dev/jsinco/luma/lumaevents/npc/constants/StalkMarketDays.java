package dev.jsinco.luma.lumaevents.npc.constants;

import dev.jsinco.luma.lumaevents.npc.obj.StalkMarketDay;
import dev.jsinco.luma.lumaevents.npc.obj.StalkMarketDay.Month;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StalkMarketDays {

    // Event planned to go from April 20/21 to May 8/11

    // April
    public static final StalkMarketDay APR_17 = new StalkMarketDay(17, Month.APR, 16);
    public static final StalkMarketDay APR_18 = new StalkMarketDay(18, Month.APR, 15);
    public static final StalkMarketDay APR_20 = new StalkMarketDay(20, Month.APR, 17);
    public static final StalkMarketDay APR_21 = new StalkMarketDay(21, Month.APR, 16);
    public static final StalkMarketDay APR_22 = new StalkMarketDay(22, Month.APR, 14);
    public static final StalkMarketDay APR_23 = new StalkMarketDay(23, Month.APR, 15);
    public static final StalkMarketDay APR_24 = new StalkMarketDay(24, Month.APR, 18);
    public static final StalkMarketDay APR_25 = new StalkMarketDay(25, Month.APR, 14);
    public static final StalkMarketDay APR_26 = new StalkMarketDay(26, Month.APR, 15);
    public static final StalkMarketDay APR_27 = new StalkMarketDay(27, Month.APR, 16);
    public static final StalkMarketDay APR_28 = new StalkMarketDay(28, Month.APR, 16);
    public static final StalkMarketDay APR_29 = new StalkMarketDay(29, Month.APR, 13);
    public static final StalkMarketDay APR_30 = new StalkMarketDay(30, Month.APR, 20);
    // May
    public static final StalkMarketDay MAY_1 = new StalkMarketDay(1, Month.MAY, 18);
    public static final StalkMarketDay MAY_2 = new StalkMarketDay(2, Month.MAY, 16);
    public static final StalkMarketDay MAY_3 = new StalkMarketDay(3, Month.MAY, 17);
    public static final StalkMarketDay MAY_4 = new StalkMarketDay(4, Month.MAY, 14);
    public static final StalkMarketDay MAY_5 = new StalkMarketDay(5, Month.MAY, 16);
    public static final StalkMarketDay MAY_6 = new StalkMarketDay(6, Month.MAY, 16);
    public static final StalkMarketDay MAY_7 = new StalkMarketDay(7, Month.MAY, 14);
    public static final StalkMarketDay MAY_8 = new StalkMarketDay(8, Month.MAY, 19);
    public static final StalkMarketDay MAY_9 = new StalkMarketDay(9, Month.MAY, 16);
    public static final StalkMarketDay MAY_10 = new StalkMarketDay(10, Month.MAY, 23);
    public static final StalkMarketDay MAY_11 = new StalkMarketDay(11, Month.MAY, 24);
    // Extra in case I forget to end event
    public static final StalkMarketDay MAY_12 = new StalkMarketDay(12, Month.MAY, 16);
    public static final StalkMarketDay MAY_13 = new StalkMarketDay(13, Month.MAY, 15);
    public static final StalkMarketDay MAY_14 = new StalkMarketDay(14, Month.MAY, 13);
    public static final StalkMarketDay MAY_15 = new StalkMarketDay(15, Month.MAY, 16);
    public static final StalkMarketDay MAY_16 = new StalkMarketDay(16, Month.MAY, 22);
    public static final StalkMarketDay MAY_17 = new StalkMarketDay(17, Month.MAY, 15);
    public static final StalkMarketDay MAY_18 = new StalkMarketDay(18, Month.MAY, 16);
    public static final StalkMarketDay MAY_19 = new StalkMarketDay(19, Month.MAY, 16);
    public static final StalkMarketDay MAY_20 = new StalkMarketDay(20, Month.MAY, 16);
    public static final StalkMarketDay MAY_21 = new StalkMarketDay(21, Month.MAY, 16);
    public static final StalkMarketDay MAY_22 = new StalkMarketDay(22, Month.MAY, 16);


    private static final Map<String, StalkMarketDay> VALUES = new LinkedHashMap<>();

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
