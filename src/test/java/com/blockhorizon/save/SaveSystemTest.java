package com.blockhorizon.save;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.blockhorizon.world.BlockType;
import com.blockhorizon.world.VoxelWorld;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class SaveSystemTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private SaveData data(String seed) {
        SaveData data = new SaveData();
        data.seed = seed;
        data.playerY = 12.05f;
        data.inventory.put(BlockType.WOOD.id(), 17);
        data.edits.put("1,2,3", VoxelWorld.AIR_MARKER);
        data.openedChests.add("2,3,4");
        data.achievements.add("第一次采集");
        data.blocksPlaced = 24;
        data.distanceTravelled = 419;
        return data;
    }
    @Test public void unicodePathAndFullProgressRoundTrip() throws Exception {
        SaveSystem saves = new SaveSystem(temporary.newFolder("中文目录").toPath());
        assertTrue(saves.save(data("雪原-星愿")));
        SaveData restored = saves.load();
        assertEquals("雪原-星愿", restored.seed);
        assertEquals(Integer.valueOf(17), restored.inventory.get("wood"));
        assertEquals(VoxelWorld.AIR_MARKER, restored.edits.get("1,2,3"));
        assertEquals(24, restored.blocksPlaced);
        assertEquals(419f, restored.distanceTravelled, 0.001f);
        assertEquals(SaveData.CURRENT_VERSION, restored.version);
        assertEquals(1, restored.openedChests.size());
    }
    @Test public void corruptedPrimaryRecoversPreviousSnapshot() throws Exception {
        SaveSystem saves = new SaveSystem(temporary.newFolder().toPath());
        assertTrue(saves.save(data("previous")));
        assertTrue(saves.save(data("latest")));
        Files.writeString(Path.of(saves.saveLocation()), "{broken");
        assertEquals("previous", saves.load().seed);
        assertTrue(saves.save(data("recovered")));
        Files.writeString(Path.of(saves.saveLocation()), "invalid again");
        assertEquals("previous", saves.load().seed);
    }
    @Test public void rejectedDataNeverOverwritesHealthySave() throws Exception {
        SaveSystem saves = new SaveSystem(temporary.newFolder().toPath());
        assertTrue(saves.save(data("healthy")));
        SaveData bad = data("bad");
        bad.playerX = Float.NaN;
        assertFalse(saves.save(bad));
        assertEquals("healthy", saves.load().seed);
        bad = data("bad"); bad.version = 999;
        assertFalse(saves.save(bad));
        assertEquals("healthy", saves.load().seed);
    }
    @Test public void legacyVersionTwoMigratesWithoutDeletingOriginal() throws Exception {
        Path legacy = temporary.newFile("legacy.json").toPath();
        SaveData old = data("legacy-中文");
        old.version = 2;
        Json json = new Json(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        Files.writeString(legacy, json.prettyPrint(old));
        SaveSystem saves = new SaveSystem(temporary.newFolder().toPath(), legacy);
        assertTrue(saves.exists());
        assertEquals("legacy-中文", saves.load().seed);
        assertTrue(Files.exists(legacy));
        assertTrue(Files.isRegularFile(Path.of(saves.saveLocation())));
    }
    @Test public void invalidIdsAndOutOfBoundsEditsAreRejected() throws Exception {
        SaveSystem saves = new SaveSystem(temporary.newFolder().toPath());
        SaveData bad = data("bad"); bad.edits.put("1024,1,0", "stone");
        assertFalse(saves.save(bad));
        bad = data("bad"); bad.inventory.put("nonexistent", 1);
        assertFalse(saves.save(bad));
        bad = data("bad"); bad.inventory.put("wood", null);
        assertFalse(saves.save(bad));
        bad = data("bad"); bad.inventory = null;
        assertFalse(saves.save(bad));
        assertNull(saves.load());
    }
    @Test public void inaccessibleDirectoryReturnsFailureWithoutCrashing() throws Exception {
        SaveSystem saves = new SaveSystem(temporary.newFile("not-a-directory").toPath());
        assertFalse(saves.save(data("safe")));
        assertFalse(saves.lastError().isBlank());
    }
    @Test public void successfulWritesLeaveNoTemporaryFiles() throws Exception {
        SaveSystem saves = new SaveSystem(temporary.newFolder().toPath());
        assertTrue(saves.save(data("one")));
        assertTrue(saves.save(data("two")));
        try (var files = Files.list(Path.of(saves.saveLocation()).getParent())) {
            assertEquals(2, files.count());
        }
    }
    @Test public void emptyDirectoryHasNoContinueSlot() throws Exception {
        SaveSystem saves = new SaveSystem(temporary.newFolder().toPath());
        assertFalse(saves.exists());
        assertNull(saves.load());
    }
}
