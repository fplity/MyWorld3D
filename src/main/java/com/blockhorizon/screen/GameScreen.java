package com.blockhorizon.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.blockhorizon.BlockHorizonGame;
import com.blockhorizon.GameConfig;
import com.blockhorizon.desktop.SmokeEvidence;
import com.blockhorizon.entity.EntityManager;
import com.blockhorizon.player.Inventory;
import com.blockhorizon.player.PlayerController;
import com.blockhorizon.player.PlayerStats;
import com.blockhorizon.player.RecipeBook;
import com.blockhorizon.render.AtmosphereRenderer;
import com.blockhorizon.render.ParticleSystem;
import com.blockhorizon.render.WorldRenderer;
import com.blockhorizon.save.SaveData;
import com.blockhorizon.ui.GameHud;
import com.blockhorizon.world.BlockType;
import com.blockhorizon.world.GridPos;
import com.blockhorizon.world.Noise;
import com.blockhorizon.world.VoxelRaycaster;
import com.blockhorizon.world.VoxelWorld;
import com.blockhorizon.world.WorldGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class GameScreen extends InputAdapter implements Screen {
    private final BlockHorizonGame game;
    private final VoxelWorld world;
    private final PerspectiveCamera camera;
    private final PlayerController player;
    private final PlayerStats stats = new PlayerStats();
    private final Inventory inventory = new Inventory();
    private final AtmosphereRenderer atmosphere;
    private final WorldRenderer worldRenderer;
    private final EntityManager entities;
    private final ParticleSystem particles = new ParticleSystem();
    private final ModelBatch modelBatch = new ModelBatch();
    private final ShapeRenderer selectionShapes = new ShapeRenderer();
    private final GameHud hud;
    private final Set<String> achievements = new HashSet<>();
    private final Random random;
    private VoxelRaycaster.Hit target;
    private GridPos miningBlock;
    private float miningTime;
    private float miningProgress;
    private float attackCooldown;
    private float autoSaveTimer = 42f;
    private float saveStatusTimer;
    private String saveStatus = "";
    private boolean paused;
    private boolean inventoryOpen;
    private boolean mapOpen;
    private boolean dead;
    private boolean debug;
    private boolean hiddenHud;
    private boolean screenshotRequested;
    private boolean shrineAwakened;
    private int questStage;
    private int blocksPlaced;
    private float distanceTravelled;
    private final Vector3 previousPosition = new Vector3();
    private float companionHealCooldown;
    private boolean disposed;
    private int smokeFrames;

    public GameScreen(BlockHorizonGame game, String seedText, SaveData save) {
        this.game = game;
        String seed = save != null && save.seed != null ? save.seed : seedText;
        world = new WorldGenerator(seed).generate();
        if (save != null && save.edits != null) {
            for (Map.Entry<String, String> edit : save.edits.entrySet()) {
                try { world.applyEdit(GridPos.parse(edit.getKey()), edit.getValue()); }
                catch (RuntimeException exception) { Gdx.app.error("GameScreen", "忽略损坏的方块编辑", exception); }
            }
        }

        camera = new PerspectiveCamera(72f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.045f;
        camera.far = 190f;
        player = new PlayerController(camera);
        if (save == null) {
            inventory.giveStarterKit();
            player.spawn(world.findSpawn());
        } else restore(save);
        previousPosition.set(player.position());

        long numericSeed = Noise.seedHash(world.seed());
        random = new Random(numericSeed ^ 0xB10C0FFEE0L);
        atmosphere = new AtmosphereRenderer(numericSeed);
        if (save != null) atmosphere.setTimeOfDay(save.timeOfDay);
        worldRenderer = new WorldRenderer(world);
        entities = new EntityManager(world, atmosphere.environment(), numericSeed);
        if (save != null && save.companionUnlocked) entities.unlockCompanion();
        hud = new GameHud(game, world, inventory, player, stats, atmosphere);
        if (Boolean.getBoolean("blockhorizon.smoke")) {
            if ("night".equalsIgnoreCase(System.getProperty("blockhorizon.smokeScenario", ""))) atmosphere.setTimeOfDay(0.65f);
            String overlay = System.getProperty("blockhorizon.smokeOverlay", "");
            inventoryOpen = "inventory".equalsIgnoreCase(overlay);
            mapOpen = "map".equalsIgnoreCase(overlay);
            paused = "pause".equalsIgnoreCase(overlay);
        }
        if (save == null) {
            hud.notify("欢迎来到方境。先找一棵树，开始你的故事。", new Color(0xbdf7eaff), 6f);
            hud.notify("提示：按 E 打开背包与合成", new Color(0x8fb8c7ff), 5f);
        } else hud.notify("世界已从上次的记忆中苏醒");
    }

    private void restore(SaveData save) {
        inventory.restore(save.inventory == null ? Map.of() : save.inventory, save.berries, save.selectedSlot, save.creative);
        stats.restore(save.health, save.hunger, save.stamina);
        Vector3 savedPosition = new Vector3(save.playerX, save.playerY, save.playerZ);
        if (!world.insideHorizontalBounds(MathUtils.floor(savedPosition.x), MathUtils.floor(savedPosition.z)) || savedPosition.y < 0) {
            player.spawn(world.findSpawn());
        } else if (!save.creative && player.collides(world, savedPosition.x, savedPosition.y, savedPosition.z)) {
            player.spawn(world.findSpawn());
        } else player.restore(savedPosition, save.yaw, save.pitch, save.flying && save.creative);
        dead = !stats.alive();
        blocksPlaced = save.blocksPlaced;
        distanceTravelled = save.distanceTravelled;
        questStage = MathUtils.clamp(save.questStage, 0, 5);
        shrineAwakened = save.shrineAwakened;
        if (save.achievements != null) achievements.addAll(save.achievements);
        if (save.openedChests != null) world.restoreOpenedChests(save.openedChests);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(Boolean.getBoolean("blockhorizon.smoke") ? null : this);
        updateCursor();
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        boolean active = !paused && !inventoryOpen && !mapOpen && !dead && !Boolean.getBoolean("blockhorizon.smoke");
        if (active && Gdx.input.isCursorCatched()) player.look(Gdx.input.getDeltaX(), Gdx.input.getDeltaY());

        atmosphere.update(delta, camera, paused || dead);
        if (!paused && !dead) {
            player.update(delta, world, stats, active, inventory.creative());
            distanceTravelled += previousPosition.dst(player.position());
            previousPosition.set(player.position());
            updateSurvival(delta);
            if (!dead) updateEntities(delta);
            if (active && !dead) updateInteraction(delta);
            else resetMining();
            updateQuest();
            updateAutosave(delta);
        }
        particles.update(delta);
        hud.update(delta);
        attackCooldown -= delta;
        companionHealCooldown -= delta;
        if (saveStatusTimer > 0) saveStatusTimer -= delta;
        else saveStatus = "";

        renderWorld();
        hud.draw(inventoryOpen, mapOpen, paused, dead, debug, hiddenHud, miningProgress, target, questStage, saveStatus);
        if (screenshotRequested) captureScreenshot();
        runSmokeHook();
    }

    private void updateSurvival(float delta) {
        float impact = player.consumeLandingImpact();
        if (!inventory.creative() && impact > 10.4f) {
            float damage = (impact - 9.5f) * 3.1f;
            stats.damage(damage);
            game.audio().hurt();
            hud.warn("坠落伤害  -" + Math.round(damage));
        }
        if (player.position().y < -5f) {
            stats.damage(100f);
        }
        if (!stats.alive()) {
            die();
            return;
        }
        if (entities.companionUnlocked() && stats.health() < 42f && companionHealCooldown <= 0f) {
            stats.heal(9f);
            companionHealCooldown = 38f;
            particles.sparkle(entities.companionPosition(), new Color(0xbcaaffff));
            game.audio().companion();
            hud.notify("小星灵轻轻碰了碰你，伤口正在愈合", new Color(0xc6b9ffff), 4.5f);
        }
    }

    private void updateEntities(float delta) {
        EntityManager.UpdateResult result = entities.update(delta, player.position(), atmosphere.isNight(), paused);
        if (result.damage() > 0 && !inventory.creative()) {
            stats.damage(result.damage());
            game.audio().hurt();
            hud.warn(result.message());
            if (!stats.alive()) die();
        }
        if (atmosphere.consumeLightningEvent()) {
            game.audio().thunder();
            hud.notify("雷声从远方滚来", new Color(0xaec8e8ff), 2.8f);
        }
        if (atmosphere.consumeMeteorEvent()) {
            hud.notify("流星划过夜空——也许该许个愿", new Color(0xb8b7ffff), 4.5f);
            unlockAchievement("星愿守望者");
        }
    }

    private void die() {
        if (dead) return;
        dead = true;
        resetMining();
        Gdx.input.setCursorCatched(false);
        saveWorld(false);
    }

    private void updateInteraction(float delta) {
        target = VoxelRaycaster.cast(world, camera.position, camera.direction, GameConfig.REACH);
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            resetMining();
            return;
        }

        if (attackCooldown <= 0f) {
            EntityManager.AttackResult attack = entities.attack(camera.position, camera.direction,
                    target == null ? GameConfig.REACH : target.distance(),
                    inventory.creative() ? 100f : 15f);
            if (attack.hit()) {
                attackCooldown = 0.34f;
                resetMining();
                particles.burst(attack.position(), attack.type() == EntityManager.MobType.SHADE ? new Color(0xa064dfff) : new Color(0x65aa56ff), 18, 3.5f);
                game.audio().hit();
                if (attack.killed()) {
                    if (attack.type() == EntityManager.MobType.SHADE) {
                        inventory.add(BlockType.CRYSTAL, 1);
                        hud.notify("击败影徘徊者，获得回响晶体");
                    } else {
                        int berries = 1 + random.nextInt(2);
                        inventory.addBerries(berries);
                        hud.notify("小苔团留下了浆果 ×" + berries);
                    }
                }
                return;
            }
        }

        if (target == null || !target.type().breakable()) {
            if (target != null && !target.type().breakable() && attackCooldown <= 0) {
                hud.warn("基岩不可破坏");
                attackCooldown = 1.5f;
            }
            resetMining();
            return;
        }
        if (!target.block().equals(miningBlock)) {
            miningBlock = target.block();
            miningTime = 0f;
        }
        float duration = inventory.creative() ? 0.06f : 0.11f + target.type().hardness() * 0.72f;
        miningTime += delta;
        miningProgress = MathUtils.clamp(miningTime / duration, 0f, 1f);
        if (miningTime >= duration) breakTarget(target);
    }

    private void breakTarget(VoxelRaycaster.Hit hit) {
        BlockType type = hit.type();
        GridPos position = hit.block();
        world.set(position, null);
        if (type != BlockType.WATER && type != BlockType.BEDROCK) inventory.add(type, 1);
        if (type == BlockType.LEAVES && random.nextFloat() < 0.32f) {
            inventory.addBerries(1);
            hud.notify("树叶间藏着一颗浆果");
        }
        if (type == BlockType.CHEST && !world.isChestOpened(position)) {
            inventory.add(BlockType.PLANKS, 2);
        }
        particles.burst(position.center(), type.color(), type.emissive() ? 32 : 18, type.emissive() ? 5.2f : 3.4f);
        game.audio().breakBlock(type);
        worldRenderer.rebuildDirty();
        unlockAchievement("第一次采集");
        if (type == BlockType.WOOD && inventory.count(BlockType.WOOD) >= 12) unlockAchievement("伐木工");
        if (type == BlockType.CRYSTAL && world.shrineCenter() != null && position.equals(world.shrineCenter()) && !shrineAwakened) {
            awakenShrine(position);
        }
        resetMining();
        target = null;
    }

    private void awakenShrine(GridPos position) {
        shrineAwakened = true;
        questStage = 5;
        entities.unlockCompanion();
        atmosphere.clearWeather();
        inventory.add(BlockType.CRYSTAL, 3);
        stats.heal(35f);
        particles.sparkle(position.center(), new Color(0xb7a3ffff));
        game.audio().companion();
        hud.notify("光芒聚成了一个小小身影……它会陪你走下去", new Color(0xd3c8ffff), 8f);
        unlockAchievement("唤醒回响");
    }

    private void resetMining() {
        miningBlock = null;
        miningTime = 0f;
        miningProgress = 0f;
    }

    private void placeOrInteract() {
        target = VoxelRaycaster.cast(world, camera.position, camera.direction, GameConfig.REACH);
        if (dead || target == null) return;
        if (target.type() == BlockType.CHEST) {
            openChest(target.block());
            return;
        }
        GridPos placement = target.placementPosition();
        if (target.normal().equals(new GridPos(0, 0, 0))) return;
        if (!world.insideHorizontalBounds(placement.x(), placement.z()) || placement.y() <= 0 || placement.y() >= GameConfig.MAX_BUILD_HEIGHT) {
            hud.warn("这里无法放置方块");
            return;
        }
        if (world.get(placement) != null || player.overlapsBlock(placement)) {
            hud.warn("空间被占用");
            return;
        }
        BlockType selected = inventory.selected();
        if (!inventory.remove(selected, 1)) {
            hud.warn("没有足够的" + selected.displayName());
            return;
        }
        world.set(placement, selected);
        blocksPlaced++;
        worldRenderer.rebuildDirty();
        particles.burst(placement.center(), selected.color(), 9, 1.7f);
        game.audio().place();
        if (blocksPlaced >= 25) unlockAchievement("建筑师");
    }

    private void openChest(GridPos position) {
        if (!world.openChest(position)) {
            hud.notify("宝箱已经空了");
            return;
        }
        inventory.add(BlockType.CRYSTAL, 2);
        inventory.add(BlockType.GLOWSTONE, 4);
        inventory.add(BlockType.PLANKS, 8);
        inventory.addBerries(4);
        game.audio().treasure();
        particles.sparkle(position.center(), new Color(0xffd37aff));
        hud.notify("古老宝箱第一次被打开：晶体 ×2、星辉石 ×4、木板 ×8、浆果 ×4", new Color(0xffd98aff), 7f);
        unlockAchievement("寻宝人");
    }

    private void updateQuest() {
        if (questStage == 0 && inventory.count(BlockType.WOOD) >= 3) {
            questStage = 1;
            hud.notify("目标完成：现在打开背包，把原木制成木板");
        } else if (questStage == 1 && inventory.count(BlockType.PLANKS) >= 4) {
            questStage = 2;
            hud.notify("木板准备好了。远处似乎有一座林间小屋。", new Color(0xbdeee4ff), 5f);
        } else if (questStage == 2 && world.cabinCenter() != null && player.position().dst(world.cabinCenter().center()) < 8f) {
            questStage = 3;
            unlockAchievement("旅行者");
            hud.notify("你发现了林间小屋。屋里也许留着前人的物品。", new Color(0xf0c275ff), 5f);
        } else if (questStage == 3 && world.shrineCenter() != null && player.position().dst(world.shrineCenter().center()) < 9f) {
            questStage = 4;
            hud.notify("这一带有奇怪的低语……中央晶体正在等待回应。", new Color(0xc6b2ffff), 6f);
        }
        if (distanceTravelled > 420f) unlockAchievement("漫游者");
        if (atmosphere.isNight()) unlockAchievement("夜行者");
    }

    private void unlockAchievement(String name) {
        if (achievements.add(name)) hud.achievement(name);
    }

    private void updateAutosave(float delta) {
        autoSaveTimer -= delta;
        if (autoSaveTimer <= 0f) {
            saveWorld(true);
            autoSaveTimer = 42f;
        }
    }

    private boolean saveWorld(boolean automatic) {
        if (Boolean.getBoolean("blockhorizon.smoke") && !Boolean.getBoolean("blockhorizon.smokeSave")) return true;
        SaveData save = new SaveData();
        save.seed = world.seed();
        save.playerX = player.position().x;
        save.playerY = player.position().y;
        save.playerZ = player.position().z;
        save.yaw = player.yaw();
        save.pitch = player.pitch();
        save.flying = player.flying();
        save.health = stats.health();
        save.hunger = stats.hunger();
        save.stamina = stats.stamina();
        save.timeOfDay = atmosphere.timeOfDay();
        inventory.snapshot().forEach((type, count) -> save.inventory.put(type.id(), count));
        save.berries = inventory.berries();
        save.selectedSlot = inventory.selectedIndex();
        save.creative = inventory.creative();
        save.edits.putAll(world.edits());
        save.openedChests.addAll(world.openedChests());
        save.achievements.addAll(achievements);
        save.questStage = questStage;
        save.shrineAwakened = shrineAwakened;
        save.companionUnlocked = entities.companionUnlocked();
        save.blocksPlaced = blocksPlaced;
        save.distanceTravelled = distanceTravelled;
        boolean success = game.saves().save(save);
        saveStatus = success ? (automatic ? "自动存档完成" : "世界已保存") : "保存失败";
        saveStatusTimer = 3f;
        if (!automatic) {
            if (success) hud.notify("世界已保存");
            else hud.warn("保存失败");
        }
        return success;
    }

    private void renderWorld() {
        Color clear = atmosphere.skyColor();
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        modelBatch.begin(camera);
        atmosphere.renderModels(modelBatch);
        worldRenderer.render(modelBatch, camera, atmosphere.environment());
        entities.render(modelBatch, atmosphere.environment());
        modelBatch.end();
        if (target != null && !paused && !inventoryOpen && !mapOpen && !dead) drawSelection(target.block());
        atmosphere.renderWeather(camera);
        particles.render(camera);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    private void drawSelection(GridPos block) {
        float e = 0.0035f;
        float x0 = block.x() - e, y0 = block.y() - e, z0 = block.z() - e;
        float x1 = block.x() + 1 + e, y1 = block.y() + 1 + e, z1 = block.z() + 1 + e;
        Gdx.gl.glLineWidth(2f);
        selectionShapes.setProjectionMatrix(camera.combined);
        selectionShapes.begin(ShapeRenderer.ShapeType.Line);
        selectionShapes.setColor(0.9f, 1f, 0.97f, 0.9f);
        line(x0,y0,z0,x1,y0,z0); line(x1,y0,z0,x1,y0,z1); line(x1,y0,z1,x0,y0,z1); line(x0,y0,z1,x0,y0,z0);
        line(x0,y1,z0,x1,y1,z0); line(x1,y1,z0,x1,y1,z1); line(x1,y1,z1,x0,y1,z1); line(x0,y1,z1,x0,y1,z0);
        line(x0,y0,z0,x0,y1,z0); line(x1,y0,z0,x1,y1,z0); line(x1,y0,z1,x1,y1,z1); line(x0,y0,z1,x0,y1,z1);
        selectionShapes.end();
        Gdx.gl.glLineWidth(1f);
    }

    private void line(float x1,float y1,float z1,float x2,float y2,float z2) {
        selectionShapes.line(x1,y1,z1,x2,y2,z2);
    }

    private void captureScreenshot() {
        screenshotRequested = false;
        try {
            Pixmap pixmap = flippedFrameBuffer();
            String name = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".png";
            PixmapIO.writePNG(Gdx.files.absolute(game.saves().screenshotDirectory().resolve(name).toString()), pixmap);
            pixmap.dispose();
            hud.notify("截图已保存：" + name);
        } catch (RuntimeException exception) {
            hud.warn("截图保存失败");
        }
    }

    private void runSmokeHook() {
        if (!Boolean.getBoolean("blockhorizon.smoke")) return;
        smokeFrames++;
        if (smokeFrames == 10 && "gameplay".equals(System.getProperty("blockhorizon.smokeScenario"))) smokeGameplayChecks();
        if (smokeFrames == 45) {
            try {
                Pixmap pixmap = flippedFrameBuffer();
                String destination = System.getProperty("blockhorizon.smokeScreenshot", "build/smoke/world.png");
                PixmapIO.writePNG(Gdx.files.absolute(new java.io.File(destination).getAbsolutePath()), pixmap);
                pixmap.dispose();
                SmokeEvidence.success(Map.of("mode", System.getProperty("blockhorizon.smokeOverlay", "world"),
                        "scenario", System.getProperty("blockhorizon.smokeScenario", "day"),
                        "seed", world.seed(), "blocks", world.blockCount(),
                        "visibleChunks", worldRenderer.visibleChunks(), "totalChunks", worldRenderer.totalChunks(),
                        "mobs", entities.mobCount(), "frames", smokeFrames));
            } catch (RuntimeException exception) {
                Gdx.app.error("Smoke", "截图失败", exception);
            }
        }
        if (smokeFrames > 75) Gdx.app.exit();
    }

    private void smokeGameplayChecks() {
        inventory.add(BlockType.WOOD, 2);
        int before = inventory.count(BlockType.PLANKS);
        craft(0);
        if (inventory.count(BlockType.PLANKS) != before + 4) throw new IllegalStateException("合成回归失败");
        GridPos chest = world.cabinCenter().add(2, 0, 2);
        openChest(chest);
        int crystals = inventory.count(BlockType.CRYSTAL);
        openChest(chest);
        if (inventory.count(BlockType.CRYSTAL) != crystals) throw new IllegalStateException("宝箱重复领取");
        GridPos shrine = world.shrineCenter();
        breakTarget(new VoxelRaycaster.Hit(shrine, new GridPos(0, 1, 0), BlockType.CRYSTAL, 1));
        if (!shrineAwakened || !entities.companionUnlocked() || questStage != 5)
            throw new IllegalStateException("伙伴解锁回归失败");
        inventory.setCreative(true);
        player.restore(new Vector3(0.5f, 3, 0.5f), 0, 0, true);
        camera.position.set(0.5f, 35.5f, 0.5f);
        camera.direction.set(0, -1, 0); camera.update();
        world.set(0, 34, 0, BlockType.STONE);
        worldRenderer.rebuildDirty();
        int placed = blocksPlaced;
        placeOrInteract();
        if (blocksPlaced != placed + 1 || world.get(0, 35, 0) != inventory.selected())
            throw new IllegalStateException("方块放置回归失败");
        world.set(0, 35, 0, null);
        dead = true;
        placeOrInteract();
        touchDown(0, 0, 0, Input.Buttons.RIGHT);
        if (world.get(0, 35, 0) != null || blocksPlaced != placed + 1)
            throw new IllegalStateException("死亡交互回归失败");
        dead = false;
        respawn();
        inventory.setCreative(false);
        world.set(0, 34, 0, null);
        worldRenderer.rebuildDirty();
        if (!saveWorld(false)) throw new IllegalStateException("实际游戏存档失败");
        SaveData saved = game.saves().load();
        if (saved == null || !saved.companionUnlocked || saved.blocksPlaced != blocksPlaced
                || saved.questStage != 5 || saved.version != SaveData.CURRENT_VERSION)
            throw new IllegalStateException("实际游戏存档恢复失败");
    }

    private Pixmap flippedFrameBuffer() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        Pixmap source = Pixmap.createFromFrameBuffer(0, 0, width, height);
        Pixmap flipped = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            flipped.drawPixmap(source, 0, y, width, 1, 0, height - 1 - y, width, 1);
        }
        source.dispose();
        return flipped;
    }

    private void craft(int index) {
        if (index < 0 || index >= RecipeBook.RECIPES.size()) return;
        if (inventory.craft(RecipeBook.RECIPES.get(index))) {
            game.audio().craft();
            hud.notify("合成成功：" + RecipeBook.RECIPES.get(index).name());
        } else hud.warn("材料不足");
    }

    private void eatBerry() {
        if (stats.hunger() >= 99f) {
            hud.notify("你现在还不饿");
            return;
        }
        if (inventory.eatBerry()) {
            stats.feed(18f);
            stats.heal(2f);
            game.audio().place();
            hud.notify("吃下了一颗浆果");
        } else hud.warn("背包里没有浆果");
    }

    private void togglePause() {
        paused = !paused;
        if (paused) saveWorld(true);
        updateCursor();
    }

    private void updateCursor() {
        Gdx.input.setCursorCatched(!paused && !inventoryOpen && !mapOpen && !dead);
    }

    private void respawn() {
        stats.respawn();
        player.spawn(world.findSpawn());
        dead = false;
        paused = false;
        previousPosition.set(player.position());
        hud.notify("晨风把你带回了世界");
        updateCursor();
    }

    private void saveAndReturn() {
        if (saveWorld(false)) game.showTitle();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (dead) {
            if (keycode == Input.Keys.R) respawn();
            else if (keycode == Input.Keys.ESCAPE) saveAndReturn();
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) {
            if (inventoryOpen) inventoryOpen = false;
            else if (mapOpen) mapOpen = false;
            else togglePause();
            updateCursor();
            return true;
        }
        if (paused) return true;
        if (keycode == Input.Keys.E) {
            inventoryOpen = !inventoryOpen;
            mapOpen = false;
            resetMining();
            updateCursor();
            return true;
        }
        if (keycode == Input.Keys.M) {
            mapOpen = !mapOpen;
            inventoryOpen = false;
            resetMining();
            updateCursor();
            return true;
        }
        if (keycode == Input.Keys.F1) hiddenHud = !hiddenHud;
        else if (keycode == Input.Keys.F2) screenshotRequested = true;
        else if (keycode == Input.Keys.F3) debug = !debug;
        else if (keycode == Input.Keys.F5) saveWorld(false);
        else if (keycode == Input.Keys.C) {
            inventory.toggleCreative();
            if (!inventory.creative()) {
                player.setFlying(false);
                escapeSolidBlock();
            }
            hud.notify(inventory.creative() ? "创造模式开启" : "生存模式开启");
        } else if (keycode == Input.Keys.F && inventory.creative()) {
            player.toggleFlying();
            if (!player.flying()) escapeSolidBlock();
            hud.notify(player.flying() ? "飞行模式开启" : "飞行模式关闭");
        } else if (keycode == Input.Keys.R && !inventoryOpen && !mapOpen) eatBerry();
        else if (keycode == Input.Keys.T && inventory.creative()) atmosphere.toggleWeather();
        else if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            int index = keycode - Input.Keys.NUM_1;
            if (inventoryOpen) craft(index);
            else inventory.select(index);
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (dead) return true;
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        if (paused) {
            int action = hud.pauseAction(screenX, screenY, w, h);
            if (action == 1) togglePause();
            else if (action == 2) saveWorld(false);
            else if (action == 3) saveAndReturn();
            return true;
        }
        if (inventoryOpen) {
            int recipe = hud.recipeAt(screenX, screenY, w, h);
            if (recipe >= 0) craft(recipe);
            return true;
        }
        if (mapOpen) return true;
        if (button == Input.Buttons.RIGHT) {
            placeOrInteract();
            return true;
        }
        return false;
    }

    private void escapeSolidBlock() {
        if (player.collides(world, player.position().x, player.position().y, player.position().z)) {
            player.spawn(world.findSpawn());
            previousPosition.set(player.position());
        }
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (!paused && !inventoryOpen && !mapOpen && !dead) inventory.scroll(amountY > 0 ? 1 : -1);
        return true;
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override public void pause() { if (!dead) { paused = true; saveWorld(true); updateCursor(); } }
    @Override public void resume() { }
    @Override public void hide() { Gdx.input.setCursorCatched(false); }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (!dead) saveWorld(true);
        worldRenderer.dispose();
        atmosphere.dispose();
        entities.dispose();
        particles.dispose();
        modelBatch.dispose();
        selectionShapes.dispose();
    }
}
