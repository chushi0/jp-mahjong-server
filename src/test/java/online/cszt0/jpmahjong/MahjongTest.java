package online.cszt0.jpmahjong;

import online.cszt0.jpmahjong.game.Mahjong;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MahjongTest {

    @Test
    public void winTest() {
        Mahjong.Environment environment = new Mahjong.Environment();
        environment.changfeng = Mahjong.Feng.Dong;
        environment.menfeng = Mahjong.Feng.Nan;
        environment.dora.add(new Mahjong.Card(Mahjong.Card.Type.M, 3));
        environment.ridora.add(new Mahjong.Card(Mahjong.Card.Type.S, 8));

        // 立直，自摸，断幺九，平和，三色同顺，一杯口，宝牌2，里宝牌2，11番20符 三倍满
        Mahjong.Plate plate = new Mahjong.Plate();
        plate.cards.addAll(Arrays.asList(
                new Mahjong.Card(Mahjong.Card.Type.M, 2),
                new Mahjong.Card(Mahjong.Card.Type.M, 2),
                new Mahjong.Card(Mahjong.Card.Type.M, 3),
                new Mahjong.Card(Mahjong.Card.Type.M, 3),
                new Mahjong.Card(Mahjong.Card.Type.M, 4),
                new Mahjong.Card(Mahjong.Card.Type.M, 4),
                new Mahjong.Card(Mahjong.Card.Type.P, 2),
                new Mahjong.Card(Mahjong.Card.Type.P, 3),
                new Mahjong.Card(Mahjong.Card.Type.P, 4),
                new Mahjong.Card(Mahjong.Card.Type.S, 2),
                new Mahjong.Card(Mahjong.Card.Type.S, 3),
                new Mahjong.Card(Mahjong.Card.Type.S, 8),
                new Mahjong.Card(Mahjong.Card.Type.S, 8)
        ));
        Mahjong.WinResult result = Mahjong.checkWin(plate, new Mahjong.Card(Mahjong.Card.Type.S, 4), environment, Mahjong.CardSource.ZiMo, Mahjong.RiChiType.RiChi, false);
        assert result != null && result.fan == 11 && result.fu == 20;


        // 国士无双十三面，-2番25符，两倍役满
        plate.cards.clear();
        plate.cards.addAll(Arrays.asList(
                new Mahjong.Card(Mahjong.Card.Type.M, 1),
                new Mahjong.Card(Mahjong.Card.Type.M, 9),
                new Mahjong.Card(Mahjong.Card.Type.P, 1),
                new Mahjong.Card(Mahjong.Card.Type.P, 9),
                new Mahjong.Card(Mahjong.Card.Type.S, 1),
                new Mahjong.Card(Mahjong.Card.Type.S, 9),
                new Mahjong.Card(Mahjong.Card.Type.Z, 1),
                new Mahjong.Card(Mahjong.Card.Type.Z, 2),
                new Mahjong.Card(Mahjong.Card.Type.Z, 3),
                new Mahjong.Card(Mahjong.Card.Type.Z, 4),
                new Mahjong.Card(Mahjong.Card.Type.Z, 5),
                new Mahjong.Card(Mahjong.Card.Type.Z, 6),
                new Mahjong.Card(Mahjong.Card.Type.Z, 7)
        ));
        result = Mahjong.checkWin(plate, new Mahjong.Card(Mahjong.Card.Type.S, 1), environment, Mahjong.CardSource.ZiMo, Mahjong.RiChiType.None, false);
        assert result != null && result.fan == -2 && result.fu == 25;


        // 纯正九莲宝灯，-2番，两倍役满
        plate.cards.clear();
        plate.cards.addAll(Arrays.asList(
                new Mahjong.Card(Mahjong.Card.Type.M, 1),
                new Mahjong.Card(Mahjong.Card.Type.M, 1),
                new Mahjong.Card(Mahjong.Card.Type.M, 1),
                new Mahjong.Card(Mahjong.Card.Type.M, 2),
                new Mahjong.Card(Mahjong.Card.Type.M, 3),
                new Mahjong.Card(Mahjong.Card.Type.M, 4),
                new Mahjong.Card(Mahjong.Card.Type.M, 5),
                new Mahjong.Card(Mahjong.Card.Type.M, 6),
                new Mahjong.Card(Mahjong.Card.Type.M, 7),
                new Mahjong.Card(Mahjong.Card.Type.M, 8),
                new Mahjong.Card(Mahjong.Card.Type.M, 9),
                new Mahjong.Card(Mahjong.Card.Type.M, 9),
                new Mahjong.Card(Mahjong.Card.Type.M, 9)
        ));
        result = Mahjong.checkWin(plate, new Mahjong.Card(Mahjong.Card.Type.M, 5), environment, Mahjong.CardSource.ZiMo, Mahjong.RiChiType.None, false);
        assert result != null && result.fan == -2;

        // 立直、役牌发、役牌中、对对和、三暗刻、宝牌3、拔北宝牌2、里宝牌3，15番50符，累计役满
        environment.dora.clear();
        environment.ridora.clear();
        environment.dora.add(new Mahjong.Card(Mahjong.Card.Type.S, 4));
        environment.ridora.add(new Mahjong.Card(Mahjong.Card.Type.S, 2));
        plate.cards.clear();
        plate.cards.addAll(Arrays.asList(
                new Mahjong.Card(Mahjong.Card.Type.P, 4),
                new Mahjong.Card(Mahjong.Card.Type.P, 4),
                new Mahjong.Card(Mahjong.Card.Type.S, 2),
                new Mahjong.Card(Mahjong.Card.Type.S, 2),
                new Mahjong.Card(Mahjong.Card.Type.S, 2),
                new Mahjong.Card(Mahjong.Card.Type.S, 4),
                new Mahjong.Card(Mahjong.Card.Type.S, 4),
                new Mahjong.Card(Mahjong.Card.Type.S, 4),
                new Mahjong.Card(Mahjong.Card.Type.Z, 6),
                new Mahjong.Card(Mahjong.Card.Type.Z, 6),
                new Mahjong.Card(Mahjong.Card.Type.Z, 6),
                new Mahjong.Card(Mahjong.Card.Type.Z, 7),
                new Mahjong.Card(Mahjong.Card.Type.Z, 7)
        ));
        plate.bei = 2;
        result = Mahjong.checkWin(plate, new Mahjong.Card(Mahjong.Card.Type.Z, 7), environment, Mahjong.CardSource.RongHu, Mahjong.RiChiType.RiChi, false);
        assert result != null && result.fan == 15 && result.fu == 50;
    }
}
