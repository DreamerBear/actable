package com.mhc.actable.manager.handler;


/**
 * 启动时执行处理的接口
 *
 * @author chenbin.sun
 */
public interface StartUpHandler {

    /**
     * 将Jar包中的Entities同步到spring-datasource主数据库,只支持mysql
     *
     * @param jarLocalPath jar本地路径
     */
    void syncJarEntitiesToMainDB(String jarLocalPath);
}
