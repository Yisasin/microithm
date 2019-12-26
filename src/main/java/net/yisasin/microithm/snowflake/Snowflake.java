package net.yisasin.microithm.snowflake;

import java.util.Date;

/**
 * This is the main class for using Snowflake. You usually call these two methods {@link #nextId()} and {@link #convert(long)}
 *
 * <p>Here is an example of how snowflake is used for a simple Class.
 *
 * <pre>
 * final Snowflake idGenerator = new Snowflake(13, 17);     // build a Snowflake Object for workerid with 13 and datacentedid with 17
 * long id = idGenerator.nextId();                          // generator id
 * SnowMeta snowMeta = idGenerator.convert(id);             // parse id
 * </pre>
 *
 *
 * @author maxiaofeng
 */
public class Snowflake {

    // 初始时间截 (2017-01-01)
    private static final long INITIAL_TIME_STAMP = 1483200000000L;

    // 机器ID所占的位数
    private static final long WORKER_ID_BITS = 5L;

    // 数据标识ID所占的位数
    private static final long DATACENTER_ID_BITS = 5L;

    // 支持的最大机器ID，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    // 支持的最大数据标识id，结果是31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    // 序列在ID中占的位数
    private static final long SEQUENCE_BITS = 12L;

    // 机器ID的偏移量(12)
    private static final long WORKERID_OFFSET = SEQUENCE_BITS;

    // 数据中心ID的偏移量(12+5)
    private static final long DATACENTERID_OFFSET = SEQUENCE_BITS + WORKER_ID_BITS;

    // 时间戳的偏移量(5+5+12)
    private static final long TIMESTAMP_OFFSET = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    // 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    // 机器ID掩码31
    private static final long WORKER_ID_MASK = ~(-1L << WORKER_ID_BITS);
    // 数据标识ID掩码31
    private static final long DATACENTER_ID_MASK = ~(-1L << DATACENTER_ID_BITS);
    // 时间戳掩码2的41次方减1
    private static final long TIMESTAMP_MASK = ~(-1L << 41L);

    // 工作节点ID(0~31)
    private final long workerId;

    // 数据中心ID(0~31)
    private final long datacenterId;

    // 毫秒内序列(0~4095)
    private long sequence = 0L;

    // 上次生成ID的时间截
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param workerId 工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public Snowflake(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("WorkerID 不能大于 %d 或小于 0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("DataCenterID 不能大于 %d 或小于 0", MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 获得下一个ID (用同步锁保证线程安全)
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        //  如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("当前时间小于上一次记录的时间戳！");
        }
        //  如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
        //  sequence等于0说明毫秒内序列已经增长到最大值
            if (sequence == 0) {
        //  阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        //  时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }
        //  上次生成ID的时间截
        lastTimestamp = timestamp;

        //  移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - INITIAL_TIME_STAMP) << TIMESTAMP_OFFSET)
                | (datacenterId << DATACENTERID_OFFSET)
                | (workerId << WORKERID_OFFSET)
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 反解 ID
     * @param id ID号
     * @return 对应原数据
     */
    public SnowMeta convert(long id) {
        SnowMeta ret = new SnowMeta();

        ret.setSequence(id & Snowflake.SEQUENCE_MASK);

        ret.setWorkerID((id >>> Snowflake.WORKERID_OFFSET) & Snowflake.WORKER_ID_MASK);

        ret.setDatacenterID((id >>> Snowflake.DATACENTERID_OFFSET) & Snowflake.DATACENTER_ID_MASK);

        ret.setTimeStamp((id >>> Snowflake.TIMESTAMP_OFFSET) & Snowflake.TIMESTAMP_MASK);

        ret.setGenerationTime(new Date(ret.getTimeStamp() + Snowflake.INITIAL_TIME_STAMP));
        return ret;
    }

}
