package com.cms.system.service.impl;

import java.util.List;

import com.cms.common.annotation.DataScope;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.utils.DateUtils;
import com.cms.common.utils.uuid.IdGenerator;
import com.cms.common.core.domain.entity.SysComp;
import com.cms.system.service.ISysCompService;
import com.cms.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cms.system.mapper.SysRegistrMapper;
import com.cms.common.core.domain.entity.SysRegistr;
import com.cms.system.service.ISysRegistrService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报名信息Service业务层处理
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Service
public class SysRegistrServiceImpl implements ISysRegistrService 
{
    @Autowired
    private SysRegistrMapper sysRegistrMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysCompService sysCompService;

    // 定义日志记录器
    Logger logger = LoggerFactory.getLogger(SysRegistrServiceImpl.class);

    /**
     * 查询报名信息
     * 
     * @param registrId 报名信息主键
     * @return 报名信息
     */
    @Override
    public SysRegistr selectSysRegistrByRegistrId(Long registrId)
    {
        return sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
    }

    /**
     * 查询报名信息列表
     * 
     * @param sysRegistr 报名信息
     * @return 报名信息
     */
    @Override
    @DataScope(userAlias = "u")
    public List<SysRegistr> selectSysRegistrList(SysRegistr sysRegistr) {
        logger.info("查询报名信息列表, sysRegistr: {}", sysRegistr);
        List<SysRegistr> registrList = sysRegistrMapper.selectSysRegistrList(sysRegistr);

        // 调用竞赛业务层，根据竞赛编码查询竞赛信息，更新竞赛名称
        for (SysRegistr registr : registrList) {
            SysComp sysComp = sysCompService.selectSysCompByCompId(registr.getCompId());
            if (sysComp != null) {
                registr.setCompName(sysComp.getCompName());
            }
        }

        return registrList;
    }


    /**
     * 新增报名信息
     * 
     * @param sysRegistr 报名信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertSysRegistr(SysRegistr sysRegistr) {
        logger.info("新增报名信息, sysRegistr: {}", sysRegistr);
        try {
            // 获得唯一id,设置到对象中
            sysRegistr.setRegistrId(IdGenerator.generateId(0));

            // 根据参数的竞赛编码，获得竞赛名称
            SysComp sysComp = validateAndGetSysComp(sysRegistr.getCompId());
            sysRegistr.setCompName(sysComp.getCompName());

            // 根据参数的用户编码，获得用户名称和创建者
            SysUser sysUser = validateAndGetSysUser(sysRegistr.getUserId());

            sysRegistr.setCreateBy(sysUser.getUserName());
            sysRegistr.setUserName(sysUser.getUserName());//设置用户名称
            sysRegistr.setCreateTime(DateUtils.getNowDate());

            //评分频率默认为0
            sysRegistr.setScoreCount(0L); // 初始化评分频率为0

            // 插入报名信息，并返回受影响的行数
            int rows = sysRegistrMapper.insertSysRegistr(sysRegistr);

            // 如果报名成功，竞赛的访问频率+1
            if (rows > 0 && sysComp != null) {
                updateCompAccessFrequency(sysComp);
            }

            logger.info("新增报名信息结果: {}", rows);
            return rows;
        } catch (Exception e) {
            // 记录异常日志
            logger.error("插入报名信息失败", e);
            throw new RuntimeException("插入报名信息失败", e); // 抛出自定义异常
        }
    }

    private SysComp validateAndGetSysComp(Long compId) {
        logger.info("验证并获取竞赛信息, compId: {}", compId);
        SysComp sysComp = sysCompService.selectSysCompByCompId(compId);
        if (sysComp == null) {
            throw new IllegalArgumentException("竞赛编码无效");
        }
        return sysComp;
    }

    private SysUser validateAndGetSysUser(Long userId) {
        logger.info("验证并获取用户信息, userId: {}", userId);
        SysUser sysUser = sysUserService.selectUserById(userId);
        if (sysUser == null) {
            throw new IllegalArgumentException("用户编码无效");
        }
        return sysUser;
    }

    private void updateCompAccessFrequency(SysComp sysComp) {
        logger.info("更新竞赛访问频率, sysComp: {}", sysComp);
        if (sysComp == null) {
            logger.error("竞赛对象为空，无法更新访问频率");
            throw new IllegalArgumentException("竞赛对象为空");
        }

        if (sysComp.getAccessFrequency() == null) {
            logger.warn("竞赛访问频率字段为空，初始化为0");
            sysComp.setAccessFrequency(0);
        }

        sysComp.setAccessFrequency(sysComp.getAccessFrequency() + 1);
        sysCompService.updateSysComp(sysComp);
    }

    /**
     * 修改报名信息
     * 
     * @param sysRegistr 报名信息
     * @return 结果
     */
    @Override
    public int updateSysRegistr(SysRegistr sysRegistr)
    {
        logger.info("修改报名信息, sysRegistr: {}", sysRegistr);
        try {
            sysRegistr.setUpdateTime(DateUtils.getNowDate());
            int result = sysRegistrMapper.updateSysRegistr(sysRegistr);
            logger.info("修改报名信息结果: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("修改报名信息失败, sysRegistr: {}", sysRegistr, e);
            throw new RuntimeException("修改报名信息失败", e);
        }
    }

    /**
     * 批量删除报名信息
     * 
     * @param registrIds 需要删除的报名信息主键
     * @return 结果
     */
    @Override
    public int deleteSysRegistrByRegistrIds(Long[] registrIds)
    {
        logger.info("批量删除报名信息, registrIds: {}", registrIds);
        try {
            int result = sysRegistrMapper.deleteSysRegistrByRegistrIds(registrIds);
            logger.info("批量删除报名信息结果: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("批量删除报名信息失败, registrIds: {}", registrIds, e);
            throw new RuntimeException("批量删除报名信息失败", e);
        }
    }

    /**
     * 删除报名信息信息
     * 
     * @param registrId 报名信息主键
     * @return 结果
     */
    @Override
    public int deleteSysRegistrByRegistrId(Long registrId)
    {
        logger.info("删除报名信息信息, registrId: {}", registrId);
        try {
            int result = sysRegistrMapper.deleteSysRegistrByRegistrId(registrId);
            logger.info("删除报名信息信息结果: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("删除报名信息信息失败, registrId: {}", registrId, e);
            throw new RuntimeException("删除报名信息信息失败", e);
        }
    }

    /**
     * 根据用户ID和竞赛ID查询参赛者信息
     * 
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 参赛者信息
     */
    @Override
    public SysRegistr selectSysRegistrByUserIdAndCompId(Long userId, Long compId) {
        logger.info("根据用户ID和竞赛ID查询参赛者信息, userId: {}, compId: {}", userId, compId);
        return sysRegistrMapper.selectSysRegistrByUserIdAndCompId(userId, compId);
    }

}
