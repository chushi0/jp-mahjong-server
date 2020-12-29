package online.cszt0.jpmahjong.game;

import org.springframework.lang.Nullable;

import java.util.ArrayList;

/**
 * 玩家
 */
public abstract class Player {
    /**
     * 玩家名
     */
    public String name;
    /**
     * 准备状态
     */
    public boolean ready;
    /**
     * 点数
     */
    public int point;
    /**
     * 点数改变
     */
    public int pointChange;
    /**
     * 牌面信息
     */
    public Mahjong.Plate plate;
    /**
     * 刚刚摸到的牌
     */
    public Mahjong.Card justObtain;
    /**
     * 舍张
     */
    public ArrayList<Mahjong.Shezhang> shezhang;
    /**
     * 立直标记
     */
    public Mahjong.RiChiType riChiType;
    /**
     * 一发标记
     */
    public boolean ihatsu;
    /**
     * 振听标记
     * （非立直状态下）出牌时更新
     */
    public boolean zhenting;
    /**
     * 听牌
     */
    public ArrayList<Mahjong.Card> tingpai;
    /**
     * （流局或和牌时）需展示手牌
     */
    public boolean showPlate;

    /**
     * 中途操作选项
     */
    protected int flag;
    /**
     * 可操作的牌
     */
    protected Mahjong.Card operateCard;
    /**
     * 牌来源
     */
    protected int operateSrc;

    protected static final int FLAG_NONE = 0;
    /**
     * 吃
     */
    protected static final int FLAG_CHI = 1;
    /**
     * 碰
     */
    protected static final int FLAG_PENG = 2;
    /**
     * 和
     */
    protected static final int FLAG_RON = 4;

    /**
     * 新游戏初始化
     */
    public void newGame() {
        point = 25000;
    }

    /**
     * 新的一局初始化
     */
    public void newMatch() {
        plate = new Mahjong.Plate();
        shezhang = new ArrayList<>();
        riChiType = Mahjong.RiChiType.None;
        ihatsu = false;
        zhenting = false;
        tingpai = null;
        showPlate = false;
        pointChange = 0;
    }

    /**
     * 等待玩家操作
     *
     * @param firstXun 是否是首巡（无人鸣牌的第一巡）
     * @return 玩家操作
     */
    public abstract Action waitForAction(boolean firstXun, boolean canGang);

    /**
     * 初始化选项
     */
    public void initOperate(Mahjong.Card card, int src) {
        flag = FLAG_NONE;
        operateCard = card;
        operateSrc = src;
    }

    public void operateCanChi() {
        flag |= FLAG_CHI;
    }

    public void operateCanPeng() {
        flag |= FLAG_PENG;
    }

    public void operateCanRon() {
        flag |= FLAG_RON;
    }

    /**
     * 发送操作选项（多次调用只发送一次）
     */
    public abstract void sendOperate();

    /**
     * 打断操作
     */
    public abstract void interruptOperate();

    /**
     * 等待荣和
     *
     * @return 如果该玩家决定荣和，则返回 true，否则返回 false
     */
    public abstract boolean waitForRon();

    /**
     * 等待碰操作
     * 该方法会同时更改手牌结构以完成操作
     *
     * @return 如果该玩家决定碰，返回 1；决定杠，返回 2；否则，返回 0
     */
    public abstract int waitForPeng();

    /**
     * 等待吃操作
     * 该方法会同时更改手牌结构以完成操作
     *
     * @return 如果该玩家决定吃，返回 true
     */
    public abstract boolean waitForChi();

    /**
     * 是否需要将打出的牌横放
     * 如果立直宣言牌被他家鸣牌，则下一张打出的牌需要横放
     *
     * @return 如果需要，则返回 true
     */
    public boolean needPlayAsRichi() {
        if (riChiType == Mahjong.RiChiType.None) {
            return false;
        }
        for (Mahjong.Shezhang shezhang : shezhang) {
            if (shezhang.status == Mahjong.Shezhang.Status.RiChi) {
                return false;
            }
        }
        return true;
    }

    /**
     * 分析听牌
     */
    public void analyseTingpai() {
        ArrayList<Mahjong.ListenResult> results = Mahjong.checkListen(plate, null, new Mahjong.Environment(), riChiType);
        if (results == null) {
            tingpai = null;
            return;
        }
        tingpai = new ArrayList<>();
        for (Mahjong.ListenResult result : results) {
            if (!tingpai.contains(result.waitFor)) {
                tingpai.add(result.waitFor);
            }
        }
    }

    public static class Action {
        public final ActionType type;
        public final Mahjong.Card outCard;

        public Action(ActionType type, Mahjong.Card outCard) {
            this.type = type;
            this.outCard = outCard;
        }
    }

    public enum ActionType {
        // 流局（九种九牌）
        // out card = null
        KSKH,
        // 出牌
        // out card = 出的牌
        Play,
        // 暗杠
        // out card = 暗杠的牌中的一个
        // 由于只有国士无双才能抢暗杠，而且国士无双抢的一定是幺九牌，不可能包含红宝牌，因此可以简单地返回其中一个
        // （而且说起来国士无双了就不算红宝牌了）
        AnGang,
        // 加杠
        // out card = 补杠的牌
        JiaGang,
        // 拔北
        // out card = 北
        Bei,
        // 立直
        // out card = 立直宣言牌
        RiChi,
        // 自摸
        // out card = null
        TsuMmo,
        // 吃
        Chi,
        // 碰
        Pong,
        // 大明杠
        Kong,
        // 荣和
        Ron
    }
}
