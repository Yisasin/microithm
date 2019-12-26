package net.yisasin.microithm.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SnowflakeTest {

    private Snowflake idGenerator;

    @Before
    public void init() {
        idGenerator = new Snowflake(13, 17);
    }

    @Test
    public void nextId() {

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
    public void convert() throws ParseException {

        SnowMeta convert = idGenerator.convert(1L);
        Date generationTime = convert.getGenerationTime();

        Assert.assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01"), generationTime);
    }
}