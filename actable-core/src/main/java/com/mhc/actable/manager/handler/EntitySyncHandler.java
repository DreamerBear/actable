package com.mhc.actable.manager.handler;

import com.mhc.actable.manager.system.SysMysqlCreateTableManager;
import com.mhc.actable.manager.util.SpringContextUtil;
import com.mhc.actable.utils.JarUtils;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 启动时进行处理的实现类
 *
 * @author chenbin.sun
 */
public class EntitySyncHandler {

    private static final Logger log = LoggerFactory.getLogger(EntitySyncHandler.class);

    /**
     * 将Jar包中的Entities同步到spring-datasource主数据库,只支持mysql
     *
     * @param jarLocalPath jar本地路径
     */
    public static void syncJarEntitiesToMainDB(String jarLocalPath) {
        try {

            Pair<ClassLoader, List<Class>> pair = JarUtils.fetchEntityClassesFromJar(jarLocalPath);
            List<Class> entities = pair.getValue();
            log.info("执行Entities同步处理方法,entities = {}", entities);
            SysMysqlCreateTableManager sysMysqlCreateTableManager = SpringContextUtil.getBean(SysMysqlCreateTableManager.class);
            sysMysqlCreateTableManager.createMysqlTable(entities);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
