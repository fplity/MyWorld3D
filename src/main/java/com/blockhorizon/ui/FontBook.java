package com.blockhorizon.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Array;

public final class FontBook implements Disposable {
    private final Array<FreeTypeFontGenerator> generators = new Array<>();
    private static final String UI_CHARS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "方境新建世界继续旅程随机种子开始返回退出设置保存音量画质控制鼠标移动视角左右键采集放置"
            + "背包合成地图暂停回到标题恢复游戏生命饱食体力创造飞行细雨晴朗繁花草甸青翠林地琥珀沙海"
            + "寂静雪原风语高地潮汐海岸草方块泥土石头沙子原木树叶雪块水木板星辉石回响晶体石砖"
            + "基岩古旧宝箱目标收集制作探索发现唤醒未知伙伴按键数字配方材料不足成功获得浆果暗影生物"
            + "击中了你自动存档已完成首次破坏建筑师伐木工旅行者夜行者星愿守望者晨午昏夜坐标天气"
            + "时刻生物帧数隐藏界面截屏音效开启关闭吃下了一颗恢复打开模式普通沉浸在水中休息"
            + "这一带似乎有奇怪的低语有什么东西在回应光芒聚成了一个小小身影它会陪你走下去"
            + "坠落伤害你倒下了按重生或菜单古老宝箱第一次被打开里面装着惊喜保存失败加载世界生成地形"
            + "搭建网格完成点击进入查看配方消耗产出当前拥有不可破坏距离太远空间被占用没有足够方块"
            + "流星划过夜空也许该许个愿雷声从远方滚来小苔团影徘徊者击败掉落状态任务已全部完成自由探索"
            + "零一二三四五六七八九十百年月日：，。！？·—×≈∞▏↑↓←→▲●【】（）";

    public final BitmapFont tiny;
    public final BitmapFont small;
    public final BitmapFont body;
    public final BitmapFont heading;
    public final BitmapFont title;

    public FontBook() {
        tiny = generate(15, 0f);
        small = generate(18, 0f);
        body = generate(22, 0f);
        heading = generate(30, 0.5f);
        title = generate(56, 1.2f);
    }

    private BitmapFont generate(int size, float borderWidth) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/LXGWWenKaiGBLite-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = UI_CHARS;
        parameter.borderWidth = borderWidth;
        parameter.borderColor.set(0.05f, 0.09f, 0.13f, 0.9f);
        parameter.shadowOffsetX = 1;
        parameter.shadowOffsetY = 2;
        parameter.shadowColor.set(0, 0, 0, 0.35f);
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.incremental = true;
        BitmapFont font = generator.generateFont(parameter);
        font.getData().markupEnabled = true;
        generators.add(generator);
        return font;
    }

    @Override
    public void dispose() {
        disposeFont(tiny);
        disposeFont(small);
        disposeFont(body);
        disposeFont(heading);
        disposeFont(title);
        for (FreeTypeFontGenerator generator : generators) generator.dispose();
        generators.clear();
    }

    private void disposeFont(BitmapFont font) {
        if (font.getData() instanceof Disposable data) data.dispose();
        font.dispose();
    }
}
