package dev.jsinco.luma.lumaevents.challenges;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.Serializable;

@Getter
@Setter
public abstract class Challenge implements Serializable {

    protected final ChallengeType type;
    protected final int stages;

    protected int currentStage;
    protected boolean assigned;

    protected Challenge(ChallengeType type, int stages) {
        this.type = type;
        this.stages = stages;
    }

    public boolean isCompleted() {
        return currentStage >= stages;
    }

    public boolean addStage(int amount) {
        currentStage += amount;
        return isCompleted();
    }

    public static class TypeAdapter extends com.google.gson.TypeAdapter<Challenge> {

        @Override
        public void write(JsonWriter out, Challenge value) throws IOException {
            out.beginObject();
            out.name("type").value(value.getType().name());
            out.name("currentStage").value(value.getCurrentStage());
            out.name("assigned").value(value.isAssigned());
            out.endObject();
        }

        @Override
        public Challenge read(JsonReader in) throws IOException {
            in.beginObject();
            ChallengeType type = null;
            int currentStage = 0;
            boolean assigned = false;

            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "type" ->
                        type = ChallengeType.valueOf(in.nextString());
                    case "currentStage" ->
                        currentStage = in.nextInt();
                    case "assigned" ->
                        assigned = in.nextBoolean();
                }
            }
            in.endObject();

            if (type == null) {
                throw new IOException("Missing required field 'type'");
            }
            return type.newInstance(currentStage, assigned);
        }
    }
}
