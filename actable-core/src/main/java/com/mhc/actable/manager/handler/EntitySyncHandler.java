package com.mhc.actable.manager.handler;


/**
 * Entity同步执行器
 *
 * @author chenbin.sun
 */
public interface EntitySyncHandler {

    /**
     * 将Jar包中的Entities同步到spring-datasource主数据库,只支持mysql
     *
     * @param jarLocalPath jar本地路径
     */
    void syncJarEntitiesToMainDB(String jarLocalPath);
}
