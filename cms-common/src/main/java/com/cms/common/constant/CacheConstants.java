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
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    // ------------------ 通知相关 ------------------

    /**
     * 通知信息缓存 key 前缀
     */
    public static final String NOTICE_INFO_KEY = "sys_notice:";

    /**
     * 通知列表缓存 key 前缀（精细化缓存 key）
     */
    public static final String NOTICE_LIST_KEY = "notice:list:";

    // ------------------ 审批相关 ------------------

    /**
     * 审批信息缓存 key 前缀
     */
    public static final String APPROVAL_INFO_KEY = "sys_approval:";

    /**
     * 审批列表缓存 key 前缀（精细化缓存 key）
     */
    public static final String APPROVAL_LIST_KEY = "approval:list:";

    // ------------------ 奖项相关 ------------------

    /**
     * 奖项信息缓存 key 前缀
     */
    public static final String AWARD_INFO_KEY = "sys_award:";

    /**
     * 奖项列表缓存 key 前缀
     */
    public static final String AWARD_LIST_KEY = "award:list:";

    /**
     * 奖项统计缓存 key 前缀
     */
    public static final String AWARD_STATISTICS_KEY = "award:statistics:";

    // ------------------ 竞赛相关 ------------------

    /**
     * 竞赛信息缓存 key 前缀
     */
    public static final String COMP_INFO_KEY = "sys_comp:";

    /**
     * 竞赛列表缓存 key 前缀（精细化缓存 key）
     */
    public static final String COMP_LIST_KEY = "comp:list:";

    /**
     * 竞赛推荐缓存 key 前缀
     */
    public static final String COMP_RECOMMEND_KEY = "comp:recommend:";

    // ------------------ 逻辑过期策略相关常量 ------------------

    /**
     * 逻辑过期时间（单位：秒）
     */
    public static final long LOGICAL_EXPIRE_TIME = 60 * 60; // 1小时

    /**
     * 逻辑过期检查窗口（单位：秒）
     */
    public static final long LOGICAL_EXPIRE_CHECK_WINDOW = 60 * 5; // 5分钟

    // ------------------ 素材/资料相关 ------------------

    /**
     * 资料信息缓存 key 前缀
     */
    public static final String MATERIAL_INFO_KEY = "sys_material:";

    /**
     * 资料列表缓存 key 前缀（精细化缓存 key）
     */
    public static final String MATERIAL_LIST_KEY = "material:list:";

    // ------------------ 反馈相关 ------------------

    /**
     * 反馈信息缓存 key 前缀
     */
    public static final String FEEDBACK_INFO_KEY = "sys_feedback:";

    /**
     * 反馈列表缓存 key 前缀
     */
    public static final String FEEDBACK_LIST_KEY = "feedback:list:";

    // ------------------ 参与相关 ------------------

    /**
     * 参与信息缓存 key 前缀
     */
    public static final String PARTICIPATION_INFO_KEY = "sys_participation:";

    /**
     * 参与列表缓存 key 前缀
     */
    public static final String PARTICIPATION_LIST_KEY = "participation:list:";

    // ------------------ 评分相关 ------------------

    /**
     * 评分信息缓存 key 前缀
     */
    public static final String SCORE_INFO_KEY = "sys_score:";

    /**
     * 评分列表缓存 key 前缀
     */
    public static final String SCORE_LIST_KEY = "score:list:";

    // ------------------ 部门相关 ------------------

    /**
     * 部门信息缓存 key 前缀
     */
    public static final String DEPT_INFO_KEY = "sys_dept:";

    /**
     * 部门列表缓存 key 前缀
     */
    public static final String DEPT_LIST_KEY = "dept:list:";

    // ------------------ 作品/提交相关 ------------------

    /**
     * 作品信息缓存 key 前缀
     */
    public static final String SUBMISSION_INFO_KEY = "sys_submission:";

    /**
     * 作品列表缓存 key 前缀
     */
    public static final String SUBMISSION_LIST_KEY = "submission:list:";

    /**
     * 作品最大文件key 前缀
     */
    public static final Integer MAX_SUBMISSION_FILE_SIZE = 1024 * 1024 * 10; // 10M

    // ------------------ 其它 ------------------

    /**
     * 角色信息缓存 key 前缀
     */
    public static final String ROLE_KEY = "sys_role:";

    /**
     * 角色竞赛关联缓存 key 前缀
     */
    public static final String ROLE_COMP_KEY = "sys_role_comp:";

    /**
     * 角色菜单关联缓存 key 前缀
     */
    public static final String ROLE_MENU_KEY = "sys_role_menu:";

    /**
     * 用户信息缓存 key 前缀
     */
    public static final String USER_KEY = "sys_user:";

    /**
     * 用户竞赛关联缓存 key 前缀
     */
    public static final String USER_COMP_KEY = "sys_user_comp:";

    /**
     * 用户角色关联缓存 key 前缀
     */
    public static final String USER_ROLE_KEY = "sys_user_role:";

    // 缓存 TTL 与延时删除时间配置

    /**
     * 缓存默认过期时间（秒）
     */
    public static final Long CACHE_TTL_SECONDS = 3600L;

    /**
     * 延时删除时间（毫秒）
     */
    public static final Long DELAY_DELETE_MS = 500L;

    /**
     * 默认缓存时间（秒）
     */
    public static final Integer DEFAULT_CACHE_TTL = 3600; // 统一缓存时间（秒）

    // 禁止实例化
    private CacheConstants() {}
}