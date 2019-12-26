package net.yisasin.microithm.snowflake;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * This is the meta data for snowflake algorithm
 *
 * @author maxiaofeng[yisasin@163.com]
 */
@Data
public class SnowMeta implements Serializable {

    private Date generationTime;
    private long timeStamp;
    private long workerID;
    private long datacenterID;
    private long sequence;

}
