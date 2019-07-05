package com.mhc.actable.manager.system;

import com.mhc.actable.annotation.*;
import com.mhc.actable.command.CreateTableParam;
import com.mhc.actable.command.SysMysqlColumns;
import com.mhc.actable.command.TableConfigParam;
import com.mhc.actable.constants.Constants;
import com.mhc.actable.constants.MySqlTypeConstant;
import com.mhc.actable.dao.system.CreateMysqlTablesMapper;
import com.mhc.actable.utils.ClassTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;


/**
 * 项目启动时自动扫描配置的目录中的model，根据配置的规则自动创建或更新表 该逻辑只适用于mysql，其他数据库尚且需要另外扩展，因为sql的语法不同
 *
 * @author sunchenbin
 * @version 2016年6月23日 下午5:58:12
 */
@Transactional
@Service("sysMysqlCreateTableManager")
public class SysMysqlCreateTableManagerImpl implements SysMysqlCreateTableManager {

    private static final Logger log = LoggerFactory.getLogger(SysMysqlCreateTableManagerImpl.class);

    @Autowired
    private CreateMysqlTablesMapper createMysqlTablesMapper;

    // 获取Mysql的类型，以及类型需要设置几个长度
    private static Map<String, Object> mySqlTypeAndLengthMap = mySqlTypeAndLengthMap();

    /**
     * 读取配置文件的三种状态（创建表、更新表、不做任何事情）
     */
    @Override
    public void syncMysqlTable(List<Class> entities) {
        // 初始化用于存储各种操作表结构的容器
        Map<String, Map<TableConfigParam, List<Object>>> baseTableMap = initTableMap();

        // 循环全部的model
        for (Class<?> clas : entities) {

            // 没有打注解不需要创建表
            if (null == clas.getAnnotation(Table.class)) {
                continue;
            }
            // 构建出全部表的增删改的map
            buildTableMapConstruct(clas, baseTableMap);
        }

        // 根据传入的map，分别去创建或修改表结构
        createOrModifyTableConstruct(baseTableMap);
    }

    /**
     * 初始化用于存储各种操作表结构的容器
     *
     * @return 初始化map
     */
    private Map<String, Map<TableConfigParam, List<Object>>> initTableMap() {
        Map<String, Map<TableConfigParam, List<Object>>> baseTableMap = new HashMap<>();
        // 1.用于存需要创建的表名+结构
        baseTableMap.put(Constants.NEW_TABLE_MAP, new HashMap<>());
        // 2.用于存需要更新字段类型等的表名+结构
        baseTableMap.put(Constants.MODIFY_TABLE_MAP, new HashMap<>());
        // 3.用于存需要增加字段的表名+结构
        baseTableMap.put(Constants.ADD_TABLE_MAP, new HashMap<>());
        // 4.用于存需要删除字段的表名+结构
        baseTableMap.put(Constants.REMOVE_TABLE_MAP, new HashMap<>());
        // 5.用于存需要删除主键的表名+结构
        baseTableMap.put(Constants.DROPKEY_TABLE_MAP, new HashMap<>());
        // 6.用于存需要删除唯一约束的表名+结构
        baseTableMap.put(Constants.DROPINDEXANDUNIQUE_TABLE_MAP, new HashMap<>());
        // 7.用于存需要增加的索引
        baseTableMap.put(Constants.ADDINDEX_TABLE_MAP, new HashMap<>());
        // 8.用于存需要增加的唯一约束
        baseTableMap.put(Constants.ADDUNIQUE_TABLE_MAP, new HashMap<>());
        return baseTableMap;
    }

    /**
     * 构建出全部表的增删改的map
     *
     * @param clas         package中的model的Class
     * @param baseTableMap 用于存储各种操作表结构的容器
     */
    private void buildTableMapConstruct(Class<?> clas, Map<String, Map<TableConfigParam, List<Object>>> baseTableMap) {

        // 获取model的table注解
        Table table = clas.getAnnotation(Table.class);
        TableConfigParam tableConfigParam = new TableConfigParam(table);

        // 1. 用于存表的全部字段
        List<Object> allFieldList = getAllFields(mySqlTypeAndLengthMap, clas);
        if (allFieldList.size() == 0) {
            log.warn("扫描model发现" + clas.getName() + "没有建表字段请检查！");
            return;
        }

        // 先查该表是否以存在
        int exist = createMysqlTablesMapper.findTableCountByTableName(table.name());

        // 不存在时
        if (exist == 0) {
            baseTableMap.get(Constants.NEW_TABLE_MAP).put(tableConfigParam, allFieldList);
            baseTableMap.get(Constants.ADDINDEX_TABLE_MAP).put(tableConfigParam, getAddIndexList(null, allFieldList));
            baseTableMap.get(Constants.ADDUNIQUE_TABLE_MAP).put(tableConfigParam, getAddUniqueList(null, allFieldList));
            return;
        }

        // 已存在时理论上做修改的操作，这里查出该表的结构
        List<SysMysqlColumns> tableColumnList = createMysqlTablesMapper
                .findTableEnsembleByTableName(table.name());

        // 从sysColumns中取出我们需要比较的列的List
        // 先取出name用来筛选出增加和删除的字段
        List<String> columnNames = ClassTools.getPropertyValueList(tableColumnList,
                SysMysqlColumns.COLUMN_NAME_KEY);

        // 验证对比从model中解析的allFieldList与从数据库查出来的columnList
        // 2. 找出增加的字段
        List<Object> addFieldList = getAddFieldList(table, allFieldList, columnNames);

        // 3. 找出删除的字段
        List<Object> removeFieldList = getRemoveFieldList(table, columnNames, allFieldList);

        // 4. 找出更新的字段
        List<Object> modifyFieldList = getModifyFieldList(table, columnNames, tableColumnList, allFieldList);

        // 5. 找出需要删除主键的字段
        List<Object> dropKeyFieldList = getDropKeyFieldList(table, columnNames, tableColumnList, allFieldList);

        // 查询当前表中全部的索引和唯一约束
        Set<String> allIndexAndUniqueNames = createMysqlTablesMapper.findTableIndexByTableName(table.name());

        // 6. 找出需要删除的索引和唯一约束
        List<Object> dropIndexAndUniqueFieldList = getDropIndexAndUniqueList(allIndexAndUniqueNames, allFieldList);

        // 7. 找出需要新增的索引
        List<Object> addIndexFieldList = getAddIndexList(allIndexAndUniqueNames, allFieldList);

        // 8. 找出需要新增的唯一约束
        List<Object> addUniqueFieldList = getAddUniqueList(allIndexAndUniqueNames, allFieldList);

        if (addFieldList.size() != 0) {
            baseTableMap.get(Constants.ADD_TABLE_MAP).put(tableConfigParam, addFieldList);
        }
        if (removeFieldList.size() != 0) {
            baseTableMap.get(Constants.REMOVE_TABLE_MAP).put(tableConfigParam, removeFieldList);
        }
        if (modifyFieldList.size() != 0) {
            baseTableMap.get(Constants.MODIFY_TABLE_MAP).put(tableConfigParam, modifyFieldList);
        }
        if (dropKeyFieldList.size() != 0) {
            baseTableMap.get(Constants.DROPKEY_TABLE_MAP).put(tableConfigParam, dropKeyFieldList);
        }
        if (dropIndexAndUniqueFieldList.size() != 0) {
            baseTableMap.get(Constants.DROPINDEXANDUNIQUE_TABLE_MAP).put(tableConfigParam, dropIndexAndUniqueFieldList);
        }
        if (addIndexFieldList.size() != 0) {
            baseTableMap.get(Constants.ADDINDEX_TABLE_MAP).put(tableConfigParam, addIndexFieldList);
        }
        if (addUniqueFieldList.size() != 0) {
            baseTableMap.get(Constants.ADDUNIQUE_TABLE_MAP).put(tableConfigParam, addUniqueFieldList);
        }
    }

    /**
     * 找出需要新建的索引
     *
     * @param allIndexAndUniqueNames 当前数据库的索引很约束名
     * @param allFieldList           model中的所有字段
     * @return 需要新建的索引
     */
    private List<Object> getAddIndexList(Set<String> allIndexAndUniqueNames, List<Object> allFieldList) {
        List<Object> addIndexFieldList = new ArrayList<Object>();
        if (null == allIndexAndUniqueNames) {
            allIndexAndUniqueNames = new HashSet<String>();
        }
        for (Object obj : allFieldList) {
            CreateTableParam createTableParam = (CreateTableParam) obj;
            if (null != createTableParam.getFiledIndexName() && !allIndexAndUniqueNames.contains(createTableParam.getFiledIndexName())) {
                addIndexFieldList.add(createTableParam);
            }
        }
        return addIndexFieldList;
    }

    /**
     * 找出需要新建的唯一约束
     *
     * @param allIndexAndUniqueNames 当前数据库的索引很约束名
     * @param allFieldList           model中的所有字段
     * @return 需要新建的唯一约束
     */
    private List<Object> getAddUniqueList(Set<String> allIndexAndUniqueNames, List<Object> allFieldList) {
        List<Object> addUniqueFieldList = new ArrayList<Object>();
        if (null == allIndexAndUniqueNames) {
            allIndexAndUniqueNames = new HashSet<String>();
        }
        for (Object obj : allFieldList) {
            CreateTableParam createTableParam = (CreateTableParam) obj;
            if (null != createTableParam.getFiledUniqueName() && !allIndexAndUniqueNames.contains(createTableParam.getFiledUniqueName())) {
                addUniqueFieldList.add(createTableParam);
            }
        }
        return addUniqueFieldList;
    }

    /**
     * 找出需要删除的索引和唯一约束
     *
     * @param allIndexAndUniqueNames 当前数据库的索引很约束名
     * @param allFieldList           model中的所有字段
     * @return 需要删除的索引和唯一约束
     */
    private List<Object> getDropIndexAndUniqueList(Set<String> allIndexAndUniqueNames, List<Object> allFieldList) {
        List<Object> dropIndexAndUniqueFieldList = new ArrayList<Object>();
        if (null == allIndexAndUniqueNames || allIndexAndUniqueNames.size() == 0) {
            return dropIndexAndUniqueFieldList;
        }
        List<String> currentModelIndexAndUnique = new ArrayList<String>();
        for (Object obj : allFieldList) {
            CreateTableParam createTableParam = (CreateTableParam) obj;
            if (null != createTableParam.getFiledIndexName()) {
                currentModelIndexAndUnique.add(createTableParam.getFiledIndexName());
            }
            if (null != createTableParam.getFiledUniqueName()) {
                currentModelIndexAndUnique.add(createTableParam.getFiledUniqueName());
            }
        }
        for (String string : allIndexAndUniqueNames) {
            if (!currentModelIndexAndUnique.contains(string)) {
                dropIndexAndUniqueFieldList.add(string);
            }
        }
        return dropIndexAndUniqueFieldList;
    }

    /**
     * 返回需要删除主键的字段
     *
     * @param table           表
     * @param columnNames     数据库中的结构
     * @param tableColumnList 表结构
     * @param allFieldList    model中的所有字段
     * @return 需要删除主键的字段
     */
    private List<Object> getDropKeyFieldList(Table table, List<String> columnNames,
                                             List<SysMysqlColumns> tableColumnList, List<Object> allFieldList) {
        Map<String, CreateTableParam> fieldMap = getAllFieldMap(allFieldList);
        List<Object> dropKeyFieldList = new ArrayList<Object>();
        for (SysMysqlColumns sysColumn : tableColumnList) {
            // 数据库中有该字段时
            CreateTableParam createTableParam = fieldMap.get(sysColumn.getColumn_name());
            if (createTableParam != null) {
                // 原本是主键，现在不是了，那么要去做删除主键的操作
                if ("PRI".equals(sysColumn.getColumn_key()) && !createTableParam.isFieldIsKey()) {
                    dropKeyFieldList.add(createTableParam);
                }

            }
        }
        return dropKeyFieldList;
    }

    /**
     * 根据数据库中表的结构和model中表的结构对比找出修改类型默认值等属性的字段
     *
     * @param table           表
     * @param columnNames     数据库中的结构
     * @param tableColumnList 表结构
     * @param allFieldList    model中的所有字段
     * @return 需要修改的字段
     */
    private List<Object> getModifyFieldList(Table table, List<String> columnNames, List<SysMysqlColumns> tableColumnList,
                                            List<Object> allFieldList) {
        Map<String, CreateTableParam> fieldMap = getAllFieldMap(allFieldList);
        List<Object> modifyFieldList = new ArrayList<Object>();
        for (SysMysqlColumns sysColumn : tableColumnList) {
            // 数据库中有该字段时
            CreateTableParam createTableParam = fieldMap.get(sysColumn.getColumn_name());
            if (createTableParam != null) {
                // 验证是否有更新
                // 1.验证类型
                if (!sysColumn.getData_type().toLowerCase().equals(createTableParam.getFieldType().toLowerCase())) {
                    modifyFieldList.add(createTableParam);
                    continue;
                }
                // 2.验证长度
                // 3.验证小数点位数
                int length = (Integer) mySqlTypeAndLengthMap.get(createTableParam.getFieldType().toLowerCase());
                String typeAndLength = createTableParam.getFieldType().toLowerCase();
                if (length == 1) {
                    // 拼接出类型加长度，比如varchar(1)
                    typeAndLength = typeAndLength + "(" + createTableParam.getFieldLength() + ")";
                } else if (length == 2) {
                    // 拼接出类型加长度，比如varchar(1)
                    typeAndLength = typeAndLength + "(" + createTableParam.getFieldLength() + ","
                            + createTableParam.getFieldDecimalLength() + ")";
                }
                // 判断类型+长度是否相同
                if (!sysColumn.getColumn_type().toLowerCase().equals(typeAndLength)) {
                    modifyFieldList.add(createTableParam);
                    continue;
                }

                // 4.验证主键
                if (!"PRI".equals(sysColumn.getColumn_key()) && createTableParam.isFieldIsKey()) {
                    // 原本不是主键，现在变成了主键，那么要去做更新
                    modifyFieldList.add(createTableParam);
                    continue;
                }

                // 5.验证自增
                if ("auto_increment".equals(sysColumn.getExtra()) && !createTableParam.isFieldIsAutoIncrement()) {
                    modifyFieldList.add(createTableParam);
                    continue;
                }

                // 6.验证默认值
                if (sysColumn.getColumn_default() == null || sysColumn.getColumn_default().equals("")) {
                    // 数据库默认值是null，model中注解设置的默认值不为NULL时，那么需要更新该字段
                    if (!"NULL".equals(createTableParam.getFieldDefaultValue())) {
                        modifyFieldList.add(createTableParam);
                        continue;
                    }
                } else if (!sysColumn.getColumn_default().equals(createTableParam.getFieldDefaultValue())) {
                    // 两者不相等时，需要更新该字段
                    modifyFieldList.add(createTableParam);
                    continue;
                }

                // 7.验证是否可以为null(主键不参与是否为null的更新)
                if (sysColumn.getIs_nullable().equals("NO") && !createTableParam.isFieldIsKey()) {
                    if (createTableParam.isFieldIsNull()) {
                        // 一个是可以一个是不可用，所以需要更新该字段
                        modifyFieldList.add(createTableParam);
                        continue;
                    }
                } else if (sysColumn.getIs_nullable().equals("YES") && !createTableParam.isFieldIsKey()) {
                    if (!createTableParam.isFieldIsNull()) {
                        // 一个是可以一个是不可用，所以需要更新该字段
                        modifyFieldList.add(createTableParam);
                        continue;
                    }
                }

                // 8.验证注释
                if (!createTableParam.getComment().equals(sysColumn.getColumn_comment())) {
                    // 两者不相等时，需要更新该字段
                    modifyFieldList.add(createTableParam);
                    continue;
                }

            }
        }
        return modifyFieldList;
    }

    /**
     * 将allFieldList转换为Map结构
     *
     * @param allFieldList
     * @return
     */
    private Map<String, CreateTableParam> getAllFieldMap(List<Object> allFieldList) {
        // 将fieldList转成Map类型，字段名作为主键
        Map<String, CreateTableParam> fieldMap = new HashMap<String, CreateTableParam>();
        for (Object obj : allFieldList) {
            CreateTableParam createTableParam = (CreateTableParam) obj;
            fieldMap.put(createTableParam.getFieldName(), createTableParam);
        }
        return fieldMap;
    }

    /**
     * 根据数据库中表的结构和model中表的结构对比找出删除的字段
     *
     * @param table        表
     * @param columnNames  数据库中的结构
     * @param allFieldList model中的所有字段
     */
    private List<Object> getRemoveFieldList(Table table, List<String> columnNames, List<Object> allFieldList) {
        Map<String, CreateTableParam> fieldMap = getAllFieldMap(allFieldList);
        // 用于存删除的字段
        List<Object> removeFieldList = new ArrayList<Object>();
        for (String fieldNm : columnNames) {
            // 判断该字段在新的model结构中是否存在
            if (fieldMap.get(fieldNm) == null) {
                // 不存在，做删除处理
                removeFieldList.add(fieldNm);
            }
        }
        return removeFieldList;
    }

    /**
     * 根据数据库中表的结构和model中表的结构对比找出新增的字段
     *
     * @param table        表
     * @param allFieldList model中的所有字段
     * @param columnNames  数据库中的结构
     * @return 新增的字段
     */
    private List<Object> getAddFieldList(Table table, List<Object> allFieldList, List<String> columnNames) {
        List<Object> addFieldList = new ArrayList<Object>();
        for (Object obj : allFieldList) {
            CreateTableParam createTableParam = (CreateTableParam) obj;
            // 循环新的model中的字段，判断是否在数据库中已经存在
            if (!columnNames.contains(createTableParam.getFieldName())) {
                // 不存在，表示要在数据库中增加该字段
                addFieldList.add(obj);
            }
        }
        return addFieldList;
    }

    /**
     * 迭代出所有model的所有fields
     *
     * @param mySqlTypeAndLengthMap mysql数据类型和对应几个长度的map
     * @param clas                  准备做为创建表依据的class
     * @return 表的全部字段
     */
    private List<Object> getAllFields(Map<String, Object> mySqlTypeAndLengthMap, Class<?> clas) {
        List<Object> fieldList = new ArrayList<Object>();
        Field[] fields = clas.getDeclaredFields();

        // 判断是否有父类，如果有拉取父类的field，这里只支持多层继承
        fields = recursionParents(clas, fields);

        for (Field field : fields) {
            // 判断方法中是否有指定注解类型的注解
            boolean hasAnnotation = field.isAnnotationPresent(Column.class);
            if (hasAnnotation) {
                // 根据注解类型返回方法的指定类型注解
                Column column = field.getAnnotation(Column.class);
                CreateTableParam param = new CreateTableParam();
                param.setFieldName(column.name());
                param.setFieldType(column.type().toLowerCase());
                param.setFieldLength(column.length());
                param.setFieldDecimalLength(column.decimalLength());
                param.setComment(column.comment());
                // 主键时设置必须不为null
                if (column.isKey()) {
                    param.setFieldIsNull(false);
                } else {
                    param.setFieldIsNull(column.isNull());
                }
                param.setFieldIsKey(column.isKey());
                param.setFieldIsAutoIncrement(column.isAutoIncrement());
                param.setFieldDefaultValue(column.defaultValue());
                int length = (Integer) mySqlTypeAndLengthMap.get(column.type().toLowerCase());
                param.setFileTypeLength(length);
                // 获取当前字段的@Index注解
                Index index = field.getAnnotation(Index.class);
                if (null != index) {
                    param.setFiledIndexName((index.name() == null || index.name().equals("")) ? (Constants.IDX + column.name()) : index.name());
                    param.setFiledIndexValue(index.value().length == 0 ? Arrays.asList(column.name()) : Arrays.asList(index.value()));
                }
                // 获取当前字段的@Unique注解
                Unique unique = field.getAnnotation(Unique.class);
                if (null != unique) {
                    param.setFiledUniqueName((unique.name() == null || unique.name().equals("")) ? (Constants.UNI + column.name()) : unique.name());
                    param.setFiledUniqueValue(unique.value().length == 0 ? Arrays.asList(column.name()) : Arrays.asList(unique.value()));
                }
                fieldList.add(param);
            }
        }
        return fieldList;
    }

    /**
     * 递归扫描父类的fields
     *
     * @param clas   类
     * @param fields 属性
     */
    @SuppressWarnings("rawtypes")
    private Field[] recursionParents(Class<?> clas, Field[] fields) {
        if (clas.getSuperclass() != null) {
            Class clsSup = clas.getSuperclass();
            List<Field> fieldList = new ArrayList<Field>();
            fieldList.addAll(Arrays.asList(fields));
            fieldList.addAll(Arrays.asList(clsSup.getDeclaredFields()));
            fields = new Field[fieldList.size()];
            int i = 0;
            for (Object field : fieldList.toArray()) {
                fields[i] = (Field) field;
                i++;
            }
            fields = recursionParents(clsSup, fields);
        }
        return fields;
    }

    /**
     * 根据传入的map创建或修改表结构
     *
     * @param baseTableMap 操作sql的数据结构
     */
    private void createOrModifyTableConstruct(Map<String, Map<TableConfigParam, List<Object>>> baseTableMap) {
        // 1. 创建表
        createTableByMap(baseTableMap.get(Constants.NEW_TABLE_MAP));
        // 2. 删除要变更主键的表的原来的字段的主键
        dropFieldsKeyByMap(baseTableMap.get(Constants.DROPKEY_TABLE_MAP));
        // 3. 添加新的字段
        addFieldsByMap(baseTableMap.get(Constants.ADD_TABLE_MAP));
        // 4. 删除字段
        removeFieldsByMap(baseTableMap.get(Constants.REMOVE_TABLE_MAP));
        // 5. 修改字段类型等
        modifyFieldsByMap(baseTableMap.get(Constants.MODIFY_TABLE_MAP));
        // 6. 删除索引和约束
        dropIndexAndUniqueByMap(baseTableMap.get(Constants.DROPINDEXANDUNIQUE_TABLE_MAP));
        // 7. 创建索引
        addIndexByMap(baseTableMap.get(Constants.ADDINDEX_TABLE_MAP));
        // 8. 创建约束
        addUniqueByMap(baseTableMap.get(Constants.ADDUNIQUE_TABLE_MAP));

    }

    /**
     * 根据map结构删除索引和唯一约束
     *
     * @param dropIndexAndUniqueMap 用于删除索引和唯一约束
     */
    private void dropIndexAndUniqueByMap(Map<TableConfigParam, List<Object>> dropIndexAndUniqueMap) {
        if (dropIndexAndUniqueMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : dropIndexAndUniqueMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableConfigParam, Object> map = new HashMap<TableConfigParam, Object>();
                    map.put(entry.getKey(), obj);
                    log.info("开始删除表" + entry.getKey() + "中的索引" + obj);
                    createMysqlTablesMapper.dorpTabelIndex(map);
                    log.info("完成删除表" + entry.getKey() + "中的索引" + obj);
                }
            }
        }
    }

    /**
     * 根据map结构创建索引
     *
     * @param addIndexMap 用于创建索引和唯一约束
     */
    private void addIndexByMap(Map<TableConfigParam, List<Object>> addIndexMap) {
        if (addIndexMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : addIndexMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableConfigParam, Object> map = new HashMap<TableConfigParam, Object>();
                    map.put(entry.getKey(), obj);
                    CreateTableParam fieldProperties = (CreateTableParam) obj;
                    if (null != fieldProperties.getFiledIndexName()) {
                        log.info("开始创建表" + entry.getKey() + "中的索引" + fieldProperties.getFiledIndexName());
                        createMysqlTablesMapper.addTableIndex(map);
                        log.info("完成创建表" + entry.getKey() + "中的索引" + fieldProperties.getFiledIndexName());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构创建唯一约束
     *
     * @param addUniqueMap 用于创建索引和唯一约束
     */
    private void addUniqueByMap(Map<TableConfigParam, List<Object>> addUniqueMap) {
        if (addUniqueMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : addUniqueMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableConfigParam, Object> map = new HashMap<TableConfigParam, Object>();
                    map.put(entry.getKey(), obj);
                    CreateTableParam fieldProperties = (CreateTableParam) obj;
                    if (null != fieldProperties.getFiledUniqueName()) {
                        log.info("开始创建表" + entry.getKey() + "中的唯一约束" + fieldProperties.getFiledUniqueName());
                        createMysqlTablesMapper.addTableUnique(map);
                        log.info("完成创建表" + entry.getKey() + "中的唯一约束" + fieldProperties.getFiledUniqueName());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构修改表中的字段类型等
     *
     * @param modifyTableMap 用于存需要更新字段类型等的表名+结构
     */
    private void modifyFieldsByMap(Map<TableConfigParam, List<Object>> modifyTableMap) {
        // 做修改字段操作
        if (modifyTableMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : modifyTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableConfigParam, Object> map = new HashMap<TableConfigParam, Object>();
                    map.put(entry.getKey(), obj);
                    CreateTableParam fieldProperties = (CreateTableParam) obj;
                    log.info("开始修改表" + entry.getKey() + "中的字段" + fieldProperties.getFieldName());
                    createMysqlTablesMapper.modifyTableField(map);
                    log.info("完成修改表" + entry.getKey() + "中的字段" + fieldProperties.getFieldName());
                }
            }
        }
    }

    /**
     * 根据map结构删除表中的字段
     *
     * @param removeTableMap 用于存需要删除字段的表名+结构
     */
    private void removeFieldsByMap(Map<TableConfigParam, List<Object>> removeTableMap) {
        // 做删除字段操作
        if (removeTableMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : removeTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableConfigParam, Object> map = new HashMap<TableConfigParam, Object>();
                    map.put(entry.getKey(), obj);
                    String fieldName = (String) obj;
                    log.info("开始删除表" + entry.getKey() + "中的字段" + fieldName);
                    createMysqlTablesMapper.removeTableField(map);
                    log.info("完成删除表" + entry.getKey() + "中的字段" + fieldName);
                }
            }
        }
    }

    /**
     * 根据map结构对表中添加新的字段
     *
     * @param addTableMap 用于存需要增加字段的表名+结构
     */
    private void addFieldsByMap(Map<TableConfigParam, List<Object>> addTableMap) {
        // 做增加字段操作
        if (addTableMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : addTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableConfigParam, Object> map = new HashMap<TableConfigParam, Object>();
                    map.put(entry.getKey(), obj);
                    CreateTableParam fieldProperties = (CreateTableParam) obj;
                    log.info("开始为表" + entry.getKey() + "增加字段" + fieldProperties.getFieldName());
                    createMysqlTablesMapper.addTableField(map);
                    log.info("完成为表" + entry.getKey() + "增加字段" + fieldProperties.getFieldName());
                }
            }
        }
    }

    /**
     * 根据map结构删除要变更表中字段的主键
     *
     * @param dropKeyTableMap 用于存需要删除主键的表名+结构
     */
    private void dropFieldsKeyByMap(Map<TableConfigParam, List<Object>> dropKeyTableMap) {
        // 先去做删除主键的操作，这步操作必须在增加和修改字段之前！
        if (dropKeyTableMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : dropKeyTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableConfigParam, Object> map = new HashMap<TableConfigParam, Object>();
                    map.put(entry.getKey(), obj);
                    CreateTableParam fieldProperties = (CreateTableParam) obj;
                    log.info("开始为表" + entry.getKey() + "删除主键" + fieldProperties.getFieldName());
                    createMysqlTablesMapper.dropKeyTableField(map);
                    log.info("完成为表" + entry.getKey() + "删除主键" + fieldProperties.getFieldName());
                }
            }
        }
    }

    /**
     * 根据map结构创建表
     *
     * @param newTableMap 用于存需要创建的表名+结构
     */
    private void createTableByMap(Map<TableConfigParam, List<Object>> newTableMap) {
        // 做创建表操作
        if (newTableMap.size() > 0) {
            for (Entry<TableConfigParam, List<Object>> entry : newTableMap.entrySet()) {
                Map<TableConfigParam, List<Object>> map = new HashMap<TableConfigParam, List<Object>>();
                map.put(entry.getKey(), entry.getValue());
                log.info("开始创建表：" + entry.getKey());
                createMysqlTablesMapper.createTable(map);
                log.info("完成创建表：" + entry.getKey());
            }
        }
    }

    /**
     * 获取Mysql的类型，以及类型需要设置几个长度，这里构建成map的样式
     * 构建Map(字段名(小写),需要设置几个长度(0表示不需要设置，1表示需要设置一个，2表示需要设置两个))
     */
    public static Map<String, Object> mySqlTypeAndLengthMap() {
        Field[] fields = MySqlTypeConstant.class.getDeclaredFields();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : fields) {
            LengthCount lengthCount = field.getAnnotation(LengthCount.class);
            map.put(field.getName().toLowerCase(), lengthCount.LengthCount());
        }
        return map;
    }
}
