package com.cms.common.utils;

import java.nio.charset.Charset;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.cms.common.constant.Constants;

/**
 * Redis使用FastJson（反）序列化：
 * 定义了一个使用 FastJson 进行序列化和反序列化的 Redis 序列化器，
 * 确保 Java 对象可以正确存储和读取于 Redis 中。
 * 序列化：将 Java 对象转换为 JSON 字符串，并存储到 Redis 中，确保复杂对象结构完整保存。
 * 反序列化：从 Redis 中读取 JSON 字符串，并将其解析为原始 Java 对象，保证数据一致性和可读性。
 */
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T> {

    // 默认编码
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    // 默认白名单
    static final Filter AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(Constants.JSON_WHITELIST_STR);

    // 目标类型
    private final Class<T> clazz;

    // 构造函数
    public FastJson2JsonRedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    // 序列化：增加 WriteMapNullValue 选项，确保空字段也能写入
    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName, JSONWriter.Feature.WriteMapNullValue)
                .getBytes(DEFAULT_CHARSET);
    }


    // 反序列化：从字节数组转换为字符串，再解析为目标类型对象
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);
        return JSON.parseObject(str, clazz, AUTO_TYPE_FILTER);
    }
}
