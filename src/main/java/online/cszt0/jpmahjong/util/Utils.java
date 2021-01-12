package online.cszt0.jpmahjong.util;

import java.util.Map;
import java.util.Random;

public class Utils {
    /**
     * 生成随机 key
     *
     * @return key
     */
    public static String randomKey() {
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

    /**
     * 生成随机 id，并将对象添加到唯一管理集合。
     * yuan
     * @param uniqueIdMap id 与对象映射集合
     * @param object 持有此id的对象
     * @param <T> 对象的类型
     * @return 生成的 id
     */
    public static <T> String randomUniqueId(Map<String, T> uniqueIdMap, T object) {
        String id;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (uniqueIdMap) {
            do {
                id = randomKey();
            } while (uniqueIdMap.containsKey(id));
            uniqueIdMap.put(id, object);
        }
        return id;
    }
}
