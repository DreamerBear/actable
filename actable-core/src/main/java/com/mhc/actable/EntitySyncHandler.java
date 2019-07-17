package com.mhc.actable;

import com.alibaba.fastjson.JSON;
import com.mhc.actable.core.manager.system.SysMysqlCreateTableManager;
import com.mhc.actable.core.manager.util.SpringContextUtil;
import com.mhc.actable.core.utils.JarUtils;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 表结构同步器
 *
 * @author xuchao
 */
public class EntitySyncHandler {

    private static final Logger log = LoggerFactory.getLogger(EntitySyncHandler.class);

    /**
     * 将Jar包中的Entities同步到spring-datasource主数据库
     * 只支持mysql
     *
     * @param jarLocalPath jar本地路径
     */
    public static void syncJarEntitiesToMainDB(String jarLocalPath) {
        try {
            Pair<ClassLoader, List<Class>> pair = JarUtils.fetchEntityClassesFromJar(jarLocalPath);
            List<Class> entities = pair.getValue();
            log.info("执行Entities同步处理方法,entities = {}", JSON.toJSONString(entities));
            SysMysqlCreateTableManager sysMysqlCreateTableManager = SpringContextUtil.getBean(SysMysqlCreateTableManager.class);
            sysMysqlCreateTableManager.syncMysqlTable(entities);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 预览Jar包中的Entities将会产生的ddl语句(已格式化)
     * 与spring-datasource主数据库作对比,只支持mysql
     *
     * @param jarLocalPath jar本地路径
     */
    public static List<String> preViewJarEntitiesDDLs(String jarLocalPath) {
        try {
            Pair<ClassLoader, List<Class>> pair = JarUtils.fetchEntityClassesFromJar(jarLocalPath);
            List<Class> entities = pair.getValue();
            log.info("执行ddls预览方法,entities = {}", JSON.toJSONString(entities));
            SysMysqlCreateTableManager sysMysqlCreateTableManager = SpringContextUtil.getBean(SysMysqlCreateTableManager.class);
            return sysMysqlCreateTableManager.generateDDLs(entities);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
