package online.cszt0.jpmahjong.game;

import org.springframework.lang.Nullable;

import java.util.*;

/**
 * 用于麻将的各种工具方法
 */
@SuppressWarnings("unused")
public class Mahjong {
    /**
     * 牌
     */
    public static class Card implements Comparable<Card> {
        /**
         * 牌类型
         */
        public enum Type {
            M("m"),
            P("p"),
            S("s"),
            Z("z");

            public final String label;

            Type(String label) {
                this.label = label;
            }

            static Type getValueByLabel(String label) {
                switch (label) {
                    case "m":
                        return M;
                    case "p":
                        return P;
                    case "s":
                        return S;
                    case "z":
                        return Z;
                }
                return null;
            }
        }

        /**
         * 类型
         */
        private final Type type;
        /**
         * 数字（0表示红五，字牌1-4为风牌，5-7为三元牌）
         */
        private final int number;

        public Card(Type type, int number) {
            assert type != null;
            assert number >= 0 && number <= 9;
            assert type != Type.Z || number != 0 && number <= 7;
            this.type = type;
            this.number = number;
        }

        /**
         * 获取牌类型
         *
         * @return 牌类型
         */
        public Type getType() {
            return type;
        }

        /**
         * 获取牌面数字，红五依旧返回5
         *
         * @return 牌面数字
         */
        public int getNumber() {
            if (number == 0) return 5;
            return number;
        }

        /**
         * 判断是否是红宝牌
         *
         * @return 如果是红宝牌，则返回0
         */
        public boolean isRedDora() {
            return number == 0;
        }

        @Override
        public int compareTo(Card o) {
            // 根据类型比较
            if (type != o.type) {
                return type.ordinal() - o.type.ordinal();
            }
            // 考虑红五的情况下，根据牌面数字比较
            if (getNumber() != o.getNumber()) {
                return getNumber() - o.getNumber();
            }
            // 红五比正常牌要小
            return number - o.number;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Card card = (Card) o;

            if (number != card.number) return false;
            return type == card.type;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + number;
            return result;
        }

        /**
         * 在忽略红宝牌的情况下判断是否是相同牌
         *
         * @param card 另一牌
         * @return 是否相同
         */
        public boolean equalsIgnoreRedDora(Card card) {
            if (this == card) return true;
            if (card == null || getClass() != card.getClass()) return false;
            return type == card.type && getNumber() == card.getNumber();
        }

        /**
         * 转化为忽略红宝牌的版本
         *
         * @return 如果这张牌是红宝牌，则转为普通的牌，否则返回自身
         */
        public Card asIgnoreRedDora() {
            if (number != 0) return this;
            return new Card(type, 5);
        }

        /**
         * 判断是否是幺九牌
         *
         * @return 如果是幺九牌，返回 true，否则返回 false
         */
        public boolean isYaoJiu() {
            return number == 1 || number == 9 || type == Type.Z;
        }

        /**
         * 判断是否是三元牌
         *
         * @return 如果是三元牌，则返回 true
         */
        public boolean isSanYuan() {
            return type == Type.Z && (
                    number == 5 || number == 6 || number == 7
            );
        }

        /**
         * 判断是否为字牌
         *
         * @return 如果是字牌，返回 true
         */
        public boolean isZiPai() {
            return type == Type.Z;
        }

        /**
         * 判断是否为风牌
         *
         * @return 如果是风牌，返回 true
         */
        public boolean isFeng() {
            return type == Type.Z && number <= 4;
        }

        /**
         * 判断是否是绿牌（绿一色用的牌）
         *
         * @return 如果是，返回 true
         */
        public boolean isGreen() {
            return (type == Type.Z && number == 6) || (
                    type == Type.S && (
                            number == 2 || number == 3 || number == 4 || number == 6 || number == 8
                    )
            );
        }
    }

    /**
     * 风
     */
    public enum Feng {
        Dong(1),
        Nan(2),
        Xi(3),
        Bei(4);

        public final int number;

        Feng(int number) {
            this.number = number;
        }
    }

    /**
     * 副露
     */
    public static class Fulu {
        /**
         * 自己原本的牌
         */
        public final ArrayList<Card> original = new ArrayList<>(4);
        /**
         * 获得的其他家的牌
         */
        public Card obtain;
        /**
         * 加杠的牌
         */
        public Card jiagang;
        /**
         * 来源
         */
        public int from;

        /**
         * 判断该副露是否是 顺子
         *
         * @return 如果是顺子，返回 true
         */
        public boolean isChi() {
            if (original.size() == 2) {
                return !original.get(0).equalsIgnoreRedDora(original.get(1));
            }
            return false;
        }

        /**
         * 判断该副露是否是刻子（包含杠）
         *
         * @return 如果是刻子，返回 true
         */
        public boolean isPeng() {
            if (original.size() == 2) {
                return original.get(0).equalsIgnoreRedDora(original.get(1));
            }
            return true;
        }

        /**
         * 判断该副露是否是杠（包含大明杠、补杠和暗杠）
         *
         * @return 如果是杠，返回 true
         */
        public boolean isGang() {
            return original.size() == 3 || original.size() == 4 || jiagang != null;
        }

        /**
         * 判断该副露是否是暗杠
         *
         * @return 如果是暗杠，返回 true
         */
        public boolean isAnGang() {
            return original.size() == 4;
        }

        /**
         * 如果是顺子的话，获取最小的那一张牌
         *
         * @return 如果是顺子，获取最小的那一张牌。
         */
        public Card getChiStart() {
            assert isChi();
            Card c1 = original.get(0).asIgnoreRedDora();
            Card c2 = original.get(1).asIgnoreRedDora();
            Card c3 = obtain.asIgnoreRedDora();
            if (c1.number > c2.number) c1 = c2;
            if (c1.number > c3.number) c1 = c3;
            return c1;
        }
    }

    /**
     * 役种表
     */
    public enum YiZhongEnumerate {
        // 一番役
        LiZhi("立直"), // 立直，门前役
        DuanYaoJiu("断幺九"), // 断幺九，非食断模式下为门前役
        ZiMo("门前清自摸和"), // 门前清自摸和，门前役
        MenFeng("役牌：门风牌"), // 役牌：门风牌
        ChangFeng("役牌：场风牌"), // 役牌：场风牌
        DoubleFeng("役牌：双风牌"), // 役牌：双风牌
        Bai("役牌：白"), // 役牌：白
        Fa("役牌：发"), // 役牌：发
        Zhong("役牌：中"), // 役牌：中
        PingHu("平和"), // 平和，门前役
        YiBeiKou("一杯口"), // 一杯口，门前役
        QiangGang("抢杠"), // 抢杠
        LingShangKaiHua("岭上开花"), // 岭上开花
        HaiDiMoYue("海底摸月"), // 海底摸月
        HeDiLaoYv("河底捞鱼"), // 河底捞鱼
        YiFa("一发"), // 一发，门前役
        Dora("宝牌"), // 宝牌，不是役
        ChiDora("赤宝牌"), // 赤宝牌，不是役
        BeiDora("拔北宝牌"), // 拔北宝牌，不是役
        LiDora("里宝牌"), // 里宝牌，不是役

        // 二番役
        LiangLiZhi("两立直"), // 两立直，门前役
        SanSeTongKe("三色同刻"), // 三色同刻
        SanGangZi("三杠子"), // 三杠子
        DuiDuiHu("对对和"), // 对对和
        SanAnKe("三暗刻"), // 三暗刻
        XiaoSanYuan("小三元"), // 小三元
        HunLaoTou("混老头"), // 混老头
        QiDuiZi("七对子"), // 七对子，门前役
        HunQuanDaiYaoJiu("混全带幺九"), // 混全带幺九，食下役
        YiQiTongGuan("一气通贯"), // 一气通贯，食下役
        SanSeTongShun("三色同顺"), // 三色同顺，食下役

        // 三番役
        ErBeiKou("二杯口"), // 二杯口，门前役
        ChunQuanDaiYaoJiu("纯全带幺九"), // 纯全带幺九，食下役
        HunYiSe("混一色"), // 混一色，食下役

        // 六番役
        QingYiSe("清一色"), // 清一色，食下役

        // 满贯
        LiuJuManGuan("流局满贯"), // 流局满贯

        // 役满
        TianHu("天和"), // 天和，门前役
        DiHu("地和"), // 地和，门前役
        DaSanYuan("大三元"), // 大三元
        SiAnKe("四暗刻"), // 四暗刻，门前役
        ZiYiSe("字一色"), // 字一色
        LvYiSe("绿一色"), // 绿一色
        QingLaoTou("清老头"), // 清老头
        GuoShiWuShuang("国士无双"), // 国士无双，门前役
        XiaoSiXi("小四喜"), // 小四喜
        SiGangZi("四杠子"), // 四杠子
        JiuLianBaoDeng("九莲宝灯"), // 九莲宝灯，门前役

        // 双倍役满
        SiAnKeDanJi("四暗刻单骑"), // 四暗刻单骑，门前役
        GuoShiShiSanMian("国士无双十三面"), // 国士无双十三面，门前役
        ChunZhengJiuLian("纯正九莲宝灯"), // 纯正九莲宝灯，门前役
        DaSiXi("大四喜"), // 大四喜
        ;

        private final String name;

        YiZhongEnumerate(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 玩家手牌及副露
     */
    public static class Plate {
        /**
         * 手牌
         */
        public final ArrayList<Card> cards = new ArrayList<>(13);
        /**
         * 副露
         */
        public final ArrayList<Fulu> fulus = new ArrayList<>(4);
        /**
         * 拔北宝牌
         */
        public int bei;

        /**
         * 判断是否是门前清
         * 门前清表示没有暗杠以外的副露
         *
         * @return 如果是门前清，则返回 true
         */
        public boolean isMenqianqing() {
            for (Fulu fulu : fulus) {
                if (!fulu.isAnGang()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * 是否可以吃
         *
         * @param card 要吃的牌
         * @return 是否可以吃
         */
        public boolean canChi(Card card) {
            boolean[] flag = new boolean[4];
            for (Card c : cards) {
                if (c.type == card.type) {
                    flag[0] |= c.getNumber() + 2 == card.getNumber();
                    flag[1] |= c.getNumber() + 1 == card.getNumber();
                    flag[2] |= c.getNumber() - 1 == card.getNumber();
                    flag[3] |= c.getNumber() - 2 == card.getNumber();
                }
            }
            return flag[0] && flag[1] || flag[1] && flag[2] || flag[2] && flag[3];
        }

        /**
         * 是否可以碰
         *
         * @param card 要碰的牌
         * @return 是否可以碰
         */
        public boolean canPeng(Card card) {
            int count = 0;
            for (Card c : cards) {
                if (c.equalsIgnoreRedDora(card)) {
                    count++;
                }
            }
            return count >= 3;
        }

        /**
         * 是否可以大明杠
         *
         * @param card 要杠的牌
         * @return 是否可以大明杠
         */
        public boolean canDaMingGang(Card card) {
            int count = 0;
            for (Card c : cards) {
                if (c.equalsIgnoreRedDora(card)) {
                    count++;
                }
            }
            return count == 4;
        }

        /**
         * 是否可以加杠
         *
         * @param card 摸到的牌
         * @return 是否可以加杠
         */
        public boolean canBuGang(Card card) {
            for (Fulu fulu : fulus) {
                if (fulu.isPeng() && !fulu.isGang()) {
                    Card c = fulu.original.get(0);
                    if (c.equalsIgnoreRedDora(card)) {
                        return true;
                    }
                    for (Card cc : cards) {
                        if (c.equalsIgnoreRedDora(cc)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * 是否可以暗杠
         * <p>
         * 暗杠规则：
         * 非立直情况下，摸到四张相同的牌即可暗杠
         * 立直情况下，除了有四张相同的牌外，还必须满足以下条件：
         * 1. 这张牌刚刚摸到
         * 2. 要杠的牌只能解释为刻子
         *
         * @param card  摸到的牌
         * @param richi 是否已经立直
         * @return 是否可以暗杠
         */
        public boolean canAnGang(Card card, boolean richi) {
            if (!richi) {
                @SuppressWarnings("unchecked")
                ArrayList<Card> cards = (ArrayList<Card>) this.cards.clone();
                cards.add(card);
                Collections.sort(cards);
                card = null;
                int count = 0;
                for (Card c : cards) {
                    if (c.equalsIgnoreRedDora(card)) {
                        count++;
                        if (count == 4) {
                            return true;
                        }
                    } else {
                        card = c;
                        count = 0;
                    }
                }
                return false;
            } else {
                ArrayList<SplitWay> splitWays = splitPlate(cards, false, true);
                assert !splitWays.isEmpty();
                boolean can = true;
                for (SplitWay splitWay : splitWays) {
                    boolean found = false;
                    for (MianZi mianZi : splitWay.mianzi) {
                        if (mianZi.isPong() && mianZi.cards[0].equalsIgnoreRedDora(card)) {
                            found = true;
                        }
                    }
                    can &= found;
                }
                return can;
            }
        }

        /**
         * 是否九种九牌
         *
         * @param card 摸到的牌
         * @return 是否九种九牌
         */
        public boolean isKSKH(Card card) {
            List<Card> shisanyao = Arrays.asList(
                    new Card(Card.Type.M, 1),
                    new Card(Card.Type.M, 9),
                    new Card(Card.Type.P, 1),
                    new Card(Card.Type.P, 9),
                    new Card(Card.Type.S, 1),
                    new Card(Card.Type.S, 9),
                    new Card(Card.Type.Z, 1),
                    new Card(Card.Type.Z, 2),
                    new Card(Card.Type.Z, 3),
                    new Card(Card.Type.Z, 4),
                    new Card(Card.Type.Z, 5),
                    new Card(Card.Type.Z, 6),
                    new Card(Card.Type.Z, 7)
            );
            shisanyao.remove(card);
            cards.forEach(shisanyao::remove);
            return shisanyao.size() < 13 - 9;
        }
    }

    /**
     * 舍张
     */
    public static class Shezhang {

        public enum Status {
            /**
             * 正常
             */
            Normal,
            /**
             * 立直宣言牌
             */
            RiChi,
            /**
             * 被他家副露
             */
            Obtained
        }

        public final Card card;
        public Status status;

        public Shezhang(Card card) {
            this.card = card;
            this.status = Status.Normal;
        }
    }

    /**
     * 场况
     */
    public static class Environment {
        /**
         * 宝牌
         */
        public final ArrayList<Card> dora = new ArrayList<>(5);
        /**
         * 里宝牌
         */
        public final ArrayList<Card> ridora = new ArrayList<>(5);
        /**
         * 场风
         */
        public Feng changfeng;
        /**
         * 门风
         */
        public Feng menfeng;
        /**
         * 天和
         */
        public boolean tianhu;
        /**
         * 地和
         */
        public boolean dihu;
    }

    /**
     * 和牌、听牌时检测到的单个役种
     */
    public static class YiZhong {
        /**
         * 役种
         */
        public final YiZhongEnumerate yiZhongEnumerate;
        /**
         * 番数（负数表示对应倍数的役满）
         */
        public final int fan;
        /**
         * 是否不是役（true - 不是役）
         */
        public final boolean notYi;

        public YiZhong(YiZhongEnumerate yiZhongEnumerate, int fan, boolean notYi) {
            this.yiZhongEnumerate = yiZhongEnumerate;
            this.fan = fan;
            this.notYi = notYi;
        }

        public YiZhong(YiZhongEnumerate yiZhongEnumerate, int fan) {
            this(yiZhongEnumerate, fan, false);
        }

        @Override
        public String toString() {
            return yiZhongEnumerate + ": " + fan;
        }
    }

    /**
     * 和牌结果集
     */
    public static class WinResult {
        /**
         * 番数
         */
        public final int fan;
        /**
         * 符数
         */
        public final int fu;
        /**
         * 役种
         */
        public final ArrayList<YiZhong> yiZhongs;

        public WinResult(int fan, int fu, ArrayList<YiZhong> yiZhongs) {
            this.fan = fan;
            this.fu = fu;
            this.yiZhongs = yiZhongs;
        }
    }

    /**
     * 听牌结果集
     */
    public static class ListenResult {
        /**
         * 缺的牌
         */
        public final Card waitFor;
        /**
         * 需要打出的牌
         */
        public final Card giveup;
        /**
         * （荣和）番数
         */
        public final int fan;
        /**
         * （荣和）符数
         */
        public final int fu;
        /**
         * （荣和）役种
         */
        public final ArrayList<YiZhong> yiZhongs;
        /**
         * （自摸）番数
         */
        public final int zimofan;
        /**
         * （自摸）符数
         */
        public final int zimofu;
        /**
         * （自摸）役种
         */
        public final ArrayList<YiZhong> zimoyizhong;

        public ListenResult(Card waitFor, Card giveup, int fan, int fu, ArrayList<YiZhong> yiZhongs, int zimofan, int zimofu, ArrayList<YiZhong> zimoyizhong) {
            this.waitFor = waitFor;
            this.giveup = giveup;
            this.fan = fan;
            this.fu = fu;
            this.yiZhongs = yiZhongs;
            this.zimofan = zimofan;
            this.zimofu = zimofu;
            this.zimoyizhong = zimoyizhong;
        }
    }

    /**
     * 点数计算结果集
     */
    public static class PointResult {
        /**
         * 放铳者支付点数
         */
        public final int fangchong;
        /**
         * 自摸的情况下，庄家支付点数
         */
        public final int qinjia;
        /**
         * 自摸的情况下，闲家支付点数
         */
        public final int zijia;
        /**
         * 基本点（点数计算中的 a）
         * 参考值：
         * 2000 - 满贯
         * 3000 - 跳满
         * 4000 - 倍满
         * 6000 - 三倍满
         * 8000 - （累计）役满
         */
        public final int a;

        public PointResult(int fangchong, int qinjia, int zijia, int a) {
            this.fangchong = fangchong;
            this.qinjia = qinjia;
            this.zijia = zijia;
            this.a = a;
        }
    }

    /**
     * 役种计算结果集
     */
    public static class YiZhongResult {
        /**
         * 番数
         */
        public final int fan;
        /**
         * 符数
         */
        public final int fu;
        /**
         * 役种
         */
        public final ArrayList<YiZhong> yiZhongs;

        public YiZhongResult(int fan, int fu, ArrayList<YiZhong> yiZhongs) {
            this.fan = fan;
            this.fu = fu;
            this.yiZhongs = yiZhongs;
        }
    }

    /**
     * 牌来源
     */
    public enum CardSource {
        RongHu, // 荣和
        ZiMo, // 自摸
        LingShang, // 岭上
        HeDi, // 河底捞鱼
        HaiDi, // 海底摸月
        QiangGang, // 抢杠
    }

    /**
     * 立直种类
     */
    public enum RiChiType {
        None,
        RiChi,
        WRiChi
    }

    /**
     * 手牌中的面子
     */
    private static class MianZi {
        final Card[] cards;

        MianZi(Card c1, Card c2, Card c3) {
            cards = new Card[]{
                    c1, c2, c3
            };
            Arrays.sort(cards);
        }

        /**
         * 是否是顺子
         *
         * @return 如果是顺子，返回 true
         */
        boolean isChi() {
            return !cards[0].equalsIgnoreRedDora(cards[1]);
        }

        /**
         * 是否是刻子
         *
         * @return 如果是刻子，返回 true
         */
        boolean isPong() {
            return cards[0].equalsIgnoreRedDora(cards[1]);
        }

        boolean contains(Card card) {
            for (Card c : cards) {
                if (c.equals(card)) return true;
            }
            return false;
        }
    }

    /**
     * 手牌拆分方式
     */
    private static class SplitWay implements Cloneable {
        /**
         * 雀头
         */
        final Card[] quetou = new Card[2];
        /**
         * 面子
         */
        final ArrayList<MianZi> mianzi = new ArrayList<>(5);

        /**
         * 特殊牌型：七对子
         */
        boolean qiduizi;
        /**
         * 特殊牌型：国士无双
         */
        boolean guoshiwushuang;

        /**
         * 需要打出的牌
         */
        Card giveup;
        /**
         * 等待的牌
         */
        Card waitFor;

        @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "MethodDoesntCallSuperMethod"})
        @Override
        protected SplitWay clone() {
            SplitWay splitWay = new SplitWay();
            splitWay.quetou[0] = quetou[0];
            splitWay.quetou[1] = quetou[1];
            splitWay.mianzi.addAll(mianzi);
            splitWay.qiduizi = qiduizi;
            splitWay.guoshiwushuang = guoshiwushuang;
            splitWay.giveup = giveup;
            splitWay.waitFor = waitFor;
            return splitWay;
        }
    }

    public static class Paishan {
        public final Card[] cards;
        public int nextCardIndex;
        public int haidiCardIndex;
        public int doraCount;
        public int doraIndex;
        public int lingshangIndex;

        Paishan(Card[] cards) {
            this.cards = cards;
        }

        public static Paishan generate() {
            ArrayList<Card> allCards = new ArrayList<>();
            // M、P、S
            final int[] count = {1, 4, 4, 4, 4, 3, 4, 4, 4, 4};
            for (int i = 0; i <= 9; i++) {
                for (int j = 0; j < count[i]; j++) {
                    allCards.add(new Card(Card.Type.M, i));
                    allCards.add(new Card(Card.Type.P, i));
                    allCards.add(new Card(Card.Type.S, i));
                }
            }
            // Z
            for (int i = 1; i <= 7; i++) {
                for (int j = 0; j < 4; j++) {
                    allCards.add(new Card(Card.Type.Z, i));
                }
            }
            Collections.shuffle(allCards);
            Paishan paishan = new Paishan(allCards.toArray(new Card[0]));
            final int cardCount = (9 * 3 + 7) * 4;
            paishan.doraCount = 1;
            paishan.doraIndex = cardCount - 1 - 4 - 1;
            paishan.haidiCardIndex = cardCount - 1 - 4 - 10;
            paishan.lingshangIndex = cardCount - 1;
            return paishan;
        }

        /**
         * 从牌山中拿下一张牌
         *
         * @return 下一张牌
         */
        public Card nextCard() {
            return cards[nextCardIndex++];
        }

        /**
         * 从牌山中拿下一张岭上牌
         * 该操作会同时移动海底牌，但不会更改宝牌
         *
         * @return 下一张岭上牌
         */
        public Card nextLingshangCard() {
            haidiCardIndex--;
            return cards[lingshangIndex--];
        }

        /**
         * 判断是否已经摸到海底牌
         *
         * @return 如果海底牌已经被摸到，返回 true
         */
        public boolean isHaidi() {
            return nextCardIndex > haidiCardIndex;
        }

        /**
         * 填充宝牌
         *
         * @param env 环境
         */
        public void fillDora(Environment env) {
            for (int i = 0; i < doraCount; i++) {
                env.dora.add(doraNextCard(cards[doraIndex - 2 * i]));
                env.ridora.add(doraNextCard(cards[doraIndex + 1 - 2 * i]));
            }
        }

        /**
         * @see #doraNextCard(Card)
         */
        private static final Card[] z_dora_card = {
                new Card(Card.Type.Z, 2),
                new Card(Card.Type.Z, 3),
                new Card(Card.Type.Z, 4),
                new Card(Card.Type.Z, 1),
                new Card(Card.Type.Z, 6),
                new Card(Card.Type.Z, 7),
                new Card(Card.Type.Z, 5),
        };

        private Card doraNextCard(Card card) {
            Card.Type type = card.getType();
            if (type == Card.Type.Z) {
                return z_dora_card[card.getNumber() - 1];
            }
            int num = card.getNumber() + 1;
            if (num == 10) num = 1;
            return new Card(type, num);
        }

        public int getRemain() {
            return haidiCardIndex - nextCardIndex + 1;
        }

        /**
         * 展示的最后一张宝牌指示牌
         *
         * @return 展示的最后一张宝牌指示牌
         */
        public Card lastDoraPointer() {
            return cards[doraIndex - 2 * doraCount + 2];
        }
    }

    /**
     * 检查和牌
     *
     * @param plate       玩家手牌
     * @param environment 场况
     * @param source      牌来源
     * @param riChiType   立直类型
     * @param ibachi      是否一发
     * @return 和牌结果，如果未和牌则返回 null
     */
    @Nullable
    public static WinResult checkWin(Plate plate, Card card, Environment environment, CardSource source, RiChiType riChiType, boolean ibachi) {
        // 复制牌
        @SuppressWarnings("unchecked")
        ArrayList<Card> cards = (ArrayList<Card>) plate.cards.clone();
        cards.add(card);
        // 天和情况
        if (card == null) {
            assert plate.cards.size() == 14;
            card = plate.cards.get(0);
        }
        // 拆解牌
        ArrayList<SplitWay> splitWays = splitPlate(cards, false, false);
        // 无法拆解：未和牌
        if (splitWays.isEmpty()) {
            return null;
        }
        // 检查役种
        YiZhongResult maxResult = null;
        for (SplitWay splitWay : splitWays) {
            YiZhongResult currentResult = computeYizhong(splitWay, plate, card, source, riChiType, ibachi, environment);
            if (maxResult == null || (maxResult.fan >= 0 && currentResult.fan > maxResult.fan) || (currentResult.fan < 0 && currentResult.fan < maxResult.fan) || (currentResult.fan == maxResult.fan && currentResult.fu > maxResult.fu)) {
                maxResult = currentResult;
            }
        }
        return new WinResult(maxResult.fan, maxResult.fu, maxResult.yiZhongs);
    }

    /**
     * 检查听牌
     *
     * @param plate       玩家手牌
     * @param card        摸到的牌（如果存在的话）
     * @param environment 场况
     * @param riChiType   立直
     * @return 听牌结果，如果未听牌则返回 null
     */
    @Nullable
    public static ArrayList<ListenResult> checkListen(Plate plate, @Nullable Card card, Environment environment, RiChiType riChiType) {
        // 复制牌
        @SuppressWarnings("unchecked")
        ArrayList<Card> cards = (ArrayList<Card>) plate.cards.clone();
        if (card != null) {
            cards.add(card);
        }
        boolean canGiveup;
        switch (cards.size()) {
            default:
                assert false;
            case 1:
            case 4:
            case 7:
            case 10:
            case 13:
                canGiveup = false;
                break;
            case 2:
            case 5:
            case 8:
            case 11:
            case 14:
                canGiveup = true;
                break;
        }
        // 拆解牌
        ArrayList<SplitWay> splitWays = splitPlate(cards, canGiveup, true);
        // 无法拆解：未听牌
        if (splitWays.isEmpty()) {
            return null;
        }
        // 检查役种
        // TODO: 高点法保留最大得点
        ArrayList<ListenResult> listenResults = new ArrayList<>();
        Plate clonePlate = new Plate();
        clonePlate.bei = plate.bei;
        clonePlate.fulus.addAll(plate.fulus);
        for (SplitWay splitWay : splitWays) {
            clonePlate.cards.clear();
            clonePlate.cards.addAll(plate.cards);
            if (card != null) {
                clonePlate.cards.add(card);
                clonePlate.cards.remove(splitWay.giveup);
            }
            YiZhongResult ronghu = computeYizhong(splitWay, plate, splitWay.waitFor, CardSource.RongHu, riChiType, false, environment);
            YiZhongResult zimo = computeYizhong(splitWay, plate, splitWay.waitFor, CardSource.ZiMo, riChiType, false, environment);
            listenResults.add(new ListenResult(splitWay.waitFor, splitWay.giveup, ronghu.fan, ronghu.fu, ronghu.yiZhongs, zimo.fan, zimo.fu, zimo.yiZhongs));
        }
        return listenResults;
    }

    /**
     * 计算点数
     *
     * @param fan    番
     * @param fu     符
     * @param zhuang 和牌者是否是庄家
     * @return 计算结果
     */
    public static PointResult computePoint(int fan, int fu, boolean zhuang) {
        int a;
        if (fan >= 0) {
            if (fan < 5) {
                a = fu * (1 << (2 + fan));
                if (a > 2000) {
                    a = 2000;
                }
            } else if (fan == 5) {
                // 满贯
                a = 2000;
            } else if (fan <= 7) {
                // 跳满
                a = 3000;
            } else if (fan <= 10) {
                // 倍满
                a = 4000;
            } else if (fan <= 12) {
                // 三倍满
                a = 6000;
            } else {
                // 累计役满
                a = 8000;
            }
        } else {
            a = 8000 * -fan;
        }
        if (zhuang) {
            return new PointResult(roundPoint(6 * a), 0, roundPoint(2 * a), a);
        } else {
            return new PointResult(roundPoint(4 * a), roundPoint(2 * a), roundPoint(a), a);
        }
    }

    /**
     * 点数舍入
     * 该方法在 {@link #computePoint(int, int, boolean)} 中使用
     *
     * @param point 原始点数
     * @return 舍入后点数
     */
    private static int roundPoint(int point) {
        if (point % 100 != 0) {
            return point / 100 * 100 + 100;
        }
        return point;
    }

    /**
     * 将牌拆分为和牌形式
     *
     * @param cards      牌
     * @param canGiveup  是否可以放弃（打出）一张牌
     * @param canWaitFor 是否可以等待一张牌
     * @return 所有可能的和牌形式
     */
    private static ArrayList<SplitWay> splitPlate(ArrayList<Card> cards, boolean canGiveup, boolean canWaitFor) {
        // 如果可以舍弃一张牌，那就必须等待另一张牌
        assert !canGiveup || canWaitFor;

        // 对手牌排序
        Collections.sort(cards);

        ArrayList<SplitWay> splitWays = new ArrayList<>();

        // 递归回溯拆分
        boolean[] cardUsed = new boolean[cards.size()];
        deepSplitPlate(cards, cardUsed, canGiveup, canWaitFor, false, splitWays, new SplitWay());

        // 七对子
        splitAsQiduizi(cards, canGiveup, canWaitFor, splitWays);
        // 国士无双
        splitAsGuoshi(cards, canGiveup, canWaitFor, splitWays);

        return splitWays;
    }

    /**
     * 递归回溯将手牌拆分为 N面子+1雀头
     * <p>
     * 拆分结果中没有红宝牌标记
     *
     * @param cards      手牌
     * @param cardUsed   手牌使用标记
     * @param canGiveup  是否可以放弃一张牌
     * @param canWaitFor 是否可以等待一张牌
     * @param quetou     是否已经拆解出雀头
     * @param out        拆分结果集
     * @param splitWay   递归上下文
     */
    private static void deepSplitPlate(ArrayList<Card> cards, boolean[] cardUsed, boolean canGiveup, boolean canWaitFor, boolean quetou, ArrayList<SplitWay> out, SplitWay splitWay) {
        int cardCount = cards.size();
        // 寻找第一个未使用的牌
        int index = -1;
        for (int i = 0; i < cardCount; i++) {
            if (!cardUsed[i]) {
                index = i;
                break;
            }
        }
        // 递归结束
        if (index == -1) {
            if (!canGiveup && !canWaitFor && quetou) {
                out.add(splitWay.clone());
            }
            return;
        }
        Card card = cards.get(index).asIgnoreRedDora();
        Card.Type type = card.getType();
        int number = card.getNumber();

        if (cardCount - index > 1) {
            // 刻子
            if (card.equalsIgnoreRedDora(cards.get(index + 1))) {
                // OOO
                if (cardCount - index > 2 && card.equalsIgnoreRedDora(cards.get(index + 2))) {
                    cardUsed[index] = cardUsed[index + 1] = cardUsed[index + 2] = true;
                    splitWay.mianzi.add(new MianZi(
                            card,
                            cards.get(index + 1),
                            cards.get(index + 2)
                    ));
                    deepSplitPlate(cards, cardUsed, canGiveup, canWaitFor, quetou, out, splitWay);
                    splitWay.mianzi.remove(splitWay.mianzi.size() - 1);
                    cardUsed[index] = cardUsed[index + 1] = cardUsed[index + 2] = false;
                }
                // OOX
                if (canWaitFor) {
                    Card waitForCard = new Card(type, number);
                    cardUsed[index] = cardUsed[index + 1] = true;
                    splitWay.mianzi.add(new MianZi(
                            card,
                            cards.get(index + 1),
                            waitForCard
                    ));
                    splitWay.waitFor = waitForCard;
                    deepSplitPlate(cards, cardUsed, canGiveup, false, quetou, out, splitWay);
                    splitWay.waitFor = null;
                    splitWay.mianzi.remove(splitWay.mianzi.size() - 1);
                    cardUsed[index] = cardUsed[index + 1] = false;
                }
            }
            // 顺子
            if (type != Card.Type.Z && number < 9) {
                int next1 = findNextUnused(cards, cardUsed, type, number + 1);
                int next2 = number < 8 ? findNextUnused(cards, cardUsed, type, number + 2) : -1;
                // OOO
                if (next1 != -1 && next2 != -1) {
                    cardUsed[index] = cardUsed[next1] = cardUsed[next2] = true;
                    splitWay.mianzi.add(new MianZi(
                            card,
                            cards.get(next1).asIgnoreRedDora(),
                            cards.get(next2).asIgnoreRedDora()
                    ));
                    deepSplitPlate(cards, cardUsed, canGiveup, canWaitFor, quetou, out, splitWay);
                    splitWay.mianzi.remove(splitWay.mianzi.size() - 1);
                    cardUsed[index] = cardUsed[next1] = cardUsed[next2] = false;
                }
                if (canWaitFor) {
                    // OXO
                    if (next2 != -1) {
                        Card waitForCard = new Card(type, number + 1);
                        cardUsed[index] = cardUsed[next2] = true;
                        splitWay.mianzi.add(new MianZi(
                                card,
                                waitForCard,
                                cards.get(next2).asIgnoreRedDora()
                        ));
                        splitWay.waitFor = waitForCard;
                        deepSplitPlate(cards, cardUsed, canGiveup, false, quetou, out, splitWay);
                        splitWay.waitFor = null;
                        splitWay.mianzi.remove(splitWay.mianzi.size() - 1);
                        cardUsed[index] = cardUsed[next2] = false;
                    }
                    if (next1 != -1) {
                        // OOX
                        if (number < 8) {
                            Card waitForCard = new Card(type, number + 2);
                            cardUsed[index] = cardUsed[next1] = true;
                            splitWay.mianzi.add(new MianZi(
                                    card,
                                    cards.get(next1).asIgnoreRedDora(),
                                    waitForCard
                            ));
                            splitWay.waitFor = waitForCard;
                            deepSplitPlate(cards, cardUsed, canGiveup, false, quetou, out, splitWay);
                            splitWay.waitFor = null;
                            splitWay.mianzi.remove(splitWay.mianzi.size() - 1);
                            cardUsed[index] = cardUsed[next1] = false;
                        }
                        // XOO
                        if (number > 1) {
                            Card waitForCard = new Card(type, number - 1);
                            cardUsed[index] = cardUsed[next1] = true;
                            splitWay.mianzi.add(new MianZi(
                                    waitForCard,
                                    card,
                                    cards.get(next1).asIgnoreRedDora()
                            ));
                            splitWay.waitFor = waitForCard;
                            deepSplitPlate(cards, cardUsed, canGiveup, false, quetou, out, splitWay);
                            splitWay.waitFor = null;
                            splitWay.mianzi.remove(splitWay.mianzi.size() - 1);
                            cardUsed[index] = cardUsed[next1] = false;
                        }
                    }
                }
            }
        }
        // 雀头
        if (!quetou) {
            if (cardCount - index > 1 && card.equalsIgnoreRedDora(cards.get(index + 1))) {
                cardUsed[index] = cardUsed[index + 1] = true;
                splitWay.quetou[0] = card;
                splitWay.quetou[1] = cards.get(index + 1).asIgnoreRedDora();
                deepSplitPlate(cards, cardUsed, canGiveup, canWaitFor, true, out, splitWay);
                cardUsed[index] = cardUsed[index + 1] = false;
            }
            if (canWaitFor) {
                Card waitForCard = new Card(type, number);
                cardUsed[index] = true;
                splitWay.quetou[0] = card;
                splitWay.quetou[1] = waitForCard;
                splitWay.waitFor = waitForCard;
                deepSplitPlate(cards, cardUsed, canGiveup, false, true, out, splitWay);
                splitWay.waitFor = null;
                cardUsed[index] = false;
            }
        }

        if (canGiveup) {
            cardUsed[index] = true;
            splitWay.giveup = card;
            deepSplitPlate(cards, cardUsed, false, canWaitFor, quetou, out, splitWay);
            splitWay.giveup = null;
            cardUsed[index] = false;
        }
    }

    /**
     * 以七对子方式拆解手牌
     * <p>
     * 拆分结果中没有红宝牌标记
     *
     * @param cards      手牌
     * @param canGiveup  是否可以放弃一张牌
     * @param canWaitFor 是否可以等待一张牌
     * @param splitWays  拆分结果集
     */
    private static void splitAsQiduizi(ArrayList<Card> cards, boolean canGiveup, boolean canWaitFor, ArrayList<SplitWay> splitWays) {
        // 至少 13 张牌才能拆解为七对子
        // 不满 13 张的情况：有副露
        if (cards.size() < 13) return;
        // 统计牌种类
        HashMap<Card, Integer> cardCountMap = new HashMap<>();
        cards.forEach(card -> {
            Card key = card.asIgnoreRedDora();
            cardCountMap.put(key, cardCountMap.getOrDefault(key, 0) + 1);
        });
        // 移除成对的牌
        cardCountMap.values().removeIf(integer -> integer == 2);
        if (!canWaitFor) {
            assert cards.size() == 14;
            if (cardCountMap.isEmpty()) {
                SplitWay splitWay = new SplitWay();
                splitWay.qiduizi = true;
                splitWays.add(splitWay);
            }
        } else if (canGiveup) {
            assert cards.size() == 14;
            if (cardCountMap.size() == 2) {
                Card[] cs = cardCountMap.keySet().toArray(new Card[0]);
                int[] counts = {
                        cardCountMap.get(cs[0]),
                        cardCountMap.get(cs[1])
                };
                if (counts[0] == 1) {
                    SplitWay splitWay = new SplitWay();
                    splitWay.qiduizi = true;
                    splitWay.waitFor = cs[0];
                    splitWay.giveup = cs[1];
                    splitWays.add(splitWay);
                }
                if (counts[1] == 1) {
                    SplitWay splitWay = new SplitWay();
                    splitWay.qiduizi = true;
                    splitWay.waitFor = cs[1];
                    splitWay.giveup = cs[0];
                    splitWays.add(splitWay);
                }
            }
        } else {
            assert cards.size() == 13;
            if (cardCountMap.size() == 1) {
                Card card = cardCountMap.keySet().toArray(new Card[0])[0];
                int count = cardCountMap.get(card);
                if (count == 1) {
                    SplitWay splitWay = new SplitWay();
                    splitWay.qiduizi = true;
                    splitWay.waitFor = card;
                    splitWays.add(splitWay);
                }
            }
        }
    }

    /**
     * 以国士无双方式拆解手牌
     *
     * @param cards      手牌
     * @param canGiveup  是否可以放弃一张牌
     * @param canWaitFor 是否可以等待一张牌
     * @param splitWays  拆分结果集
     */
    private static void splitAsGuoshi(ArrayList<Card> cards, boolean canGiveup, boolean canWaitFor, ArrayList<SplitWay> splitWays) {
        // 至少 13 张牌才能拆解为国士无双
        // 不满 13 张的情况：有副露
        if (cards.size() < 13) return;
        // 十三幺
        List<Card> shisanyao = Arrays.asList(
                new Card(Card.Type.M, 1),
                new Card(Card.Type.M, 9),
                new Card(Card.Type.P, 1),
                new Card(Card.Type.P, 9),
                new Card(Card.Type.S, 1),
                new Card(Card.Type.S, 9),
                new Card(Card.Type.Z, 1),
                new Card(Card.Type.Z, 2),
                new Card(Card.Type.Z, 3),
                new Card(Card.Type.Z, 4),
                new Card(Card.Type.Z, 5),
                new Card(Card.Type.Z, 6),
                new Card(Card.Type.Z, 7)
        );
        ArrayList<Card> yaojiu = new ArrayList<>(shisanyao);
        ArrayList<Card> repeat = new ArrayList<>();
        // 其他牌
        ArrayList<Card> others = new ArrayList<>();
        cards.forEach(card -> {
            if (shisanyao.contains(card)) {
                if (!yaojiu.remove(card)) {
                    repeat.add(card);
                }
            } else {
                others.add(card);
            }
        });
        if (!canWaitFor) {
            assert cards.size() == 14;
            if (others.isEmpty() && yaojiu.isEmpty()) {
                SplitWay splitWay = new SplitWay();
                splitWay.guoshiwushuang = true;
                splitWays.add(splitWay);
            }
        } else if (canGiveup) {
            assert cards.size() == 14;
            if (others.size() == 1) {
                SplitWay splitWay = new SplitWay();
                splitWay.guoshiwushuang = true;
                splitWay.giveup = others.get(0);
                if (yaojiu.isEmpty()) {
                    assert repeat.isEmpty();
                    // 十三面
                    for (Card card : shisanyao) {
                        splitWay.waitFor = card;
                        splitWays.add(splitWay.clone());
                    }
                } else if (yaojiu.size() == 1) {
                    assert repeat.size() == 1;
                    splitWay.waitFor = yaojiu.get(0);
                    splitWays.add(splitWay.clone());
                }
            } else if (others.isEmpty()) {
                if (yaojiu.isEmpty()) {
                    assert repeat.size() == 1;
                    // 已经和牌了
                    Card rep = repeat.get(0);
                    SplitWay splitWay = new SplitWay();
                    splitWay.guoshiwushuang = true;
                    for (Card card : shisanyao) {
                        splitWay.giveup = card;
                        if (card.equals(rep)) {
                            // 进入十三面听
                            for (Card c : shisanyao) {
                                splitWay.waitFor = c;
                                splitWays.add(splitWay.clone());
                            }
                        } else {
                            // 进入一面听
                            splitWay.waitFor = card;
                            splitWays.add(splitWay.clone());
                        }
                    }
                } else if (yaojiu.size() == 1) {
                    assert repeat.size() == 2;
                    SplitWay splitWay = new SplitWay();
                    splitWay.guoshiwushuang = true;
                    splitWay.waitFor = yaojiu.get(0);
                    splitWay.giveup = repeat.get(0);
                    splitWays.add(splitWay.clone());
                    if (!splitWay.giveup.equals(repeat.get(1))) {
                        splitWay.giveup = repeat.get(1);
                        splitWays.add(splitWay.clone());
                    }
                }
            }
        } else {
            SplitWay splitWay = new SplitWay();
            splitWay.guoshiwushuang = true;
            if (yaojiu.isEmpty()) {
                // 十三面听
                for (Card c : shisanyao) {
                    splitWay.waitFor = c;
                    splitWays.add(splitWay.clone());
                }
            } else if (yaojiu.size() == 1) {
                assert repeat.size() == 1;
                // 一面听
                splitWay.waitFor = yaojiu.get(0);
                splitWays.add(splitWay);
            }
        }
    }

    /**
     * 寻找下一个没有使用的指定牌，该方法在 {@link #deepSplitPlate(ArrayList, boolean[], boolean, boolean, boolean, ArrayList, SplitWay)} 中使用
     *
     * @param cards    全部手牌
     * @param cardUsed 使用标记
     * @param type     类型
     * @param number   数字
     * @return 下标，如果没有指定牌，返回 -1
     */
    private static int findNextUnused(ArrayList<Card> cards, boolean[] cardUsed, Card.Type type, int number) {
        for (int i = 0; i < cardUsed.length; i++) {
            if (!cardUsed[i] && new Card(type, number).equalsIgnoreRedDora(cards.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算役种
     *
     * @param splitWay 手牌拆解方式
     * @param plate    手牌等信息
     * @param card     最后一张牌
     * @param source   最后一张牌来源
     * @param richi    立直类型
     * @param ibachi   是否一发
     * @param env      场况
     * @return 返回所有满足的役种及番数、符数
     */
    private static YiZhongResult computeYizhong(SplitWay splitWay, Plate plate, Card card, CardSource source, RiChiType richi, boolean ibachi, Environment env) {
        // 门前清
        boolean menqianqing = plate.isMenqianqing();
        // 听牌类型
        // 1 - 两面听
        // 2 - 嵌张听
        // 4 - 双碰听
        // 8 - 边张听
        // 16 - 单骑听
        int listenType = 0;
        if (!splitWay.qiduizi && !splitWay.guoshiwushuang) {
            for (MianZi mianzi : splitWay.mianzi) {
                Card ignoreRedDora = card.asIgnoreRedDora();
                int number = ignoreRedDora.getNumber();
                if (mianzi.contains(ignoreRedDora)) {
                    if (mianzi.isChi()) {
                        int chiStart = mianzi.cards[0].getNumber();
                        if (number == chiStart + 1) {
                            listenType |= 2;
                        } else if (number == 7 && chiStart == 7 || number == 3 && chiStart == 1) {
                            listenType |= 8;
                        } else {
                            listenType |= 1;
                        }
                    } else {
                        listenType |= 4;
                    }
                }
            }
            if (splitWay.quetou[0].equalsIgnoreRedDora(card)) {
                listenType |= 16;
            }
        }
        // 自摸
        boolean zimo = source == CardSource.ZiMo || source == CardSource.LingShang || source == CardSource.HaiDi;
        // 平和
        boolean pinghu = false;

        ArrayList<YiZhong> yizhong = new ArrayList<>();

        // 天和
        if (env.tianhu) {
            yizhong.add(new YiZhong(YiZhongEnumerate.TianHu, -1));
        }
        // 地和
        if (env.dihu) {
            yizhong.add(new YiZhong(YiZhongEnumerate.DiHu, -1));
        }

        // 立直
        if (richi == RiChiType.RiChi) {
            yizhong.add(new YiZhong(YiZhongEnumerate.LiZhi, 1));
        }
        // 两立直
        if (richi == RiChiType.WRiChi) {
            yizhong.add(new YiZhong(YiZhongEnumerate.LiangLiZhi, 2));
        }
        // 一发
        if (ibachi) {
            yizhong.add(new YiZhong(YiZhongEnumerate.YiFa, 1));
        }
        // 抢杠
        if (source == CardSource.QiangGang) {
            yizhong.add(new YiZhong(YiZhongEnumerate.QiangGang, 1));
        }
        // 岭上开花
        if (source == CardSource.LingShang) {
            yizhong.add(new YiZhong(YiZhongEnumerate.LingShangKaiHua, 1));
        }
        // 海底摸月
        if (source == CardSource.HaiDi) {
            yizhong.add(new YiZhong(YiZhongEnumerate.HaiDiMoYue, 1));
        }
        // 河底捞鱼
        if (source == CardSource.HeDi) {
            yizhong.add(new YiZhong(YiZhongEnumerate.HeDiLaoYv, 1));
        }
        // 门前清自摸和
        if (menqianqing && zimo) {
            yizhong.add(new YiZhong(YiZhongEnumerate.ZiMo, 1));
        }
        // 役牌：三元牌
        // 役牌：门风牌
        // 役牌：场风牌
        splitWay.mianzi.forEach(mianZi -> {
            Card c = mianZi.cards[0];
            if (mianZi.isPong() && c.type == Card.Type.Z) {
                if (c.number == 5) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.Bai, 1));
                } else if (c.number == 6) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.Fa, 1));
                } else if (c.number == 7) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.Zhong, 1));
                }
                if (env.changfeng == env.menfeng && c.number == env.changfeng.number) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.DoubleFeng, 2));
                } else if (c.number == env.changfeng.number) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.ChangFeng, 1));
                } else if (c.number == env.menfeng.number) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.MenFeng, 1));
                }
            }
        });
        plate.fulus.forEach(fulu -> {
            Card c = fulu.obtain;
            if (fulu.isPeng() && c.type == Card.Type.Z) {
                if (c.number == 5) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.Bai, 1));
                } else if (c.number == 6) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.Fa, 1));
                } else if (c.number == 7) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.Zhong, 1));
                }
                if (env.changfeng == env.menfeng && c.number == env.changfeng.number) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.DoubleFeng, 2));
                } else if (c.number == env.changfeng.number) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.ChangFeng, 1));
                } else if (c.number == env.menfeng.number) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.MenFeng, 1));
                }
            }
        });

        // 平和
        if (menqianqing && ((listenType & 1) != 0)) {
            int shunziCount = 0;
            boolean quetou = true;
            for (MianZi mianZi : splitWay.mianzi) {
                if (mianZi.isChi()) {
                    shunziCount++;
                }
            }
            if (splitWay.quetou[0].type == Card.Type.Z) {
                int number = splitWay.quetou[0].number;
                if (number == env.changfeng.number || number == env.menfeng.number || number > 4) {
                    quetou = false;
                }
            }
            if (shunziCount == 4 && quetou) {
                yizhong.add(new YiZhong(YiZhongEnumerate.PingHu, 1));
                pinghu = true;
            }
        }

        // 一杯口
        // 二杯口
        if (menqianqing) {
            final int[] count = new int[30];
            splitWay.mianzi.forEach(mianZi -> {
                if (mianZi.isChi()) {
                    int type = mianZi.cards[0].type.ordinal();
                    int number = mianZi.cards[0].number;
                    count[type * 10 + number]++;
                }
            });
            int k = 0;
            for (int i = 0; i < 30; i++) {
                if (count[i] > 1) {
                    k++;
                }
            }
            assert k <= 2;
            if (k == 1) {
                yizhong.add(new YiZhong(YiZhongEnumerate.YiBeiKou, 1));
            } else if (k == 2) {
                yizhong.add(new YiZhong(YiZhongEnumerate.ErBeiKou, 3));
            }
        }
        // 断幺九
        {
            boolean duanyaojiu = !card.isYaoJiu();
            for (Card c : plate.cards) {
                duanyaojiu &= !c.isYaoJiu();
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.obtain != null) {
                    duanyaojiu &= !fulu.obtain.isYaoJiu();
                }
                for (Card c : fulu.original) {
                    duanyaojiu &= !c.isYaoJiu();
                }
            }
            if (duanyaojiu) {
                yizhong.add(new YiZhong(YiZhongEnumerate.DuanYaoJiu, 1));
            }
        }
        // 三色同刻
        {
            final int[] m = new int[10];
            splitWay.mianzi.forEach(mianZi -> {
                if (mianZi.isPong()) {
                    m[mianZi.cards[0].getNumber()]++;
                }
            });
            plate.fulus.forEach(fulu -> {
                if (fulu.isPeng()) {
                    m[fulu.original.get(0).getNumber()]++;
                }
            });
            for (int i : m) {
                if (i == 3) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.SanSeTongKe, 2));
                }
            }
        }
        // 三杠子
        // 四杠子
        {
            int count = 0;
            for (Fulu fulu : plate.fulus) {
                if (fulu.isGang()) {
                    count++;
                }
            }
            if (count == 3) {
                yizhong.add(new YiZhong(YiZhongEnumerate.SanGangZi, 2));
            } else if (count == 4) {
                yizhong.add(new YiZhong(YiZhongEnumerate.SiGangZi, -1));
            }
        }
        // 对对和
        {
            int count = 0;
            for (MianZi mianzi : splitWay.mianzi) {
                if (mianzi.isPong()) {
                    count++;
                }
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.isPeng()) {
                    count++;
                }
            }
            if (count == 4) {
                yizhong.add(new YiZhong(YiZhongEnumerate.DuiDuiHu, 2));
            }
        }
        // 七对子
        if (splitWay.qiduizi) {
            yizhong.add(new YiZhong(YiZhongEnumerate.QiDuiZi, 2));
        }

        // 三暗刻
        // 四暗刻
        // 四暗刻单骑
        {
            int count = 0;
            for (MianZi mianZi : splitWay.mianzi) {
                if (mianZi.isPong()) {
                    count++;
                }
            }
            // 暗杠算暗刻
            for (Fulu fulu : plate.fulus) {
                if (fulu.isAnGang()) {
                    count++;
                }
            }
            // 双碰听并且没有自摸，减一个暗刻
            if (listenType == 4 && !zimo) {
                count--;
            }

            if (count == 3) {
                yizhong.add(new YiZhong(YiZhongEnumerate.SanAnKe, 2));
            } else if (count == 4) {
                if ((listenType & 16) != 0 || env.tianhu) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.SiAnKeDanJi, -2));
                } else {
                    yizhong.add(new YiZhong(YiZhongEnumerate.SiAnKe, -1));
                }
            }
        }
        // 小三元
        // 大三元
        {
            int count = 0;
            for (MianZi mianZi : splitWay.mianzi) {
                if (mianZi.cards[0].isSanYuan()) {
                    count++;
                }
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.original.get(0).isSanYuan()) {
                    count++;
                }
            }
            if (count == 2 && splitWay.quetou[0].isSanYuan()) {
                yizhong.add(new YiZhong(YiZhongEnumerate.XiaoSanYuan, 2));
            } else if (count == 3) {
                yizhong.add(new YiZhong(YiZhongEnumerate.DaSanYuan, -1));
            }
        }
        // 混老头
        // 清老头
        {
            boolean yaojiu = card.isYaoJiu();
            boolean zipai = card.isZiPai();
            for (Card c : plate.cards) {
                yaojiu &= c.isYaoJiu();
                zipai |= c.isZiPai();
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.isPeng()) {
                    yaojiu &= fulu.original.get(0).isYaoJiu();
                    zipai |= fulu.original.get(0).isZiPai();
                }
            }
            if (yaojiu) {
                if (zipai) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.HunLaoTou, 2));
                } else {
                    yizhong.add(new YiZhong(YiZhongEnumerate.QingLaoTou, -1));
                }
            }
        }
        // 混全带幺九
        // 纯全带幺九
        if (!splitWay.qiduizi && !splitWay.guoshiwushuang) {
            boolean zipai = splitWay.quetou[0].isZiPai();
            boolean daiyaojiu = splitWay.quetou[0].isYaoJiu();
            for (MianZi mianZi : splitWay.mianzi) {
                if (mianZi.isPong()) {
                    zipai |= mianZi.cards[0].isZiPai();
                    daiyaojiu &= mianZi.cards[0].isYaoJiu();
                } else {
                    int num = mianZi.cards[0].getNumber();
                    daiyaojiu &= num == 1 || num == 7;
                }
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.isPeng()) {
                    zipai |= fulu.original.get(0).isZiPai();
                    daiyaojiu &= fulu.original.get(0).isYaoJiu();
                } else {
                    int num = fulu.getChiStart().number;
                    daiyaojiu &= num == 1 || num == 7;
                }
            }
            if (daiyaojiu) {
                if (zipai) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.HunQuanDaiYaoJiu, menqianqing ? 2 : 1));
                } else {
                    yizhong.add(new YiZhong(YiZhongEnumerate.ChunQuanDaiYaoJiu, menqianqing ? 3 : 2));
                }
            }
        }
        // 一气通贯
        {
            final boolean[][] flag = new boolean[3][3];
            splitWay.mianzi.forEach(mianZi -> {
                if (mianZi.isChi()) {
                    int number = mianZi.cards[0].number;
                    if (number == 1) {
                        number = 0;
                    } else if (number == 4) {
                        number = 1;
                    } else if (number == 7) {
                        number = 2;
                    } else {
                        return;
                    }
                    flag[mianZi.cards[0].type.ordinal()][number] = true;
                }
            });
            plate.fulus.forEach(fulu -> {
                if (fulu.isChi()) {
                    int number = fulu.getChiStart().number;
                    if (number == 1) {
                        number = 0;
                    } else if (number == 4) {
                        number = 1;
                    } else if (number == 7) {
                        number = 2;
                    } else {
                        return;
                    }
                    flag[fulu.getChiStart().type.ordinal()][number] = true;
                }
            });
            for (boolean[] booleans : flag) {
                if (booleans[0] && booleans[1] && booleans[2]) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.YiQiTongGuan, menqianqing ? 2 : 1));
                }
            }
        }
        // 三色同顺
        {
            final boolean[][] flag = new boolean[3][10];
            splitWay.mianzi.forEach(mianZi -> {
                if (mianZi.isChi()) {
                    int number = mianZi.cards[0].number;
                    flag[mianZi.cards[0].type.ordinal()][number] = true;
                }
            });
            plate.fulus.forEach(fulu -> {
                if (fulu.isChi()) {
                    int number = fulu.getChiStart().number;
                    flag[fulu.getChiStart().asIgnoreRedDora().type.ordinal()][number] = true;
                }
            });
            for (int i = 0; i < 10; i++) {
                if (flag[0][i] && flag[1][i] && flag[2][i]) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.SanSeTongShun, menqianqing ? 2 : 1));
                }
            }
        }
        // 混一色
        // 字一色
        // 清一色
        {
            Card.Type type = null;
            boolean zipai = false;
            boolean yise = true;
            if (card.type == Card.Type.Z) {
                zipai = true;
            } else {
                type = card.type;
            }
            for (Card c : plate.cards) {
                if (c.type == Card.Type.Z) {
                    zipai = true;
                } else if (type == null) {
                    type = c.type;
                } else if (type != c.type) {
                    yise = false;
                }
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.obtain != null) {
                    Card c = fulu.obtain;
                    if (c.type == Card.Type.Z) {
                        zipai = true;
                    } else if (type == null) {
                        type = c.type;
                    } else if (type != c.type) {
                        yise = false;
                    }
                }
                for (Card c : fulu.original) {
                    if (c.type == Card.Type.Z) {
                        zipai = true;
                    } else if (type == null) {
                        type = c.type;
                    } else if (type != c.type) {
                        yise = false;
                    }
                }
            }
            if (yise) {
                if (type == null) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.ZiYiSe, -1));
                } else if (zipai) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.HunYiSe, menqianqing ? 3 : 2));
                } else {
                    yizhong.add(new YiZhong(YiZhongEnumerate.QingYiSe, menqianqing ? 6 : 5));
                }
            }
        }
        // 小四喜
        // 大四喜
        {
            int count = 0;
            for (MianZi mianZi : splitWay.mianzi) {
                if (mianZi.cards[0].isFeng()) {
                    count++;
                }
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.original.get(0).isFeng()) {
                    count++;
                }
            }
            if (count == 3 && splitWay.quetou[0].isFeng()) {
                yizhong.add(new YiZhong(YiZhongEnumerate.XiaoSiXi, -1));
            } else if (count == 4) {
                yizhong.add(new YiZhong(YiZhongEnumerate.DaSiXi, -2));
            }
        }
        // 绿一色
        {
            boolean lvyise = card.isGreen();
            for (Card c : plate.cards) {
                lvyise &= c.isGreen();
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.obtain != null) {
                    lvyise &= fulu.obtain.isGreen();
                }
                for (Card c : fulu.original) {
                    lvyise &= c.isGreen();
                }
            }
            if (lvyise) {
                yizhong.add(new YiZhong(YiZhongEnumerate.LvYiSe, -1));
            }
        }
        // 九莲宝灯
        // 纯正九莲宝灯
        if (menqianqing && plate.fulus.size() == 0) {
            Card.Type type = card.type;
            boolean jiulianbaodeng = true;
            if (type == Card.Type.Z) {
                jiulianbaodeng = false;
            }
            int[] count = new int[10];
            for (Card c : plate.cards) {
                if (c.type != type) {
                    jiulianbaodeng = false;
                }
                count[c.getNumber()]++;
            }
            boolean chunzheng = count[1] == 3 && count[9] == 3;
            for (int i = 2; i <= 8; i++) {
                chunzheng &= count[i] == 1;
            }
            count[card.getNumber()]++;
            if (count[1] < 3 || count[9] < 3) {
                jiulianbaodeng = false;
            }
            for (int i = 2; i <= 8; i++) {
                jiulianbaodeng &= count[i] >= 1;
            }
            if (jiulianbaodeng) {
                if (chunzheng || env.tianhu) {
                    yizhong.add(new YiZhong(YiZhongEnumerate.ChunZhengJiuLian, -2));
                } else {
                    yizhong.add(new YiZhong(YiZhongEnumerate.JiuLianBaoDeng, -1));
                }
            }
        }
        // 国士无双
        // 国士无双十三面
        if (splitWay.guoshiwushuang) {
            boolean shisanmian = false;
            for (Card c : plate.cards) {
                if (c.equals(card)) {
                    shisanmian = true;
                    break;
                }
            }
            if (shisanmian || env.tianhu) {
                yizhong.add(new YiZhong(YiZhongEnumerate.GuoShiShiSanMian, -2));
            } else {
                yizhong.add(new YiZhong(YiZhongEnumerate.GuoShiWuShuang, -1));
            }
        }

        // 宝牌
        {
            int doraCount = 0;
            for (Card dora : env.dora) {
                if (dora.equalsIgnoreRedDora(new Card(Card.Type.Z, 4))) {
                    doraCount += plate.bei;
                }
                if (card.equalsIgnoreRedDora(dora)) {
                    doraCount++;
                }
                for (Card c : plate.cards) {
                    if (c.equalsIgnoreRedDora(dora)) {
                        doraCount++;
                    }
                }
                for (Fulu fulu : plate.fulus) {
                    if (fulu.obtain != null && fulu.obtain.equalsIgnoreRedDora(dora)) {
                        doraCount++;
                    }
                    if (fulu.jiagang != null && fulu.jiagang.equalsIgnoreRedDora(dora)) {
                        doraCount++;
                    }
                    for (Card c : fulu.original) {
                        if (c.equalsIgnoreRedDora(dora)) {
                            doraCount++;
                        }
                    }
                }
            }
            if (doraCount > 0) {
                yizhong.add(new YiZhong(YiZhongEnumerate.Dora, doraCount, true));
            }
        }
        // 红宝牌
        {
            int doraCount = 0;
            if (card.isRedDora()) {
                doraCount++;
            }
            for (Card c : plate.cards) {
                if (c.isRedDora()) {
                    doraCount++;
                }
            }
            for (Fulu fulu : plate.fulus) {
                if (fulu.obtain != null && fulu.obtain.isRedDora()) {
                    doraCount++;
                }
                if (fulu.jiagang != null && fulu.jiagang.isRedDora()) {
                    doraCount++;
                }
                for (Card c : fulu.original) {
                    if (c.isRedDora()) {
                        doraCount++;
                    }
                }
            }
            if (doraCount > 0) {
                yizhong.add(new YiZhong(YiZhongEnumerate.ChiDora, doraCount, true));
            }
        }
        // 拔北宝牌
        if (plate.bei > 0) {
            yizhong.add(new YiZhong(YiZhongEnumerate.BeiDora, plate.bei, true));
        }
        // 里宝牌
        if (richi != RiChiType.None) {
            int doraCount = 0;
            for (Card dora : env.ridora) {
                if (dora.equalsIgnoreRedDora(new Card(Card.Type.Z, 4))) {
                    doraCount += plate.bei;
                }
                if (card.equalsIgnoreRedDora(dora)) {
                    doraCount++;
                }
                for (Card c : plate.cards) {
                    if (c.equalsIgnoreRedDora(dora)) {
                        doraCount++;
                    }
                }
                for (Fulu fulu : plate.fulus) {
                    if (fulu.obtain != null && fulu.obtain.equalsIgnoreRedDora(dora)) {
                        doraCount++;
                    }
                    if (fulu.jiagang != null && fulu.jiagang.equalsIgnoreRedDora(dora)) {
                        doraCount++;
                    }
                    for (Card c : fulu.original) {
                        if (c.equalsIgnoreRedDora(dora)) {
                            doraCount++;
                        }
                    }
                }
            }
            yizhong.add(new YiZhong(YiZhongEnumerate.LiDora, doraCount, true));
        }

        // 如果有役满，去掉其他役种
        for (YiZhong yiZhong : yizhong) {
            if (yiZhong.fan < 0) {
                yizhong.removeIf(yiZhong1 -> yiZhong1.fan >= 0);
                pinghu = false;
                break;
            }
        }
        // 如果有混老头，则不计混全带幺九
        for (YiZhong yi : yizhong) {
            if (yi.yiZhongEnumerate == YiZhongEnumerate.HunLaoTou) {
                yizhong.removeIf(y -> y.yiZhongEnumerate == YiZhongEnumerate.HunQuanDaiYaoJiu);
                break;
            }
        }

        // 计算番数
        int fan = 0;
        for (YiZhong yi : yizhong) {
            fan += yi.fan;
        }

        // 计算符数
        int fu;
        if (splitWay.qiduizi || splitWay.guoshiwushuang) {
            fu = 25;
        } else {
            if (pinghu) {
                // 平和荣和 30 符
                // 平和自摸 20 符
                fu = zimo ? 20 : 30;
            } else {
                // 符底 20 符
                fu = 20;
                // 中张明刻 2 符，暗刻 4 符，明杠 8 符，暗杠 16 符
                // 幺九明刻 4 符，暗刻 8 符，明杠 16 符，暗杠 32 符
                for (Fulu fulu : plate.fulus) {
                    int base = 2;
                    if (fulu.isGang()) {
                        base *= 4;
                        if (fulu.original.get(0).isYaoJiu()) {
                            base *= 2;
                        }
                        if (fulu.isAnGang()) {
                            base *= 2;
                        }
                        fu += base;
                    } else if (fulu.isPeng()) {
                        if (fulu.original.get(0).isYaoJiu()) {
                            base *= 2;
                        }
                        fu += base;
                    }
                }
                for (MianZi mianZi : splitWay.mianzi) {
                    int base = 4;
                    if (mianZi.isPong()) {
                        if (mianZi.cards[0].isYaoJiu()) {
                            base *= 2;
                        }
                        // 双碰听且没有自摸，对应的刻符数除以 2（因为被算作明刻）
                        if (listenType == 4 && !zimo && mianZi.cards[0].equalsIgnoreRedDora(card)) {
                            base /= 2;
                        }
                        fu += base;
                    }
                }
                // 雀头，役牌雀头 2 符，连风牌 4 符
                if (splitWay.quetou[0].type == Card.Type.Z) {
                    int num = splitWay.quetou[0].number;
                    // 三元牌
                    if (num == 5 || num == 6 || num == 7) {
                        fu += 2;
                    }
                    // 连风牌
                    else if (env.changfeng == env.menfeng && num == env.changfeng.number) {
                        fu += 4;
                    }
                    // 场风、门风
                    else if (num == env.changfeng.number || num == env.menfeng.number) {
                        fu += 2;
                    }
                }
                // 听牌型，嵌张、边张、单骑 2 符
                if ((listenType & 26) != 0) {
                    fu += 2;
                }
                // 自摸 2 符
                if (zimo) {
                    fu += 2;
                }
                // 切上
                if (fu % 10 != 0) {
                    fu += 10 - fu % 10;
                }
                // 至少 30 符
                if (fu == 20) {
                    fu = 30;
                }
                // 门清荣和 10 符
                if (menqianqing && !zimo) {
                    fu += 10;
                }
            }
        }

        return new YiZhongResult(fan, fu, yizhong);
    }

    /**
     * 流满检查
     *
     * @param shezhang 舍张
     * @return 如果流满，返回 true，否则返回 false
     */
    public static boolean isLiuman(ArrayList<Shezhang> shezhang) {
        for (Shezhang sz : shezhang) {
            if (sz.status == Shezhang.Status.Obtained || !sz.card.isYaoJiu()) {
                return false;
            }
        }
        return true;
    }
}
