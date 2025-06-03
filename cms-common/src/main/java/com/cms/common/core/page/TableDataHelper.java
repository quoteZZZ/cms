package com.cms.common.core.page;

import java.io.Serializable;
import java.util.List;

/**
 * 表格数据处理辅助类
 * 用于缓存分页查询结果和分页信息
 *
 * @author cms
 */
public class TableDataHelper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页的数据列表 */
    private List<T> rows;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int pageNum;

    /** 每页显示记录数 */
    private int pageSize;

    /**
     * 构造函数
     */
    public TableDataHelper() {
    }

    /**
     * 构造函数
     *
     * @param rows 当前页的数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页显示记录数
     */
    public TableDataHelper(List<T> rows, long total, int pageNum, int pageSize) {
        this.rows = rows;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * 构造函数 - 从TableDataInfo创建
     *
     * @param dataInfo 表格分页数据对象
     */
    @SuppressWarnings("unchecked")
    public TableDataHelper(TableDataInfo dataInfo) {
        this.rows = (List<T>) dataInfo.getRows();
        this.total = dataInfo.getTotal();

        // 从请求上下文或其他地方获取当前页码和每页大小
        com.github.pagehelper.Page<Object> page = com.github.pagehelper.PageHelper.getLocalPage();
        if (page != null) {
            this.pageNum = page.getPageNum();
            this.pageSize = page.getPageSize();
        } else {
            this.pageNum = 1;
            this.pageSize = 10; // 默认值
        }
    }

    /**
     * 获取当前页的数据列表
     *
     * @return 数据列表
     */
    public List<T> getRows() {
        return rows;
    }

    /**
     * 设置当前页的数据列表
     *
     * @param rows 数据列表
     */
    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    /**
     * 获取总记录数
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置总记录数
     *
     * @param total 总记录数
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 获取当前页码
     *
     * @return 当前页码
     */
    public int getPageNum() {
        return pageNum;
    }

    /**
     * 设置当前页码
     *
     * @param pageNum 当前页码
     */
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 获取每页显示记录数
     *
     * @return 每页显示记录数
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页显示记录数
     *
     * @param pageSize 每页显示记录数
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 将分页结果转换为TableDataInfo对象
     *
     * @return TableDataInfo对象
     */
    public TableDataInfo toTableDataInfo() {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setMsg("查询成功");
        rspData.setRows(this.rows);
        rspData.setTotal(this.total);
        return rspData;
    }
}
