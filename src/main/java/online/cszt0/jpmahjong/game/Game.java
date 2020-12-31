package online.cszt0.jpmahjong.game;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Game {
    private static final Map<String, Game> gameSet = Collections.synchronizedMap(new HashMap<>());

    /**
     * 房间 id
     */
    private final String id;
    /**
     * 主逻辑线程
     */
    private final Thread thread;

    /**
     * 玩家改变（加入或退出）锁
     * 默认情况下，游戏逻辑线程持有该锁，当且仅当允许玩家加入时才会释放
     */
    private final Lock playerChangeLock = new ReentrantLock();
    /**
     * 玩家改变条件变量
     * 当玩家改变时，需要唤醒在条件变量上等待的线程（即游戏主逻辑线程）
     */
    private final Condition playerChangeCondition = playerChangeLock.newCondition();

    /**
     * 玩家列表
     */
    private final List<Player> players = new ArrayList<>();

    /**
     * （对局时）随机决定的玩家顺序
     */
    private final List<Player> playerOrder = new ArrayList<>();

    /**
     * 玩家数量
     * 根据规则不同，该值可能为 2~4
     */
    private final int playerCount;
    /**
     * All Last 场风
     */
    private final int allLastChangfeng;

    /**
     * 场风
     */
    private int changfeng;
    /**
     * 第几场
     * 该项决定了本局东风家
     */
    private int chang;
    /**
     * 本场数
     */
    private int benchang;
    /**
     * 立直棒数
     */
    private int richi;

    /**
     * 现在是谁的回合
     */
    private int turn;
    /**
     * 是否摸岭上牌
     * 杠、拔北时，应当摸岭上牌
     */
    private boolean fromLingshang;
    /**
     * 是否应当摸牌
     * 吃、碰时，不应当再从牌山中摸牌
     */
    private boolean mopai;
    /**
     * 首巡标记
     * 用于两立直、四风连打、九种九牌、天和、地和判断
     * 有人鸣牌时，该标记立刻清除
     */
    private boolean firstXun;
    /**
     * 是否可以杠、拔北等操作
     * 除庄家首巡外，其余情况在吃、碰时不允许杠、拔北
     */
    private boolean canGang;
    /**
     * 需要翻开一张宝牌
     * 暗杠时，立即翻开宝牌，因此无需此属性
     * 明杠时，在操作后才会翻开宝牌
     */
    private boolean showDora;

    /**
     * 流局标记：九种九牌、三家和了
     */
    private boolean kskh, sjhp;
    /**
     * 杠牌数量
     */
    private int gangCount;

    /**
     * 牌山
     */
    private Mahjong.Paishan paishan;

    /**
     * 游戏继续标记
     * 当达成以下条件时游戏终止：
     * 1. 有人飞了
     * 2. All Last 时，庄家 1 位且达到最大点数
     * 3. All Last 时，庄家下庄且 1 位达到最大点数
     * 4. 加时赛中，庄家 1 位且达到最大点数
     * 5. 加时赛中，庄家下庄且 1 位达到最大点数
     * 6. 加时赛结束
     */
    private boolean gameOngoing;

    /**
     * 构建并初始化 Game 对象
     */
    public Game() {
        playerCount = 4;
        allLastChangfeng = 2;
        synchronized (this) {
            String id;
            synchronized (gameSet) {
                do {
                    id = randomKey();
                } while (gameSet.containsKey(id));
                gameSet.put(id, this);
            }
            this.id = id;

            thread = new Thread(this::run);
            thread.start();
            // 等待线程初始化完成
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 生成随机 key
     *
     * @return key
     */
    private static String randomKey() {
        String id;
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int n = random.nextInt(10 + 26 + 26);
            if (n < 10) {
                builder.append((char) ('0' + n));
                continue;
            }
            n -= 10;
            if (n < 26) {
                builder.append((char) ('a' + n));
                continue;
            }
            n -= 26;
            builder.append((char) ('A' + n));
        }
        id = builder.toString();
        return id;
    }

    public String getId() {
        return id;
    }

    private void run() {
        synchronized (this) {
            playerChangeLock.lock();
            notify();
        }
        try {
            while (!Thread.interrupted()) {
                // 等待玩家加入
                if (!waitForPlayers()) {
                    return;
                }
                // 开局
                newGame();
                gameOngoing = true;
                while (gameOngoing) {
                    // 一局开始
                    newMatch();
                    // 主逻辑循环
                    while (tickMatch()) {
                        // 流局判定
                        // 四风连打
                        if (firstXun && playerCount == 4) {
                            int[] feng = new int[4];
                            for (int i = 0; i < playerCount; i++) {
                                Player p = playerOrder.get(i);
                                if (p.shezhang.size() == 0) {
                                    break;
                                }
                                Mahjong.Card card = p.shezhang.get(0).card;
                                if (card.isFeng()) {
                                    feng[i] = card.getNumber();
                                }
                            }
                            if (feng[0] == feng[1] && feng[0] == feng[2] && feng[0] == feng[3] && feng[0] != 0) {
                                nextMatch(0);
                                // 通知玩家
                                for (Player p : playerOrder) {
                                    p.onSiFengLianDa();
                                }
                                break;
                            }
                        }
                        // 四家立直
                        if (playerCount == 4) {
                            int richiCount = 0;
                            for (Player p : playerOrder) {
                                if (p.riChiType != Mahjong.RiChiType.None) {
                                    richiCount++;
                                }
                            }
                            if (richiCount == 4) {
                                nextMatch(0);
                                // 通知玩家
                                for (Player p : playerOrder) {
                                    p.onSiJiaLiZhi();
                                }
                                break;
                            }
                        }
                        // 四杠散了
                        if (gangCount == 4 && !fromLingshang) {
                            int gangPlayerCount = 0;
                            for (Player p : playerOrder) {
                                for (Mahjong.Fulu fulu : p.plate.fulus) {
                                    if (fulu.isGang()) {
                                        gangPlayerCount++;
                                        break;
                                    }
                                }
                            }
                            if (gangPlayerCount > 1) {
                                nextMatch(0);
                                // 通知玩家
                                for (Player p : playerOrder) {
                                    p.onSiGangSanLe();
                                }
                                break;
                            }
                        }
                        // 九种九牌、三家和
                        if (kskh) {
                            nextMatch(0);
                            // 通知玩家
                            for (Player p : playerOrder) {
                                p.onPlayerKskh();
                            }
                            break;
                        }
                        if (sjhp) {
                            nextMatch(0);
                            // 通知玩家
                            for (Player p : playerOrder) {
                                p.onSanJiaHu();
                            }
                            break;
                        }
                        // 荒牌流局
                        if (paishan.isHaidi()) {
                            // 通知玩家
                            // TODO: 包含听牌信息
                            for (Player p : playerOrder) {
                                p.onHuangPaiLiuJu();
                            }
                            // 听牌
                            ArrayList<Player> tingpaiPlayers = new ArrayList<>();
                            ArrayList<Player> notingPlayers = new ArrayList<>();
                            for (Player p : playerOrder) {
                                if (p.tingpai != null) {
                                    tingpaiPlayers.add(p);
                                    p.showPlate = true;
                                } else {
                                    notingPlayers.add(p);
                                }
                            }
                            // 流满
                            ArrayList<Player> liumanPlayer = new ArrayList<>();
                            for (Player p : playerOrder) {
                                if (Mahjong.isLiuman(p.shezhang)) {
                                    liumanPlayer.add(p);
                                }
                            }
                            if (!liumanPlayer.isEmpty()) {
                                ArrayList<Boolean> zhuangs = new ArrayList<>();
                                for (Player p : liumanPlayer) {
                                    zhuangs.add(getPlayerMenfeng(p) == Mahjong.Feng.Dong);
                                }
                                // 通知玩家
                                for (Player p : playerOrder) {
                                    p.onLiuJuManGuan(liumanPlayer, zhuangs);
                                }
                                for (Player p : liumanPlayer) {
                                    if (getPlayerMenfeng(p) == Mahjong.Feng.Dong) {
                                        p.pointChange += 12000;
                                        for (Player pp : playerOrder) {
                                            if (pp != p) {
                                                pp.pointChange -= 4000;
                                            }
                                        }
                                    } else {
                                        p.pointChange += 8000;
                                        for (Player pp : playerOrder) {
                                            if (pp != p) {
                                                if (getPlayerMenfeng(pp) == Mahjong.Feng.Dong) {
                                                    pp.pointChange -= 4000;
                                                } else {
                                                    pp.pointChange -= 2000;
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // 罚符
                                int count = tingpaiPlayers.size();
                                if (count != 0 && count != playerCount) {
                                    int fafu = playerCount * 1000;
                                    for (Player p : tingpaiPlayers) {
                                        p.pointChange += fafu / tingpaiPlayers.size();
                                    }
                                    for (Player p : notingPlayers) {
                                        p.pointChange -= fafu / notingPlayers.size();
                                    }
                                }
                            }
                            applyPlayerPointChange();
                        }
                    }
                    // 一局结束
                    endMatch();
                }
                // 终局结算
                endGame();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 新游戏初始化
     */
    private void newGame() {
        // 决定玩家顺序
        assert playerOrder.isEmpty();
        playerOrder.addAll(players);
        Collections.shuffle(playerOrder);
        // 依次初始化
        for (Player player : playerOrder) {
            player.newGame();
        }
        // 初始化场风、本场等
        changfeng = 1;
        chang = 1;
        benchang = 0;
        richi = 0;
        // 发送新对局消息
        for (int i = 0; i < playerCount; i++) {
            playerOrder.get(i).onGameStart(playerOrder, i);
        }
    }

    /**
     * 终局结算
     */
    private void endGame() {
        // 计算终局信息并通知玩家
        playerOrder.sort((o1, o2) -> o1.point - o2.point);
        playerOrder.get(0).point += richi * 1000;
        for (Player p : playerOrder) {
            p.onGameEnd(playerOrder);
        }
        for (Player player : players) {
            player.ready = false;
        }
    }

    private void newMatch() {
        // 玩家初始化
        for (Player player : playerOrder) {
            player.newMatch();
        }
        // 生成牌山
        paishan = Mahjong.Paishan.generate();

        // 配牌
        // Step 1: 循环 3 次，每人拿 4 张，每人共 12 张
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < playerCount; j++) {
                Player player = getMenfengPlayer(Mahjong.Feng.values()[j]);
                for (int k = 0; k < 4; k++) {
                    player.plate.cards.add(paishan.nextCard());
                }
            }
        }
        // Step 2: 每人再拿一张，现在每人共 13 张
        for (int i = 0; i < playerCount; i++) {
            Player player = getMenfengPlayer(Mahjong.Feng.values()[i]);
            player.plate.cards.add(paishan.nextCard());
        }
        // Step 3: 庄家拿一张，庄家现在 14 张，其他人 13 张
        getMenfengPlayer(Mahjong.Feng.Dong).plate.cards.add(paishan.nextCard());

        // 重置回合计数
        turn = 0;
        fromLingshang = false;
        firstXun = true;
        mopai = false;  // 庄家不再摸牌
        canGang = true;
        showDora = false;
        kskh = false;
        sjhp = false;

        // 检查听牌（万一有人听牌了呢）
        for (Player player : playerOrder) {
            player.analyseTingpai();
        }

        // 发送配牌信息
        for (Player player : playerOrder) {
            player.onMatchStart(getPlayerMenfeng(player));
        }
    }

    /**
     * 根据门风获取玩家
     *
     * @param feng 门风
     * @return 玩家
     */
    private Player getMenfengPlayer(Mahjong.Feng feng) {
        int num = feng.number;
        int index = chang + num - 2;
        index %= playerCount;
        return playerOrder.get(index);
    }

    /**
     * 根据玩家获取门风
     *
     * @return 门风
     */
    private Mahjong.Feng getPlayerMenfeng(Player player) {
        int index = playerOrder.indexOf(player);
        index += chang - 1;
        index %= playerCount;
        return Mahjong.Feng.values()[index];
    }

    private boolean tickMatch() {
        Player player = getMenfengPlayer(Mahjong.Feng.values()[turn]);

        // 如果舍张有牌，清除第一巡标记
        if (!player.shezhang.isEmpty()) {
            firstXun = false;
        }

        // 玩家摸牌
        if (mopai) {
            Mahjong.Card card;
            if (fromLingshang) {
                card = paishan.nextLingshangCard();
            } else {
                card = paishan.nextCard();
            }
            player.justObtain = card;
            // 通知玩家摸牌
            for (Player p : playerOrder) {
                p.onCardDraw(player, turn, paishan.getRemain());
            }
        } else {
            player.justObtain = null;
        }

        // 环境
        Mahjong.Environment env = new Mahjong.Environment();
        env.changfeng = getChangfeng();
        env.tianhu = turn == 0 && firstXun;
        env.dihu = turn != 0 && firstXun;
        paishan.fillDora(env);

        // 等待玩家操作
        // 九种九牌、出牌、暗杠、加杠、拔北、立直、自摸
        Player.Action action = player.waitForAction(firstXun, canGang);
        // 处理九种九牌和自摸
        // 其他情况其他家可能会有操作，因此不能立即结算
        switch (action.type) {
            case KSKH: {
                // 九种九牌
                kskh = true;
                player.showPlate = true;
                return true;
            }
            case TsuMmo: {
                // 自摸
                assert mopai;
                player.showPlate = true;
                // 处理和牌
                env.menfeng = getPlayerMenfeng(player);
                Mahjong.CardSource source;
                if (fromLingshang) {
                    source = Mahjong.CardSource.LingShang;
                } else if (paishan.isHaidi()) {
                    source = Mahjong.CardSource.HaiDi;
                } else {
                    source = Mahjong.CardSource.ZiMo;
                }
                // 役种计算
                Mahjong.WinResult result = Mahjong.checkWin(player.plate, player.justObtain, env, source, player.riChiType, player.ihatsu);
                assert result != null;
                // 点数计算
                boolean zhuang = env.menfeng == Mahjong.Feng.Dong;
                Mahjong.PointResult point = Mahjong.computePoint(result.fan, result.fu, zhuang);
                player.pointChange = richi;
                for (int i = 0; i < playerCount; i++) {
                    Player p = getMenfengPlayer(Mahjong.Feng.values()[i]);
                    if (p != player) {
                        int v = 100 * benchang;
                        if (i == 0) {
                            v += point.qinjia;
                        } else {
                            v += point.zijia;
                        }
                        p.pointChange = -v;
                        player.pointChange += v;
                    }
                }
                applyPlayerPointChange();
                // 通知玩家
                for (Player p : playerOrder) {
                    p.onPlayerZimo(player, turn, action.outCard, result, point, zhuang);
                }
                // 清空立直棒，判断连庄
                richi = 0;
                if (zhuang) {
                    nextMatch(0);
                } else {
                    nextMatch(2);
                }
                return false;
            }
        }

        // 展示新宝牌
        if (showDora) {
            paishan.doraCount++;
            // 通知玩家
            for (Player p : playerOrder) {
                p.onNewDora(paishan.lastDoraPointer());
            }
        }

        // 清除标记
        mopai = true;
        canGang = true;
        fromLingshang = false;
        showDora = false;

        // 添加舍张
        Mahjong.Shezhang shezhang = null;
        if (action.type == Player.ActionType.Play || action.type == Player.ActionType.RiChi) {
            shezhang = new Mahjong.Shezhang(action.outCard);
            if (action.type == Player.ActionType.RiChi || player.needPlayAsRichi()) {
                shezhang.status = Mahjong.Shezhang.Status.RiChi;
            }
            player.shezhang.add(shezhang);
            // 通知玩家
            for (Player p : playerOrder) {
                // TODO: 摸切标记
                p.onCardPlay(player, turn, action.outCard, false);
            }
            if (action.type == Player.ActionType.RiChi) {
                for (Player p : playerOrder) {
                    p.onPlayerRichiDeclear(player, turn, firstXun);
                }
            }
        }

        // 通知玩家副露
        // TODO: 副露类型
        if (action.type == Player.ActionType.JiaGang || action.type == Player.ActionType.AnGang || action.type == Player.ActionType.Bei) {
            for (Player p : playerOrder) {
                p.onPlayerFulu(player, turn);
            }
        }

        // 听牌、振听分析
        if (player.riChiType == Mahjong.RiChiType.None) {
            player.zhenting = false;
            player.analyseTingpai();
            if (player.tingpai != null) {
                for (Mahjong.Shezhang sz : player.shezhang) {
                    if (player.tingpai.contains(sz.card.asIgnoreRedDora())) {
                        player.zhenting = true;
                        break;
                    }
                }
            }
            if (player.zhenting) {
                player.onZhenTing();
            }
        }

        // 一发标记
        player.ihatsu = false;

        // 查询可操作玩家
        List<Player> huPlayers = new ArrayList<>();
        Player pengPlayer = null;
        Player chiPlayer = null;

        // 荣和判断
        Mahjong.CardSource cardSource;
        switch (action.type) {
            case Play:
            case RiChi:
                if (paishan.isHaidi()) {
                    cardSource = Mahjong.CardSource.HeDi;
                } else {
                    cardSource = Mahjong.CardSource.HaiDi;
                }
                break;
            case AnGang:
            case JiaGang:
            case Bei:
                cardSource = Mahjong.CardSource.QiangGang;
                break;
            default:
                cardSource = null;
                assert false;
                break;
        }
        // 寻找可和牌玩家
        for (int i = 0; i < playerOrder.size(); i++) {
            // 因为和牌时只有一人能拿到积棒和供托，因此需要保证顺序
            Player p = playerOrder.get((chang + i - 1) % playerCount);
            // 是否听牌，且听这张牌
            if (p != player && !p.zhenting && p.tingpai != null && p.tingpai.contains(action.outCard.asIgnoreRedDora())) {
                // 检查役种
                env.menfeng = getPlayerMenfeng(p);
                Mahjong.WinResult result = Mahjong.checkWin(p.plate, action.outCard, env, cardSource, p.riChiType, p.ihatsu);
                assert result != null;
                // 暗杠且非国士无双，忽略
                if (action.type == Player.ActionType.AnGang) {
                    boolean canRon = false;
                    for (Mahjong.YiZhong yi : result.yiZhongs) {
                        // 此处一定不可能是国士十三面
                        if (yi.yiZhongEnumerate == Mahjong.YiZhongEnumerate.GuoShiWuShuang) {
                            canRon = true;
                            break;
                        }
                    }
                    if (!canRon) {
                        continue;
                    }
                }
                // 无役，振听
                boolean hasYi = false;
                for (Mahjong.YiZhong yi : result.yiZhongs) {
                    if (!yi.notYi) {
                        hasYi = true;
                        break;
                    }
                }
                if (!hasYi) {
                    p.zhenting = true;
                    continue;
                }
                // 询问
                huPlayers.add(p);
            }
        }

        // 吃、碰判断
        if (action.type == Player.ActionType.Play || action.type == Player.ActionType.RiChi) {
            // 碰
            for (Player p : playerOrder) {
                if (p == player) continue;
                if (p.plate.canPeng(action.outCard)) {
                    if (p.riChiType == Mahjong.RiChiType.None) {
                        pengPlayer = p;
                    }
                    break;
                }
            }
            // 吃
            Player p = getMenfengPlayer(Mahjong.Feng.values()[(turn + 1) % playerCount]);
            if (p.riChiType == Mahjong.RiChiType.None && p.plate.canChi(action.outCard)) {
                chiPlayer = p;
            }
        }

        // 通知玩家
        for (Player p : huPlayers) {
            p.initOperate(action.outCard, turn);
        }
        if (pengPlayer != null) {
            pengPlayer.initOperate(action.outCard, turn);
        }
        if (chiPlayer != null) {
            chiPlayer.initOperate(action.outCard, turn);
        }
        for (Player p : huPlayers) {
            p.operateCanRon();
        }
        if (pengPlayer != null) {
            pengPlayer.operateCanPeng();
        }
        if (chiPlayer != null) {
            chiPlayer.operateCanChi();
        }
        for (Player p : huPlayers) {
            p.sendOperate();
        }
        if (pengPlayer != null) {
            pengPlayer.sendOperate();
        }
        if (chiPlayer != null) {
            chiPlayer.sendOperate();
        }

        // 需要改变回合
        boolean changeTurn = true;

        // 已经有玩家行动
        boolean hasAction = false;

        // 荣和
        ArrayList<Player> decideHuPlayers = new ArrayList<>();
        for (Player p : huPlayers) {
            if (p.waitForRon()) {
                decideHuPlayers.add(p);
            }
        }
        if (!decideHuPlayers.isEmpty()) {
            for (Player p : decideHuPlayers) {
                p.showPlate = true;
            }
            hasAction = true;
            if (pengPlayer != null) pengPlayer.interruptOperate();
            if (chiPlayer != null) chiPlayer.interruptOperate();
            if (decideHuPlayers.size() == 3) {
                // 三家和流局
                sjhp = true;
                return true;
            }
            // 逐个役种、点数计算
            ArrayList<Player> players = new ArrayList<>();
            ArrayList<Integer> playerIndex = new ArrayList<>();
            ArrayList<Mahjong.WinResult> winResults = new ArrayList<>();
            ArrayList<Mahjong.PointResult> pointResults = new ArrayList<>();
            ArrayList<Boolean> zhuangs = new ArrayList<>();
            boolean first = true;
            boolean hasZhuang = false;
            for (Player p : decideHuPlayers) {
                env.menfeng = getPlayerMenfeng(p);
                Mahjong.WinResult result = Mahjong.checkWin(p.plate, action.outCard, env, cardSource, p.riChiType, p.ihatsu);
                assert result != null;
                boolean zhuang = env.menfeng == Mahjong.Feng.Dong;
                hasZhuang |= zhuang;
                Mahjong.PointResult point = Mahjong.computePoint(result.fan, result.fu, zhuang);
                p.pointChange = point.fangchong;
                player.pointChange -= point.fangchong;
                if (first) {
                    first = false;
                    p.pointChange += richi + 100 * playerCount * benchang;
                    player.pointChange -= 100 * playerCount * benchang;
                }
                players.add(p);
                playerIndex.add(playerOrder.indexOf(p));
                winResults.add(result);
                pointResults.add(point);
                zhuangs.add(zhuang);
            }
            applyPlayerPointChange();
            // 通知玩家
            for (Player p : playerOrder) {
                p.onPlayerRong(players, playerIndex, action.outCard, winResults, pointResults, zhuangs);
            }
            // 清空立直棒，判断连庄
            richi = 0;
            if (hasZhuang) {
                nextMatch(0);
            } else {
                nextMatch(2);
            }
        }

        // 碰
        if (pengPlayer != null && !hasAction) {
            int result = pengPlayer.waitForPeng();
            if (result != 0) {
                hasAction = true;
                if (chiPlayer != null) chiPlayer.interruptOperate();
                if (result == 1) {   // 碰
                    mopai = false;
                    canGang = false;
                } else {    // 杠
                    mopai = true;
                    canGang = true;
                    fromLingshang = true;
                    showDora = true;
                    gangCount++;
                }
                turn = playerOrder.indexOf(pengPlayer);
                changeTurn = false;
                if (shezhang != null) {
                    shezhang.status = Mahjong.Shezhang.Status.Obtained;
                }
                mingpai();
                // 通知玩家副露
                // TODO: 副露类型
                for (Player p : playerOrder) {
                    p.onPlayerFulu(pengPlayer, playerOrder.indexOf(pengPlayer));
                }
            }
        }

        // 吃
        if (chiPlayer != null && !hasAction) {
            boolean result = chiPlayer.waitForChi();
            if (result) {
                mopai = false;
                turn = playerOrder.indexOf(chiPlayer);
                changeTurn = false;
                if (shezhang != null) {
                    shezhang.status = Mahjong.Shezhang.Status.Obtained;
                }
                mingpai();
                // 通知玩家副露
                // TODO: 副露类型
                for (Player p : playerOrder) {
                    p.onPlayerFulu(chiPlayer, playerOrder.indexOf(chiPlayer));
                }
            }
        }

        // 结算杠、立直
        if (action.type == Player.ActionType.AnGang) {
            fromLingshang = true;
            paishan.doraCount++;
            gangCount++;
            mingpai();
            // 告知玩家宝牌
            for (Player p : playerOrder) {
                p.onNewDora(paishan.lastDoraPointer());
            }
        } else if (action.type == Player.ActionType.JiaGang) {
            fromLingshang = true;
            showDora = true;
            gangCount++;
            mingpai();
        } else if (action.type == Player.ActionType.RiChi) {
            if (firstXun) {
                player.riChiType = Mahjong.RiChiType.WRiChi;
            } else {
                player.riChiType = Mahjong.RiChiType.RiChi;
            }
            player.ihatsu = true;
            player.point -= 1000;
            richi++;
        } else if (action.type == Player.ActionType.Bei) {
            fromLingshang = true;
            mingpai();
        }

        // 改变回合
        if (changeTurn) {
            turn++;
            turn %= playerCount;
        }

        return true;
    }

    /**
     * 应用玩家点数更改
     */
    private void applyPlayerPointChange() {
        for (Player player : playerOrder) {
            player.point += player.pointChange;
        }
        // 通知玩家
        for (Player p : playerOrder) {
            p.onPointChange();
        }
    }

    private void endMatch() {
    }

    /**
     * 下一局
     *
     * @param type 类型。0：连庄，1：不连庄，继承本场数，2：不连庄，不继承本场数
     */
    private void nextMatch(int type) {
        switch (type) {
            case 0: {
                benchang++;
                // All Last 时，庄 1 位且达到最大点数，结束
                if (isAllLast()) {
                    Player top = getTop();
                    Player dong = getMenfengPlayer(Mahjong.Feng.Dong);
                    if (top == dong && top.point >= 30000) {
                        gameOngoing = false;
                    }
                }
                break;
            }
            case 2: {
                benchang = 0;
                // no break
            }
            case 1: {
                chang++;
                if (chang == playerCount) {
                    chang = 1;
                    changfeng++;
                    if (changfeng == allLastChangfeng + 2) {
                        gameOngoing = false;
                    }
                }
                if (isAllLast()) {
                    Player top = getTop();
                    if (top.point >= 30000) {
                        gameOngoing = false;
                    }
                }
                break;
            }
        }
    }

    /**
     * 判断是否已经 All Last
     *
     * @return 如果已经 All Last，则返回 true
     */
    private boolean isAllLast() {
        return changfeng == allLastChangfeng && chang == playerCount || changfeng > allLastChangfeng;
    }

    /**
     * 获取 1 位玩家
     *
     * @return 1 位玩家
     */
    private Player getTop() {
        Player player = playerOrder.get(0);
        for (Player p : playerOrder) {
            if (p.point > player.point) {
                player = p;
            }
        }
        return player;
    }

    /**
     * 获取场风
     *
     * @return 场风
     */
    private Mahjong.Feng getChangfeng() {
        // FIXME: 三人麻将中，西风圈打完后的加时赛应该是北风还是东风？同理，二人麻将应该是哪一个？
        return Mahjong.Feng.values()[(changfeng - 1) % playerCount];
    }

    /**
     * 鸣牌处理
     */
    private void mingpai() {
        // 清除首巡标记
        firstXun = false;
        // 清除一发标记
        for (Player player : playerOrder) {
            player.ihatsu = false;
        }
    }

    /**
     * 等待玩家准备
     * 在准备完成之前，该方法将阻塞线程
     *
     * @return 如果人数足够且均已准备，返回 true；
     * 如果房间内没有人，返回 false；
     * 否则阻塞，直到出现上述两种情况
     * @throws InterruptedException 如果在等待过程中被中断
     */
    private boolean waitForPlayers() throws InterruptedException {
        while (!Thread.interrupted()) {
            if (players.isEmpty()) {
                return false;
            }
            int readyCount = 0;
            for (Player player : players) {
                if (player.ready) {
                    readyCount++;
                }
            }
            if (readyCount == playerCount) {
                return true;
            }
            playerChangeCondition.await();
        }
        throw new InterruptedException();
    }

    /**
     * 加入游戏
     *
     * @param player 玩家
     * @return 如果成功加入，返回 true，否则，返回 false
     */
    public synchronized boolean playerJoin(Player player) {
        try {
            if (!playerChangeLock.tryLock(1, TimeUnit.SECONDS)) {
                return false;
            }
            players.add(player);
            playerChangeCondition.signal();
            playerChangeLock.unlock();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 退出游戏
     *
     * @param player 玩家
     * @return 如果成功退出，返回 true，否则，返回 false
     */
    public synchronized boolean playerExit(Player player) {
        try {
            if (!playerChangeLock.tryLock(1, TimeUnit.SECONDS)) {
                return false;
            }
            if (players.remove(player)) {
                playerChangeCondition.signal();
            }
            playerChangeLock.unlock();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
