package com.mhc.actable.core.dao.system;

import com.mhc.actable.core.command.SysMysqlColumns;
import com.mhc.actable.core.command.TableConfigParam;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 创建更新表结构的Mapper
 * @author sunchenbin
 *
 */
public interface CreateMysqlTablesMapper {

	/**
	 * 根据结构注解解析出来的信息创建表
	 * @param tableMap 表结构的map
	 */
	public void createTable(@Param("tableMap") Map<TableConfigParam, List<Object>> tableMap);

	/**
	 * 根据表名查询表在库中是否存在，存在返回1，不存在返回0
	 * @param tableName 表结构的map
	 * @return 存在返回1，不存在返回0
	 */
	public int findTableCountByTableName(@Param("tableName") String tableName);
	
	/**
	 * 根据表名查询库中该表的字段结构等信息
	 * @param tableName 表结构的map
	 * @return 表的字段结构等信息
	 */
	public List<SysMysqlColumns> findTableEnsembleByTableName(@Param("tableName") String tableName);
	
	/**
	 * 增加字段
	 * @param tableMap 表结构的map
	 */
	public void addTableField(@Param("tableMap") Map<TableConfigParam, Object> tableMap);
	
	/**
	 * 删除字段
	 * @param tableMap 表结构的map
	 */
	public void removeTableField(@Param("tableMap") Map<TableConfigParam, Object> tableMap);
	
	/**
	 * 修改字段
	 * @param tableMap 表结构的map
	 */
	public void modifyTableField(@Param("tableMap") Map<TableConfigParam, Object> tableMap);
	
	/**
	 * 删除主键约束，附带修改其他字段属性功能
	 * @param tableMap 表结构的map
	 */
	public void dropKeyTableField(@Param("tableMap") Map<TableConfigParam, Object> tableMap);
	
	/**
	 * 根据表名删除表
	 * @param tableName 表名
	 */
	public void dorpTableByName(@Param("tableName") String tableName);
	
	/**
	 * 查询当前表存在的索引(除了主键索引primary)
	 * @param tableName 表名
	 * @return 索引名列表
	 */
	public Set<String> findTableIndexByTableName(@Param("tableName") String tableName);
	
	/**
	 * 删除表索引
	 * @param tableMap
	 */
	public void dorpTabelIndex(@Param("tableMap") Map<TableConfigParam, Object> tableMap);
	
	/**
	 * 创建索引
	 * @param tableMap
	 */
	public void addTableIndex(@Param("tableMap") Map<TableConfigParam, Object> tableMap);
	
	/**
	 * 创建唯一约束
	 * @param tableMap
	 */
	public void addTableUnique(@Param("tableMap") Map<TableConfigParam, Object> tableMap);
	
}
