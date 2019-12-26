package net.yisasin.microithm.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SnowflakeTest {

    @Test
    public void nextId() {

        final Snowflake idGenerator = new Snowflake(13, 17);

        int expectSize = 1000000;
        // 线程池并行执行 expectSize 次ID生成
        ExecutorService executorService = Executors.newCachedThreadPool();
        Set<Long> idSet = ConcurrentHashMap.newKeySet((int)(expectSize / 0.75) + 1);

        // 生成 ID START
        LocalDateTime start = LocalDateTime.now();
        for (int i = 0; i < expectSize; i++) {
            executorService.execute(() -> idSet.add(idGenerator.nextId()));
        }
        executorService.shutdown();
        LocalDateTime end = LocalDateTime.now();
        // 生成 ID END

        Duration duration = Duration.between(start, end);
        long millis = duration.toMillis();

        log.info(String.format("计划生成 %s 个ID共用 %d 毫秒。", expectSize, millis));

        Assert.assertEquals(expectSize, idSet.size());
    }

    @Test
    public void convert() {
    }
}