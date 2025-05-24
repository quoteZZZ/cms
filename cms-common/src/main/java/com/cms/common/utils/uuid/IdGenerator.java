package com.cms.common.utils.uuid;

import java.util.concurrent.atomic.AtomicLong;

/**
 * OptimizedIdGenerator 类用于生成唯一的 ID，采用雪花算法（Snowflake Algorithm）生成基于时间戳、机器ID和序列号的唯一ID。
 * 然后将该 ID 转换为 Base58 编码的字符串，用于满足前端展示需求。
 *
 * 亮点：
 * 1. **采用雪花算法生成唯一ID**：该算法能够保证在分布式系统中每台机器生成的ID都是唯一的，通过时间戳、机器ID和序列号拼接生成ID。
 * 2. **生成 7 位唯一ID**：为了确保生成的唯一ID长度为7位数字，我们将生成的 ID 强制限制在一个较小的范围内（最大 9999999）。
 * 3. **高效并发**：使用 AtomicLong 来生成序列号，保证线程安全，避免使用全局锁，从而提高并发性能。
 * 4. **Base58 编码**：通过 Base58 编码，将生成的 long 类型 ID 转换为易于展示的字符串。Base58 编码减少了与 URL 等特殊字符的冲突。
 * 5. **纪元时间灵活性**：自定义纪元时间（例如：2025年1月1日），使得 ID 生成的时间更加灵活，适应不同需求。
 * 6. **ID限制为7位**：生成的 ID 被限制为7位的 long 数字，确保不会超过该长度，满足展示需求。
 */
public class IdGenerator {

    // 自定义纪元（2025年1月1日），单位为秒
    private final long twepoch = 1672444800L;

    // 位数配置：机器ID占3位，最多支持8台机器；每秒内序列号占5位，最多支持32个ID。
    private final long workerIdBits = 3L;
    private final long sequenceBits = 5L;

    // 计算位移：将生成的ID分为不同的部分，时间戳、机器ID和序列号
    private final long workerIdShift = sequenceBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits;

    // 序列号掩码（最大支持每秒生成32个ID）
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long workerId;  // 当前机器的workerId（需要设置）
    private AtomicLong sequence = new AtomicLong(0L);  // 序列号，使用原子操作保证线程安全
    private long lastTimestamp = -1L;  // 上次生成ID的时间戳

    // 最大支持的ID值，用于确保生成的ID不超过7位数字
    private final long maxId = 9999999L;

    /**
     * 构造方法，workerId 用来标识不同的机器或节点。
     * @param workerId 当前机器的 workerId
     * @throws IllegalArgumentException 如果 workerId 不在允许的范围内则抛出异常
     */
    public IdGenerator(long workerId) {
        if (workerId > (1L << workerIdBits) - 1 || workerId < 0) {
            throw new IllegalArgumentException("workerId can't be greater than " + ((1L << workerIdBits) - 1) + " or less than 0");
        }
        this.workerId = workerId;
    }

    /**
     * 生成唯一的ID，使用雪花算法生成的 ID 是 long 类型。
     * 并强制限制 ID 在 7 位数字以内（最大值为9999999）。
     * @return 生成的唯一ID（long 类型）
     */
    public long nextId() {
        long timestamp = System.currentTimeMillis() / 1000;  // 时间精度调整为秒

        // 如果时钟回退，抛出异常，防止生成重复的ID
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        // 如果当前时间戳相同，增加序列号
        if (lastTimestamp == timestamp) {
            long seq = sequence.incrementAndGet() & sequenceMask;  // 增加序列号
            if (seq == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);  // 如果序列号用完，等待下一个毫秒
            }
        } else {
            sequence.set(0);  // 时间戳变化时，重置序列号
        }

        lastTimestamp = timestamp;

        // 生成唯一ID
        long id = ((timestamp - twepoch) << timestampLeftShift) | (workerId << workerIdShift) | sequence.get();

        // 限制 ID 不超过 9999999（7 位数字）
        return id % (maxId + 1);  // 确保ID不会超过最大值并且不超过 7 位数
    }

    /**
     * 等待直到下一毫秒，用于处理序列号用完的情况。
     * @param lastTimestamp 上次生成 ID 的时间戳
     * @return 下一个时间戳
     */
    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis() / 1000;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() / 1000;
        }
        return timestamp;
    }

    /**
     * 将 long 类型的 ID 转换为 Base58 编码的字符串。
     * @param id long 类型的 ID
     * @return Base58 编码的 ID 字符串
     */
    public static String toBase58(long id) {
        final String base58Chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(base58Chars.charAt((int) (id % 58)));
            id /= 58;
        }
        return sb.reverse().toString();  // 反转字符串以得到最终的 Base58 编码
    }

    /**
     * 静态方法生成 ID 并将其转换为 Base58 编码的字符串
     *
     * @param workerId 当前机器的 workerId
     * @return Base58 编码的 ID 字符串
     */
    public static long generateId(long workerId) {
        IdGenerator generator = new IdGenerator(workerId);
        return generator.nextId();  // 直接返回生成的 long 类型 ID
    }
}
