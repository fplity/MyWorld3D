package com.blockhorizon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.nio.file.Path;
import java.util.Map;

/** Explicit success marker: an ordinary zero exit code is not enough to pass graphics verification. */
public final class SmokeEvidence {
    private static boolean succeeded;
    private SmokeEvidence() { }
    public static void success(Map<String, Object> evidence) {
        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        evidence.forEach((key, value) -> {
            JsonValue item = value instanceof Boolean b ? new JsonValue(b)
                    : value instanceof Number n ? new JsonValue(n.longValue()) : new JsonValue(value.toString());
            json.addChild(key, item);
        });
        String path = System.getProperty("blockhorizon.smokeReport", "build/smoke/result.json");
        Gdx.files.absolute(Path.of(path).toAbsolutePath().toString()).writeString(json.toJson(JsonWriter.OutputType.json), false, "UTF-8");
        succeeded = true;
    }
    public static boolean succeeded() { return succeeded; }
}
