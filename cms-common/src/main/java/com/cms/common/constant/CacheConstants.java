package com.cms.common.constant;

/**
 * 缓存Key常量定义类
 *
 * 统一定义各模块使用的缓存Key前缀，避免硬编码，便于缓存清理、监控和维护。
 * 所有Key均以冒号结尾，拼接时更清晰易读。
 *
 * 示例：
 * - 登录用户缓存： "login_tokens:{token}"
 * - 竞赛信息缓存： "sys_comp:{compId}"
 * - 竞赛列表缓存： "comp:list:{md5摘要}"
 */
public final class CacheConstants {

    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录密码错误次数 redis key 前缀
     * 用于记录用户登录密码错误次数
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 竞赛信息缓存 key (单个竞赛信息)
     */
    public static final String COMP_INFO_KEY = "sys_comp:info:";

    /**
     * 竞赛列表缓存 key (查询结果列表)
     */
    public static final String COMP_LIST_KEY = "sys_comp:list:";

    /**
     * 竞赛空值缓存 key (处理缓存穿透)
     */
    public static final String COMP_EMPTY_KEY = "sys_comp:empty:";

    /**
     * 竞赛逻辑过期缓存 key (异步更新机制)
     */
    public static final String COMP_LOGICAL_EXPIRE_KEY = "sys_comp:expire:";

    /**
     * 竞赛分布式锁前缀
     */
    public static final String COMP_LOCK_KEY = "sys_comp:lock:";

    /**
     * 竞赛推荐缓存 key
     */
    public static final String COMP_RECOMMEND_KEY = "sys_comp:recommend:";

    /**
     * 竞赛用户关联缓存 key
     */
    public static final String USER_COMP_KEY = "sys_user_comp:";

    // ============== 缓存TTL相关常量 ==============

    /**
     * 默认缓存过期时间（秒）
     * 10分钟 = 600秒
     */
    public static final int DEFAULT_CACHE_TTL = 600;

    /**
     * 空值缓存过期时间（秒）
     * 空值缓存时间较短，避免长时间占用内存
     * 2分钟 = 120秒
     */
    public static final int EMPTY_CACHE_TTL = 120;

    /**
     * 逻辑过期时间（秒）
     * 1小时 = 3600秒
     */
    public static final int LOGICAL_EXPIRE_TTL = 3600;

    /**
     * 缓存逻辑过期检查窗口（秒）
     * 当缓存剩余时间小于此值时，触发异步更新
     * 60秒 = 1分钟
     */
    public static final int LOGICAL_EXPIRE_CHECK_WINDOW = 60;

    /**
     * 防雪崩随机TTL最大值（秒）
     * 用于在基础TTL上增加随机时间，避免同时过期
     */
    public static final int CACHE_TTL_RANDOM_MAX = 300;

    /**
     * 分布式锁默认超时时间（秒）
     */
    public static final int LOCK_TIMEOUT = 10;

    public static final String JUDGE_ROLE_KEY = "sys_role:judge:";
}
