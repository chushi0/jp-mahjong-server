package online.cszt0.jpmahjong.socket;

import online.cszt0.jpmahjong.game.Mahjong;
import online.cszt0.jpmahjong.game.Player;
import online.cszt0.jpmahjong.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ServerEndpoint("/game")
@Component
public class GamePlayerSocket {

    private static final Map<String, SocketPlayer> playerSet = Collections.synchronizedMap(new WeakHashMap<>());

    private Session session;
    private SocketPlayer currentPlayer;

    private static final String KEY_ACTION = "action";
    private static final String KEY_DATA = "data";

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        session.setMaxIdleTimeout(60000);
    }

    @OnClose
    public synchronized void onClose() {
        session = null;
        if (currentPlayer != null) {
            currentPlayer.socket = null;
        }
    }

    @OnMessage
    public synchronized void onMessage(String message) {
        try {
            JSONObject object = new JSONObject(message);
            String action = object.getString(KEY_ACTION);
            JSONObject data = object.optJSONObject(KEY_DATA);
            switch (action) {
                case "create_room": // 创建房间
                case "join_room":   // 加入房间
                case "connect_player":  // 重连玩家（断线重连）
                case "random_join": // 随机加入
                    break;
                case "heart_tick":  // 心跳
                    break;
                default: {
                    SocketPlayer player = this.currentPlayer;
                    if (player != null) {
                        player.dispatchMessage(action, data);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    private synchronized void sendMessage(String action, Map<String, Object> data) {
        if (session == null) return;
        JSONObject messageObject = new JSONObject();
        messageObject.put(KEY_ACTION, action);
        if (data != null) {
            messageObject.put(KEY_DATA, data);
        }
        String message = messageObject.toString();
        assert message != null && !message.isEmpty();
        session.getAsyncRemote().sendText(message);
    }

    /**
     * 从 WebSocket 加入的玩家
     */
    private static final class SocketPlayer extends Player {

        /**
         * 最长读秒时间
         */
        private static final int MAX_SECOND = 30;
        /**
         * 等待超时时间
         */
        private static final int TIMEOUT = 45;

        /**
         * WebSocket 对象，用于数据传输
         */
        private GamePlayerSocket socket;

        /**
         * 玩家 id，用于断线重连
         */
        private final String id = Utils.randomUniqueId(playerSet, this);

        /**
         * 是否有行动需要执行
         * 若为 true，则表示需要玩家执行一个行动
         * 若为 false，则表示目前不需要玩家执行行动
         */
        private volatile boolean action;
        /**
         * 是否有中断行动需要执行
         */
        private boolean interruptAction;
        /**
         * 中断行动执行结果
         */
        private boolean interruptResult;
        /**
         * 玩家行动
         */
        private Action playerAction;
        /**
         * 玩家执行行动锁
         */
        private final Lock actionLock = new ReentrantLock();
        /**
         * 玩家执行行动条件变量
         */
        private final Condition actionCondition = actionLock.newCondition();
        /**
         * 玩家执行行动读秒
         */
        private int second = MAX_SECOND;

        /**
         * 等待玩家操作，在玩家操作之后，或者超时后才会返回
         *
         * @return true - 玩家操作；false - 超时，此时玩家剩余读秒时间被设置为 0
         */
        private boolean waitAction() throws InterruptedException {
            boolean result = true;
            if (!action) return true;
            actionLock.lock();
            playerAction = null;
            while (action) {
                result = actionCondition.await(TIMEOUT, TimeUnit.SECONDS);
                if (!result) {
                    second = 0;
                    action = false;
                }
            }
            actionLock.unlock();
            return result;
        }

        /**
         * 等待中断行动
         */
        private boolean waitForInterruptAction() throws InterruptedException {
            if (!interruptAction) {
                interruptResult = waitAction();
            }
            interruptAction = true;
            return interruptResult;
        }

        /**
         * 如果连接可用，则向玩家发送消息数据
         *
         * @param action 动作
         * @param data   数据
         */
        private void sendMessageIfAvailable(String action, Map<String, Object> data) {
            GamePlayerSocket socket = this.socket;
            if (socket != null) {
                socket.sendMessage(action, data);
            }
        }

        @Override
        public Action waitForPlayerAction(boolean firstXun, boolean canGang) throws InterruptedException {
            action = true;
            if (waitAction()) {
                // TODO: 检查玩家行动
                assert playerAction != null;
                return playerAction;
            }
            // 超时自动摸切
            return new Action(ActionType.Play, justObtain, true);
        }

        @Override
        protected void onOperateSend() {
            action = true;
            interruptAction = false;
            Map<String, Object> data = new HashMap<>();
            data.put("flag", flag & (FLAG_CHI | FLAG_PENG | FLAG_RON));
            sendMessageIfAvailable("operate", data);
        }

        @Override
        public void interruptOperate() {
            action = false;
            sendMessageIfAvailable("operate_interrupt", null);
        }

        @Override
        public boolean waitForRon() throws InterruptedException {
            if (waitForInterruptAction()) {
                return playerAction.type == ActionType.Ron;
            }
            return false;
        }

        @Override
        public int waitForPeng() throws InterruptedException {
            if (waitForInterruptAction()) {
                // TODO: 修改副露信息
                if (playerAction.type == ActionType.Pong) {
                    return 1;
                } else if (playerAction.type == ActionType.Kong) {
                    // TODO: 检查是否可以杠
                    return 2;
                }
            }
            return 0;
        }

        @Override
        public boolean waitForChi() throws InterruptedException {
            if (waitForInterruptAction()) {
                if (playerAction.type == ActionType.Chi) {
                    // TODO: 修改副露信息
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onGameStart(List<Player> playerList, int myIndex) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Player player : playerList) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", player.name);
                list.add(map);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("players", list);
            data.put("my_index", myIndex);
            sendMessageIfAvailable("game_start", data);
        }

        @Override
        public void onMatchStart(Mahjong.Feng menfeng) {
            Map<String, Object> data = new HashMap<>();
            data.put("menfeng", menfeng.number);
            data.put("changfeng", game.getChangfeng().number);
            data.put("chang", game.getChang());
            data.put("richi", game.getRichiCount());
            data.put("benchang", game.getBenchang());
            data.put("plate", plate.toList());
            sendMessageIfAvailable("match_start", data);
        }

        @Override
        public void onCardDraw(Player player, int playerIndex, int remain) {
            Map<String, Object> data = new HashMap<>();
            data.put("index", playerIndex);
            if (player == this) {
                data.put("me", true);
                data.put("card_type", justObtain.getType().label);
                data.put("card_number", justObtain.getRawNumber());
            } else {
                data.put("me", false);
            }
            data.put("remain", remain);
            sendMessageIfAvailable("card_draw", data);
        }

        @Override
        public void onCardPlay(Player player, int playerIndex, Mahjong.Card card, boolean moqie) {
            Map<String, Object> data = new HashMap<>();
            data.put("index", playerIndex);
            data.put("me", player == this);
            data.put("moqie", moqie);
            data.put("card_type", card.getType().label);
            data.put("card_number", card.getRawNumber());
            sendMessageIfAvailable("card_play", data);
        }

        @Override
        public void onPlayerFulu(Player player, int playerIndex) {
            // TODO: 应有获取该玩家刚刚完成的副露信息的方式
        }

        @Override
        public void onPlayerRichiDeclear(Player player, int playerIndex, boolean wrichi) {
            Map<String, Object> data = new HashMap<>();
            data.put("index", playerIndex);
            data.put("wrichi", wrichi);
            sendMessageIfAvailable("richi_declear", data);
        }

        /**
         * 包装玩家和牌时的数据
         *
         * @param player      玩家
         * @param playerIndex 玩家下标
         * @param card        和的牌
         * @param winResult   和牌结算
         * @param pointResult 点数结算
         * @param point       点数
         * @return 包装后的数据
         */
        private Map<String, Object> wrapPlayerWin(Player player, int playerIndex, Mahjong.Card card, Mahjong.WinResult winResult, Mahjong.PointResult pointResult, int point) {
            Map<String, Object> data = new HashMap<>();
            data.put("index", playerIndex);
            data.put("name", player.name);
            data.put("plate", player.plate.toData());
            data.put("card_type", card.getType().label);
            data.put("card_number", card.getRawNumber());
            data.put("fan", winResult.fan);
            data.put("fu", winResult.fu);
            data.put("point", point);
            data.put("point_base", pointResult.a);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Mahjong.YiZhong yi : winResult.yiZhongs) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", yi.yiZhongEnumerate.name());
                map.put("fan", yi.fan);
                list.add(map);
            }
            data.put("yizhong", list);
            return data;
        }

        @Override
        public void onPlayerZimo(Player player, int playerIndex, Mahjong.Card card, Mahjong.WinResult winResult, Mahjong.PointResult pointResult, boolean zhuang) {
            Map<String, Object> data = wrapPlayerWin(player, playerIndex, card, winResult, pointResult, zhuang ? (pointResult.zijia * 6) : (pointResult.qinjia + pointResult.zijia * 2));
            sendMessageIfAvailable("zimo", data);
        }

        @Override
        public void onPlayerRong(List<Player> player, List<Integer> playerIndex, Mahjong.Card card, List<Mahjong.WinResult> winResult, List<Mahjong.PointResult> pointResult, List<Boolean> zhuang) {
            List<Map<String, Object>> list = new ArrayList<>();
            int size = player.size();
            assert size == playerIndex.size();
            assert size == winResult.size();
            assert size == pointResult.size();
            assert size == zhuang.size();
            for (int i = 0; i < size; i++) {
                Map<String, Object> map = wrapPlayerWin(player.get(i), playerIndex.get(i), card, winResult.get(i), pointResult.get(i), zhuang.get(i) ? (pointResult.get(i).zijia * 6) : (pointResult.get(i).qinjia + pointResult.get(i).zijia * 2));
                list.add(map);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            sendMessageIfAvailable("rong", data);
        }

        @Override
        public void onPlayerKskh() {
            Map<String, Object> data = new HashMap<>();
            data.put("show", getShowPlateData());
            sendMessageIfAvailable("liuju_kskh", data);
        }

        private List<Map<String, Object>> getShowPlateData() {
            List<Map<String, Object>> list = new ArrayList<>();
            List<Player> playerOrder = game.getPlayerOrder();
            for (int i = 0; i < playerOrder.size(); i++) {
                Player player = playerOrder.get(i);
                if (player.showPlate) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("index", i);
                    map.put("plate", player.plate.toList());
                    list.add(map);
                }
            }
            return list;
        }

        @Override
        public void onSanJiaHu() {
            Map<String, Object> data = new HashMap<>();
            data.put("show", getShowPlateData());
            sendMessageIfAvailable("liuju_sjh", data);
        }

        @Override
        public void onHuangPaiLiuJu() {
            List<Map<String, Object>> list = new ArrayList<>();
            List<Player> playerOrder = game.getPlayerOrder();
            for (int i = 0; i < playerOrder.size(); i++) {
                Player player = playerOrder.get(i);
                if (player.showPlate) {
                    List<Map<String, Object>> tingpai = new ArrayList<>();
                    for (Mahjong.Card card : player.tingpai) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("type", card.getType().label);
                        map.put("number", card.getRawNumber());
                        tingpai.add(map);
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("index", i);
                    map.put("plate", player.plate.toList());
                    map.put("tingpai", tingpai);
                    list.add(map);
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("show", list);
            sendMessageIfAvailable("liuju_hplj", data);
        }

        @Override
        public void onSiJiaLiZhi() {
            Map<String, Object> data = new HashMap<>();
            data.put("show", getShowPlateData());
            sendMessageIfAvailable("liuju_sjlz", data);
        }

        @Override
        public void onSiGangSanLe() {
            sendMessageIfAvailable("liuju_sgsl", null);
        }

        @Override
        public void onSiFengLianDa() {
            sendMessageIfAvailable("liuju_sfld", null);
        }

        @Override
        public void onLiuJuManGuan(List<Player> players, List<Boolean> zhuang) {
            assert players.size() == zhuang.size();
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("name", player.name);
                map.put("plate", player.plate.toData());
                map.put("zhuang", zhuang.get(i));
                list.add(map);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            sendMessageIfAvailable("ljmg", data);
        }

        @Override
        public void onPointChange() {
            List<Player> players = game.getPlayerOrder();
            List<Map<String, Object>> list = new ArrayList<>();
            for (Player p : players) {
                Map<String, Object> map = new HashMap<>();
                map.put("change", p.pointChange);
                map.put("point", p.point);
                list.add(map);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            sendMessageIfAvailable("point_change", data);
        }

        @Override
        public void onGameEnd(List<Player> order) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Player p : order) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", p.name);
                map.put("point", p.point);
                list.add(map);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("order", list);
            sendMessageIfAvailable("game_end", data);
        }

        @Override
        public void onNewDora(Mahjong.Card pointer) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", pointer.getType().label);
            data.put("number", pointer.getRawNumber());
            sendMessageIfAvailable("new_dora", data);
        }

        @Override
        public void onZhenTing() {
            sendMessageIfAvailable("zhenting", null);
        }

        /**
         * 解包牌，如果这张牌不在手牌中，则返回 null
         *
         * @param data 数据包
         * @return 这张牌
         */
        private Mahjong.Card getCard(JSONObject data) throws JSONException {
            Mahjong.Card.Type cardType;
            switch (data.getString("type")) {
                case "m":
                    cardType = Mahjong.Card.Type.M;
                    break;
                case "p":
                    cardType = Mahjong.Card.Type.P;
                    break;
                case "s":
                    cardType = Mahjong.Card.Type.S;
                    break;
                case "z":
                    cardType = Mahjong.Card.Type.Z;
                    break;
                default:
                    return null;
            }
            int num = data.getInt("number");
            Mahjong.Card card = new Mahjong.Card(cardType, num);
            if (!plate.cards.contains(card) && !justObtain.equals(card)) {
                return null;
            }
            return card;
        }

        private void dispatchMessage(String action, JSONObject data) {
            switch (action) {
                case "play":
                case "richi": {
                    actionLock.lock();
                    try {
                        if (this.action) {
                            Mahjong.Card card = getCard(data.getJSONObject("card"));
                            if (card == null) return;
                            boolean moqie = data.getBoolean("moqie") && card.equals(justObtain);
                            ActionType actionType = "richi".equals(action) ? ActionType.RiChi : ActionType.Play;
                            playerAction = new Action(actionType, card, moqie);
                            actionCondition.signal();
                        }
                    } finally {
                        actionLock.unlock();
                    }
                    break;
                }
                case "kskh": {
                    actionLock.lock();
                    try {
                        if (plate.isKSKH(justObtain)) {
                            playerAction = new Action(ActionType.KSKH);
                            actionCondition.signal();
                        }
                    } finally {
                        actionLock.unlock();
                    }
                    break;
                }
                case "jiagang": {
                    actionLock.lock();
                    try {
                        if (this.action) {
                            Mahjong.Card card = getCard(data.getJSONObject("card"));
                            if (card == null) return;
                            if (plate.canBuGang(card)) {
                                playerAction = new Action(ActionType.JiaGang, card);
                                actionCondition.signal();
                            }
                        }
                    } finally {
                        actionLock.unlock();
                    }
                    break;
                }
                case "angang": {
                    actionLock.lock();
                    try {
                        if (this.action) {
                            Mahjong.Card card = getCard(data.getJSONObject("card"));
                            if (card == null) return;
                            if (plate.canAnGang(card, riChiType != Mahjong.RiChiType.None)) {
                                playerAction = new Action(ActionType.AnGang, card);
                                actionCondition.signal();
                            }
                        }
                    } finally {
                        actionLock.unlock();
                    }
                    break;
                }
                case "zimo": {
                    actionLock.lock();
                    try {
                        if (this.action) {
                            Mahjong.Card card = getCard(data.getJSONObject("card"));
                            if (card == null) return;
                            if (canZimo) {
                                playerAction = new Action(ActionType.TsuMmo);
                                actionCondition.signal();
                            }
                        }
                    } finally {
                        actionLock.unlock();
                    }
                    break;
                }
                case "ron": {
                    actionLock.lock();
                    try {
                        if (this.action && (flag & FLAG_RON) != 0) {
                            playerAction = new Action(ActionType.Ron);
                            actionCondition.signal();
                        }
                    } finally {
                        actionLock.unlock();
                    }
                    break;
                }
                case "pong": {
                    // TODO: 碰
                    break;
                }
                case "chi": {
                    // TODO: 吃
                    break;
                }
            }
        }
    }
}
