package com.ly.tcwlcrm.plug;


import java.io.Serializable;
import java.util.List;

/**
 * @author gpd
 * @date 2019/12/11
 */
public interface IPage<T> extends Serializable {

    /**
     * <p>
     * 自动 COUNT SQL【 默认：true 】
     * </p>
     *
     * @return true 是 / false 否
     */
    default boolean isSearchCount() {
        return true;
    }

    /**
     * <p>
     * 计算当前分页偏移量
     * </p>
     */
    default long offset() {
        return getCurrent() > 0 ? (getCurrent() - 1) * getSize() : 0;
    }

    /**
     * <p>
     * 当前分页总页数
     * </p>
     */
    default long getPages() {
        if (getSize() == 0) {
            return 0L;
        }
        long pages = getTotal() / getSize();
        if (getTotal() % getSize() != 0) {
            pages++;
        }
        return pages;
    }

    /**
     * <p>
     * 分页记录列表
     * </p>
     *
     * @return 分页对象记录列表
     */
    List<T> getRecords();

    /**
     * <p>
     * 设置分页记录列表
     * </p>
     */
    IPage<T> setRecords(List<T> records);

    /**
     * <p>
     * 当前满足条件总行数
     * </p>
     *
     * @return 总条数
     */
    long getTotal();

    /**
     * <p>
     * 设置当前满足条件总行数
     * </p>
     */
    IPage<T> setTotal(long total);

    /**
     * <p>
     * 当前分页总页数
     * </p>
     *
     * @return 总页数
     */
    long getSize();

    /**
     * <p>
     * 设置当前分页总页数
     * </p>
     */
    IPage<T> setSize(long size);

    /**
     * <p>
     * 当前页，默认 1
     * </p>
     *
     * @return 当然页
     */
    long getCurrent();

    /**
     * <p>
     * 设置当前页
     * </p>
     */
    IPage<T> setCurrent(long current);

}
