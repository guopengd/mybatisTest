package com.ly.tcwlcrm.utils;

import com.ly.tcwlcrm.plug.IPage;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;

/**
 * 自定义Mybatis拦截器
 *
 * @author pengdong.guo
 * @date 2019/9/31
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MysqlInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger("MysqlInterceptor");

    /**
     * 获得真正的处理对象,可能多层代理.
     */
    @SuppressWarnings("unchecked")
    public static <T> T realTarget(Object target) {
        if (Proxy.isProxyClass(target.getClass())) {
            MetaObject metaObject = SystemMetaObject.forObject(target);
            return realTarget(metaObject.getValue("h.target"));
        }
        return (T) target;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 获取MappedStatement
        MappedStatement statement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        // 获取Configuration
        Configuration configuration = statement.getConfiguration();
        // 针对定义了rowBounds，做为mapper接口方法的参数
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        Object parameter = boundSql.getParameterObject();

        // 先判断是不是SELECT操作，不是的话直接执行并打印sql
        if (!SqlCommandType.SELECT.equals(statement.getSqlCommandType())) {
            // 获取sql语句
            String sql = showSql(configuration, boundSql, parameter);
            logger.info(sql);
            return invocation.proceed();
        }

        // 获取分页参数
        Object paramObj = boundSql.getParameterObject();
        IPage<?> page = getPage(paramObj);

        // 获取sql语句
        String sql = showSql(configuration, boundSql, paramObj);

        // 不需要分页的场合，如果 size 小于 0 返回结果集
        if (null == page || page.getSize() < 0) {
            logger.info(sql);
            return invocation.proceed();
        }

        // 如果需要查询总数
        if (page.isSearchCount()) {
            // 拼接查询总数的sql
            String countSql = "select count(1) " + sql.substring(sql.toUpperCase().indexOf("FROM"));
            Connection connection = (Connection) invocation.getArgs()[0];
            try (PreparedStatement countStmt = connection.prepareStatement(countSql);
                 ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    int totalCount = rs.getInt(1);
                    logger.warn(countSql);
                    page.setTotal(totalCount);
                }
            }
        }

        // 拼接分页sql
        long current = page.getCurrent(), size = page.getSize();
        sql += " LIMIT " + (current - 1) * size + " , " + size;
        metaObject.setValue("delegate.boundSql.sql", sql);
        logger.warn(sql);
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    /**
     * 查询总记录条数
     *
     * @param sql             count sql
     * @param mappedStatement MappedStatement
     * @param boundSql        BoundSql
     * @param page            IPage
     * @param connection      Connection
     */
    protected void queryTotal(boolean overflowCurrent, String sql, MappedStatement mappedStatement, BoundSql boundSql, IPage page, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            long total = 0;
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    total = resultSet.getLong(1);
                }
            }
            page.setTotal(total);
            /*
             * 溢出总页数，设置第一页
             */
            long pages = page.getPages();
            if (overflowCurrent && page.getCurrent() > pages) {
                // 设置为第一条
                page.setCurrent(1);
            }
        } catch (Exception e) {
            throw new SQLException(String.format("Error: Method queryTotal execution error of sql :  %s \n", sql));
        }
    }


    private String getParameterValue(Object obj) {
        String value;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }
        }
        return value;
    }

    /**
     * 显示参数中完整的sql
     *
     * @param configuration   configuration
     * @param boundSql        boundSql
     * @param parameterObject parameterObject
     * @return sql
     */
    private String showSql(Configuration configuration, BoundSql boundSql, Object parameterObject) {
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));

            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    }
                }
            }
        }
        return sql;
    }

    /**
     * 判断参数里是否有page对象
     *
     * @param paramObj paramObj
     * @return page
     */
    private IPage<?> getPage(Object paramObj) {
        IPage<?> page = null;
        if (paramObj instanceof IPage) {
            page = (IPage<?>) paramObj;
        } else if (paramObj instanceof Map) {
            for (Object arg : ((Map<?, ?>) paramObj).values()) {
                if (arg instanceof IPage) {
                    page = (IPage<?>) arg;
                    break;
                }
            }
        }
        return page;
    }
}
