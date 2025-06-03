package com.cms.system.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 *
 * @param <T> 分页数据类型
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> pageData;

    /** 总记录数 */
    private int total;

    /** 当前页码 */
    private int pageNum;

    /** 每页大小 */
    private int pageSize;

    /**
     * 构造函数
     *
     * @param pageData 当前页数据
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     */
    public PageResult(List<T> pageData, int total, int pageNum, int pageSize) {
        this.pageData = pageData;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * 获取当前页数据
     *
     * @return 当前页数据
     */
    public List<T> getPageData() {
        return pageData;
    }

    /**
     * 设置当前页数据
     *
     * @param pageData 当前页数据
     */
    public void setPageData(List<T> pageData) {
        this.pageData = pageData;
    }

    /**
     * 获取总记录数
     *
     * @return 总记录数
     */
    public int getTotal() {
        return total;
    }

    /**
     * 设置总记录数
     *
     * @param total 总记录数
     */
    public void setTotal(int total) {
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
     * 获取每页大小
     *
     * @return 每页大小
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页大小
     *
     * @param pageSize 每页大小
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
