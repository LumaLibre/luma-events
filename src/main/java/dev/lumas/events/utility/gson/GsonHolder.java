package dev.lumas.events.utility.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

public class GsonHolder {

    public static final Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .setPrettyPrinting();
        GSON = builder.create();
    }
}
