package com.blockhorizon.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.blockhorizon.GameConfig;
import com.blockhorizon.world.BlockType;
import com.blockhorizon.world.GridPos;
import com.blockhorizon.world.VoxelWorld;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class SaveSystem {
    private static final long MAX_SAVE_BYTES = 20L * 1024 * 1024;
    private final Json json = new Json(JsonWriter.OutputType.json);
    private final Path directory;
    private final Path legacy;
    private String lastError = "";

    public SaveSystem() {
        this(defaultDataDirectory(), Path.of("BlockHorizon", "saves", "world.json"));
    }

    public SaveSystem(Path directory) { this(directory, null); }

    public SaveSystem(Path directory, Path legacy) {
        this.directory = directory.toAbsolutePath().normalize();
        this.legacy = legacy == null ? null : legacy.toAbsolutePath().normalize();
        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);
        json.setTypeName(null);
    }

    public static Path defaultDataDirectory() {
        String override = System.getProperty("blockhorizon.dataDir");
        if (override != null && !override.isBlank()) return Path.of(override);
        String local = System.getenv("LOCALAPPDATA");
        return local != null && !local.isBlank() ? Path.of(local, "BlockHorizon")
                : Path.of(System.getProperty("user.home"), ".blockhorizon");
    }

    private Path file() { return directory.resolve("saves/world.json"); }
    private Path backup() { return directory.resolve("saves/world.json.bak"); }

    public boolean exists() {
        return Files.isRegularFile(file()) || Files.isRegularFile(backup())
                || (legacy != null && Files.isRegularFile(legacy));
    }

    public synchronized SaveData load() {
        lastError = "";
        for (Path candidate : new Path[]{file(), backup(), legacy}) {
            if (candidate == null || !Files.isRegularFile(candidate)) continue;
            try {
                SaveData data = read(candidate);
                if (candidate.equals(legacy) && !save(data)) report("旧存档迁移失败，请保留旧文件", null);
                return data;
            } catch (Exception exception) {
                report("存档读取失败：" + candidate.getFileName(), exception);
            }
        }
        return null;
    }

    public synchronized boolean save(SaveData data) {
        Path temporary = null;
        Path backupTemporary = null;
        try {
            validate(data);
            Files.createDirectories(file().getParent());
            data.version = SaveData.CURRENT_VERSION;
            data.savedAt = System.currentTimeMillis();
            byte[] content = json.prettyPrint(data).getBytes(StandardCharsets.UTF_8);
            if (content.length > MAX_SAVE_BYTES) throw new IOException("存档过大");
            temporary = Files.createTempFile(file().getParent(), "world-", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            // Do not overwrite a healthy backup with an already-corrupted primary file.
            if (Files.isRegularFile(file())) {
                boolean healthy;
                try { read(file()); healthy = true; } catch (Exception invalid) { healthy = false; }
                if (healthy) {
                    backupTemporary = Files.createTempFile(file().getParent(), "backup-", ".tmp");
                    Files.copy(file(), backupTemporary, StandardCopyOption.REPLACE_EXISTING);
                    replace(backupTemporary, backup());
                }
            }
            replace(temporary, file());
            lastError = "";
            return true;
        } catch (Exception exception) {
            report("存档写入失败", exception);
            return false;
        } finally {
            cleanup(temporary);
            cleanup(backupTemporary);
        }
    }

    private SaveData read(Path candidate) throws IOException {
        if (Files.size(candidate) > MAX_SAVE_BYTES) throw new IOException("存档过大");
        SaveData data = json.fromJson(SaveData.class, Files.readString(candidate, StandardCharsets.UTF_8));
        validate(data);
        return data;
    }

    private static void validate(SaveData data) {
        if (data == null || data.version < 1 || data.version > SaveData.CURRENT_VERSION)
            throw new IllegalArgumentException("不支持的存档版本");
        if (data.seed == null || data.seed.isBlank() || data.seed.length() > 256)
            throw new IllegalArgumentException("无效世界种子");
        for (float value : new float[]{data.playerX, data.playerY, data.playerZ, data.yaw, data.pitch,
                data.health, data.hunger, data.stamina, data.timeOfDay, data.distanceTravelled}) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("存档数值不合法");
        }
        if (Math.abs(data.playerX) > 1024 || Math.abs(data.playerZ) > 1024 || Math.abs(data.playerY) > 1024)
            throw new IllegalArgumentException("玩家位置不合法");
        if (data.inventory == null || data.edits == null || data.openedChests == null || data.achievements == null
                || data.edits.size() > GameConfig.WORLD_SIZE * GameConfig.WORLD_SIZE * GameConfig.MAX_BUILD_HEIGHT
                || data.openedChests.size() > 4096 || data.achievements.size() > 128)
            throw new IllegalArgumentException("存档集合不合法");
        data.inventory.forEach((id, count) -> {
            BlockType.byId(id);
            if (count == null || count < 0 || count > 1_000_000) throw new IllegalArgumentException("物品数量不合法");
        });
        data.edits.forEach((position, id) -> {
            validPosition(position);
            if (!VoxelWorld.AIR_MARKER.equals(id)) BlockType.byId(id);
        });
        data.openedChests.forEach(SaveSystem::validPosition);
        if (data.berries < 0 || data.berries > 1_000_000 || data.blocksPlaced < 0 || data.distanceTravelled < 0)
            throw new IllegalArgumentException("进度数值不合法");
    }

    private static void validPosition(String serialized) {
        GridPos p = GridPos.parse(serialized);
        if (p.x() < -GameConfig.WORLD_HALF || p.x() >= GameConfig.WORLD_HALF
                || p.z() < -GameConfig.WORLD_HALF || p.z() >= GameConfig.WORLD_HALF
                || p.y() < 0 || p.y() >= GameConfig.MAX_BUILD_HEIGHT) throw new IllegalArgumentException("方块坐标越界");
    }

    private static void replace(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cleanup(Path temporary) {
        if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
    }

    private void report(String message, Exception exception) {
        lastError = message;
        if (Gdx.app != null) Gdx.app.error("SaveSystem", message, exception);
    }

    public String lastError() { return lastError; }
    public String saveLocation() { return file().toString(); }
    public Path screenshotDirectory() { return directory.resolve("screenshots"); }
}
