package com.mhc.actable.manager.handler;

import com.mhc.actable.manager.system.SysMysqlCreateTableManager;
import com.mhc.actable.utils.JarUtil;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 启动时进行处理的实现类
 *
 * @author chenbin.sun
 */
@SuppressWarnings("restriction")
@Service
public class StartUpHandlerImpl implements StartUpHandler {

    private static final Logger log = LoggerFactory.getLogger(StartUpHandlerImpl.class);

    @Autowired
    private SysMysqlCreateTableManager sysMysqlCreateTableManager;

    @Override
    public void syncJarEntitiesToMainDB(String jarLocalPath) {
        try {
            Pair<ClassLoader, List<Class>> pair = JarUtil.fetchEntityClassesFromJar(jarLocalPath);
            List<Class> entities = pair.getValue();
            log.info("执行Entities同步处理方法,entities = {}", entities);
            sysMysqlCreateTableManager.createMysqlTable(entities);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
