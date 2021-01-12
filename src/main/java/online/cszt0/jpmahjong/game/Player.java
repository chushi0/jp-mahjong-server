package online.cszt0.jpmahjong.game;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家
 */
public abstract class Player {
    /**
     * 玩家名
     */
    public String name;
    /**
     * 加入的游戏
     */
    public Game game;
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
     * 刚刚摸到的牌可以自摸
     */
    public boolean canZimo;
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
    protected static final int FLAG_SEND = 0x80000000;
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
     * 等待玩家操作。立直后，若无特殊情况，则自动摸切
     *
     * @param firstXun 是否是首巡（无人鸣牌的第一巡）
     * @param canGang  是否可以杠
     * @return 玩家操作
     */
    public Action waitForAction(boolean firstXun, boolean canGang) throws InterruptedException {
        if (riChiType != Mahjong.RiChiType.None) {
            if (!((canGang && plate.canAnGang(justObtain, true)) || tingpai.contains(justObtain.asIgnoreRedDora()))) {
                // 延迟 1.5 秒，防止执行速度过快
                Thread.sleep(1500);
                return new Action(ActionType.Play, justObtain, true);
            }
        }
        return waitForPlayerAction(firstXun, canGang);
    }

    protected abstract Action waitForPlayerAction(boolean firstXun, boolean canGang) throws InterruptedException;

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
    public void sendOperate() {
        assert flag != FLAG_NONE;
        if ((flag & FLAG_SEND) == 0) {
            flag |= FLAG_SEND;
            onOperateSend();
        }
    }

    protected abstract void onOperateSend();

    /**
     * 打断操作
     */
    public abstract void interruptOperate();

    /**
     * 等待荣和
     *
     * @return 如果该玩家决定荣和，则返回 true，否则返回 false
     */
    public abstract boolean waitForRon() throws InterruptedException;

    /**
     * 等待碰操作
     * 该方法会同时更改手牌结构以完成操作
     *
     * @return 如果该玩家决定碰，返回 1；决定杠，返回 2；否则，返回 0
     */
    public abstract int waitForPeng() throws InterruptedException;

    /**
     * 等待吃操作
     * 该方法会同时更改手牌结构以完成操作
     *
     * @return 如果该玩家决定吃，返回 true
     */
    public abstract boolean waitForChi() throws InterruptedException;

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
        public final boolean moqie;

        public Action(ActionType type, Mahjong.Card outCard, boolean moqie) {
            this.type = type;
            this.outCard = outCard;
            this.moqie = moqie;
        }

        public Action(ActionType type, Mahjong.Card outCard) {
            this(type, outCard, false);
        }

        public Action(ActionType type) {
            this(type, null, false);
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

    /**
     * 游戏开始事件
     *
     * @param playerList 玩家列表
     * @param myIndex    我的下标
     */
    public abstract void onGameStart(List<Player> playerList, int myIndex);

    /**
     * 新一局开始
     *
     * @param menfeng 我的门风
     */
    public abstract void onMatchStart(Mahjong.Feng menfeng);

    /**
     * 玩家摸牌
     *
     * @param player      摸牌的玩家
     * @param playerIndex 摸牌的玩家下标
     * @param remain      余牌数量
     */
    public abstract void onCardDraw(Player player, int playerIndex, int remain);

    /**
     * 玩家切牌
     *
     * @param player      切牌的玩家
     * @param playerIndex 切牌的玩家下标
     * @param card        切的牌
     * @param moqie       是否是摸切
     */
    public abstract void onCardPlay(Player player, int playerIndex, Mahjong.Card card, boolean moqie);

    /**
     * 玩家副露（吃、碰、杠（大明杠、加杠、暗杠）、拔北）
     *
     * @param player      副露的玩家
     * @param playerIndex 副露的玩家下标
     */
    public abstract void onPlayerFulu(Player player, int playerIndex);

    /**
     * 玩家立直宣言
     *
     * @param player      立直的玩家
     * @param playerIndex 立直的玩家下标
     * @param wrichi      是否是两立直
     */
    public abstract void onPlayerRichiDeclear(Player player, int playerIndex, boolean wrichi);

    /**
     * 玩家自摸
     *
     * @param player      自摸的玩家
     * @param playerIndex 自摸的玩家下标
     * @param winResult   番、符、役种
     * @param pointResult 点数
     * @param zhuang      是否是庄
     */
    public abstract void onPlayerZimo(Player player, int playerIndex, Mahjong.Card card, Mahjong.WinResult winResult, Mahjong.PointResult pointResult, boolean zhuang);

    /**
     * 玩家荣和
     *
     * @param player      荣和的玩家
     * @param playerIndex 荣和的玩家下标
     * @param winResult   番、符、役种
     * @param pointResult 点数
     * @param zhuang      是否是庄
     */
    public abstract void onPlayerRong(List<Player> player, List<Integer> playerIndex, Mahjong.Card card, List<Mahjong.WinResult> winResult, List<Mahjong.PointResult> pointResult, List<Boolean> zhuang);

    /**
     * 九种九牌流局
     */
    public abstract void onPlayerKskh();

    /**
     * 三家和流局
     */
    public abstract void onSanJiaHu();

    /**
     * 荒牌流局
     */
    public abstract void onHuangPaiLiuJu();

    /**
     * 四家立直流局
     */
    public abstract void onSiJiaLiZhi();

    /**
     * 四杠散了流局
     */
    public abstract void onSiGangSanLe();

    /**
     * 四风连打流局
     */
    public abstract void onSiFengLianDa();

    /**
     * 流局满贯
     *
     * @param players 流局满贯的玩家
     * @param zhuang  是否是庄
     */
    public abstract void onLiuJuManGuan(List<Player> players, List<Boolean> zhuang);

    /**
     * 分数结算
     */
    public abstract void onPointChange();

    /**
     * 终局
     *
     * @param order 位次
     */
    public abstract void onGameEnd(List<Player> order);

    /**
     * 新的宝牌
     *
     * @param pointer 新的宝牌指示牌
     */
    public abstract void onNewDora(Mahjong.Card pointer);

    /**
     * 振听
     */
    public abstract void onZhenTing();
}
