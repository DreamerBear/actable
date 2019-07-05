package com.mhc.actable.core.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


/**
 * 通过包名获取class
 *
 * @author sunchenbin
 * @version 2016年6月23日 下午5:55:18 
 */
public class ClassTools{

	/**
	 * 取出list对象中的某个属性的值作为list返回
	 * @param objList
	 * @param fieldName
	 * @return
	 */
	public static <T, E> List<E> getPropertyValueList(List<T> objList, String fieldName){
		List<E> list = new ArrayList<E>();
		try{
			for (T object : objList){
				Field field = object.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				list.add((E) field.get(object));
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		
		return list;
	}
}
