package com.cms.system.service.impl;

import com.cms.common.annotation.DataSource;
import com.cms.common.constant.CacheConstants;
import com.cms.common.constant.UserConstants;
import com.cms.common.core.redis.RedisCache;
import com.cms.common.core.text.Convert;
import com.cms.common.enums.DataSourceType;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.StringUtils;
import com.cms.common.core.domain.entity.SysConfig;
import com.cms.system.mapper.SysConfigMapper;
import com.cms.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

/**
 * 系统参数配置业务层实现类：
 * 此类提供了对系统参数的增删改查、缓存管理及其他操作的实现。
 * 主要依赖 Redis 缓存和数据库 Mapper 来管理系统配置。
 * @author
 */
@Service // 声明为服务层组件，方便 Spring 扫描和管理
public class SysConfigServiceImpl implements ISysConfigService {

    //SysConfigMapper 用于数据库访问
    @Autowired
    private SysConfigMapper configMapper;


    // RedisCache 用于缓存操作
    @Autowired
    private RedisCache redisCache;

    /**
     * 初始化方法：项目启动时自动加载配置到缓存中
     * 使用 @PostConstruct 注解，Spring 在实例化 Bean 后调用此方法。
     */
    @PostConstruct
    public void init() {
        // 调用加载配置缓存的方法
        loadingConfigCache();
    }

    /**
     * 根据参数配置 ID 查询配置信息
     * @param configId 参数配置的唯一标识符
     * @return 返回对应的 SysConfig 对象
     */
    @Override
    @DataSource(DataSourceType.MASTER) // 指定使用主数据库的数据源
    public SysConfig selectConfigById(Long configId) {
        SysConfig config = new SysConfig();
        config.setConfigId(configId);
        // 通过 Mapper 查询数据库中的参数配置
        return configMapper.selectConfig(config);
    }

    /**
     * 根据配置键名查询对应的配置值
     * 1. 首先尝试从 Redis 缓存中获取配置值；
     * 2. 如果缓存中不存在，则从数据库查询并更新缓存。
     * @param configKey 参数的键名
     * @return 返回键名对应的配置值
     */
    @Override
    public String selectConfigByKey(String configKey) {
        // 从 Redis 缓存中获取配置值
        String configValue = Convert.toStr(redisCache.getCacheObject(getCacheKey(configKey)));
        if (StringUtils.isNotEmpty(configValue)) {
            return configValue; // 如果缓存中存在该值，则直接返回
        }
        // 缓存中不存在，从数据库查询
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        SysConfig retConfig = configMapper.selectConfig(config);
        if (StringUtils.isNotNull(retConfig)) {
            // 查询成功，将键值对存入 Redis 缓存
            redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
            return retConfig.getConfigValue();
        }
        // 如果数据库中也不存在，则返回空字符串
        return StringUtils.EMPTY;
    }

    /**
     * 检查验证码功能是否启用
     * 配置项的键名为 "sys.account.captchaEnabled"。
     * 如果未配置，则默认启用。
     * @return true 表示启用验证码，false 表示禁用
     */
    @Override
    public boolean selectCaptchaEnabled() {
        //从参数配置中获取验证码功能是否启用的配置项
        String captchaEnabled = selectConfigByKey("sys.account.captchaEnabled");
        // 如果配置项为空或未设置，默认返回 true（启用验证码）
        if (StringUtils.isEmpty(captchaEnabled)) {
            return true; // 如果配置项为空，默认返回 true（启用）
        }
        // 如果配置项不为空，则将配置值转换为布尔类型返回
        return Convert.toBool(captchaEnabled); // 转换为布尔值返回
    }

    /**
     * 查询参数配置列表
     * 根据传入的查询条件返回符合条件的参数列表。
     * @param config 查询条件
     * @return 返回符合条件的参数列表
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config) {
        // 调用 Mapper 查询数据库并返回结果
        return configMapper.selectConfigList(config);
    }

    /**
     * 新增参数配置
     * 数据库插入成功后，同时更新 Redis 缓存。
     * @param config 参数配置信息
     * @return 插入操作影响的行数
     */
    @Override
    public int insertConfig(SysConfig config) {
        int row = configMapper.insertConfig(config); // 插入到数据库
        if (row > 0) {
            // 插入成功后，将新配置写入缓存
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return row;
    }

    /**
     * 修改参数配置
     * 更新配置的同时，检查键名是否改变。如果键名改变，需要删除旧的缓存并更新新的缓存。
     * @param config 参数配置信息
     * @return 修改操作影响的行数
     */
    @Override
    public int updateConfig(SysConfig config) {
        // 获取数据库中的原始配置
        SysConfig temp = configMapper.selectConfigById(config.getConfigId());
        if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey())) {
            // 如果键名发生变化，删除旧的缓存
            redisCache.deleteObject(getCacheKey(temp.getConfigKey()));
        }
        // 更新数据库记录
        int row = configMapper.updateConfig(config);
        if (row > 0) {
            // 更新成功后，将新的键值对写入缓存
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return row;
    }

    /**
     * 批量删除参数配置
     * 同时删除对应的 Redis 缓存。
     * 如果某配置为内置类型（不可删除），则抛出异常。
     * @param configIds 要删除的配置 ID 数组
     */
    @Override
    public void deleteConfigByIds(Long[] configIds) {
        for (Long configId : configIds) {
            // 根据 ID 查询配置
            SysConfig config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType())) {
                // 内置配置禁止删除，抛出异常
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            // 删除数据库记录
            configMapper.deleteConfigById(configId);
            // 删除 Redis 缓存
            redisCache.deleteObject(getCacheKey(config.getConfigKey()));
        }
    }

    /**
     * 加载所有参数配置到 Redis 缓存
     */
    @Override
    public void loadingConfigCache() {
        List<SysConfig> configsList = configMapper.selectConfigList(new SysConfig());
        for (SysConfig config : configsList) {
            // 将每个配置键值对写入 Redis
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
    }

    /**
     * 清空所有 Redis 缓存
     */
    @Override
    public void clearConfigCache() {
        // 获取所有相关的缓存键
        Collection<String> keys = redisCache.keys(CacheConstants.SYS_CONFIG_KEY + "*");
        // 删除这些缓存
        redisCache.deleteObject(keys);
    }

    /**
     * 重置参数缓存
     * 先清空缓存，然后重新加载
     */
    @Override
    public void resetConfigCache() {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     * 如果存在相同键名且 ID 不一致，则认为不唯一。
     * @param config 参数配置信息
     * @return true 表示唯一，false 表示不唯一
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config) {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = configMapper.checkConfigKeyUnique(config.getConfigKey());
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 获取缓存键
     * 拼接缓存的完整键名，方便后续管理。
     * @param configKey 参数的键名
     * @return 返回缓存的完整键名
     */
    private String getCacheKey(String configKey) {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}
