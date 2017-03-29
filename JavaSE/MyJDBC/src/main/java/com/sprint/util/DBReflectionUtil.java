package com.sprint.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author x_zhaohu		17/03/28
 */
public class DBReflectionUtil {

	private static <T> boolean isEnabled(String tableName, T object) {
		if (tableName.trim().equals("") || tableName == null || object == null)
			return false;
		return true;
	}

	private static <T> boolean isEnabled(String tableName, T object, Map<String, Object> map, String operator1) {
		if (!isEnabled(tableName, object))
			return false;
		if (map.size() == 0 || operator1.trim().equals("") || operator1 == null)
			return false;
		return true;
	}

	private static <T> boolean isEnabled(String tableName, T object, Map<String, Object> map, String operator1, String operator2) {
		if (!isEnabled(tableName, object, map, operator1))
			return false;
		if (operator2.trim().equals("") || operator2 == null)
			return false;
		return true;
	}
	/**
	 * 通过key获取单条记录, key值要求唯一, 例如：身份证号，唯一的
	 * @param tableName 
	 * @param object 实体类,仅实例化
	 * @param map 数据库中字段与值
	 * @param operator1 "=" or "like"
	 * @return 
	 */
	public static <T> T findOnlyByKey(String tableName, T object, Map<String, Object> map, String operator1) {
		if (!isEnabled(tableName, object, map, operator1))
			throw new IllegalArgumentException("参数不合法"); 
		List<T> list = findMoreByKey(tableName, object, map, operator1);
		if (list.size() != 0) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * 通过key获取多条记录，key值可重复，例如：性别
	 * @param tableName
	 * @param object 实体类，仅实例化
	 * @param map 数据库中 字段 与 值
	 * @param operator1 "=" or "like"
	 * @return 
	 */
	public static <T> List<T> findMoreByKey(String tableName, T object, Map<String, Object> map, String operator1) {
		if (!isEnabled(tableName, object, map, operator1))
			throw new IllegalArgumentException("参数不合法"); 
		return findMoreByKeys(tableName, object, map, operator1, null); 
	}


	public static <T> T findOnlyByKeys(String tableName, T object,
						Map<String, Object> map, String operator1, String operator2) {
		if (!isEnabled(tableName, object, map, operator1))
			throw new IllegalArgumentException("参数不合法"); 
		List<T> list = findMoreByKeys(tableName, object, map, operator1, operator2);
		if (list.size() != 0) {
			return list.get(0);
		}
		return null;
		
	}
	/**
	 * 通过keys获取多条记录, key值可重复，但key唯一
	 * @param tableName
	 * @param object 实体类，仅实例化
	 * @param map 数据库中字段与值
	 * @param operator1 "=" or "like"
	 * @param operator2 "and" or "or"
	 * @return 
	 */
	public static <T> List<T> findMoreByKeys(String tableName, T object, Map<String, Object> map, String operator1, String operator2) {
		if (!isEnabled(tableName, object, map, operator1))
			throw new IllegalArgumentException("参数不合法"); 
		Set<String> keys = map.keySet();
		StringBuffer sb = new StringBuffer(tableName).append(" where ");
		int index = 0;
		for (String key : keys) {
				sb.append(key).append(" " + operator1 + " ");
				if (key.getClass() == String.class) {
					sb.append("\'")
					  .append(map.get(key))
					  .append("\'");
				}
				if (index != (keys.size() - 1)) {
					sb.append(" " + operator2 + " ");
				}
				index++;
		}
		return findAll(sb.toString(), object);
	}

	/**
	 * 通过表名和对象获取所有的数据
	 * @param tableName 表名
	 * @param object 实体类, 仅实例化 
	 * @return 
	 */
	public static <T> List<T> findAll(String tableName, T object) {
		List<T> list = new ArrayList<T>();
		Map<String, Class> paramsType = getParamsType(object);
		Set<String> keys = paramsType.keySet();
		StringBuilder sb = new StringBuilder("select * from ").append(tableName);
		Connection conn = ConnectionUtil.getConnection();
		PreparedStatement pstmt;
		String sql = sb.toString();
		int capacity = paramsType.size();
		try {
			pstmt = conn.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) { 
				Object obj = object.getClass().newInstance();
				for (String key : keys) {
					if (paramsType.get(key) == int.class) {
						Method m = getMethod(object.getClass(), "set", key);
						m.invoke(obj, rs.getInt(key));
					} else if (paramsType.get(key) == java.lang.String.class) {
						Method m = getMethod(object.getClass(), "set", key);
						m.invoke(obj, rs.getString(key));
					} else if (paramsType.get(key) == double.class) {
						Method m = getMethod(object.getClass(), "set", key);
						m.invoke(obj, rs.getDouble(key));
					} else if (paramsType.get(key) == float.class) {
						Method m = getMethod(object.getClass(), "set", key);
						m.invoke(obj, rs.getFloat(key));
					} else if (paramsType.get(key) == long.class) {
						Method m = getMethod(object.getClass(), "set", key);
						m.invoke(obj, rs.getLong(key));
					} else if (paramsType.get(key) == boolean.class) {
						Method m = getMethod(object.getClass(), "set", key);
						m.invoke(obj, rs.getBoolean(key));
					}
				}	
				list.add(((T)obj));
			}
			pstmt.close();
			conn.close();
			return list;
			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 通过表名和对象进行插入操作
	 * @param tableName 表名 
	 * @param object 实例化并带有数据的对象
	 * @return
	 */
	public static <T> boolean insert(String tableName, T object) {
		Map<String, Class> paramsType = getParamsType(object);
		Set<String> keys = paramsType.keySet();
		List<String> params = new ArrayList<String>();
		
		StringBuffer sb = new StringBuffer("insert into " + tableName);
		sb.append("(");
	
		int count = 0;
		int capacity = keys.size();
		
		boolean isExistAutoId = false;
		for (String key : keys) {
			if (key.equals("id")) {
				count++;
				capacity--;
				isExistAutoId = true;
				continue;
			}
			sb.append(key);
			params.add(key);
			if (!isExistAutoId) {
				if (count != (capacity - 1))
					sb.append(",");
			} else {
				if (count != capacity)
					sb.append(",");
			}
			count++;
		}

		sb.append(")values(");
		for (int i = 0; i < capacity; i++) {
			sb.append("?");
			if (i == (capacity -1)) {
				sb.append(")");
			} else {
				sb.append(",");
			}
		}
		
		Connection conn = ConnectionUtil.getConnection();
		String sql = sb.toString();
		System.out.println(sql);
		PreparedStatement pstmt;
		
		try {
			pstmt = conn.prepareStatement(sql);
			// int , String , double ,float ,Long,boolean
			for (int i = 1; i <= capacity; i++) {
				String key = params.get(i-1);
				if (paramsType.get(key) == int.class) {
					Method m = getMethod(object.getClass(), "get", key);
					pstmt.setInt(i, (Integer) m.invoke(object));
				} else if (paramsType.get(key) == java.lang.String.class) {
					Method m = getMethod(object.getClass(), "get", key);
					pstmt.setString(i, (String) m.invoke(object));
				} else if (paramsType.get(key) == double.class) {
					Method m = getMethod(object.getClass(), "get", key);
					pstmt.setDouble(i, (Double) m.invoke(object));
				} else if (paramsType.get(key) == float.class) {
					Method m = getMethod(object.getClass(), "get", key);
					pstmt.setFloat(i, (Float)m.invoke(object));
				} else if (paramsType.get(key) == long.class) {
					Method m = getMethod(object.getClass(), "get", key);
					pstmt.setLong(i, (Long)m.invoke(object));
				} else if (paramsType.get(key) == boolean.class) {
					Method m = getMethod(object.getClass(), "get", key);
					pstmt.setBoolean(i, (Boolean)m.invoke(object));
				}
			}
			pstmt.executeUpdate();
			pstmt.close();
			conn.close();
			return true;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/** 
	 * 通过对象获取该类的信息
	 * @param object
	 * @return
	 */
	private static <T> Map<String, Class> getParamsType(T object) {
		Class<?> clz = object.getClass();
		Field[] fields = clz.getDeclaredFields();
		Map<String, Class> paramsType = new HashMap<String,  Class>(); 
		for (Field field : fields) {
			paramsType.put(field.getName(), field.getType());
		}
		return paramsType;
	} 
	
	/**
	 * 通过class, 方法名前缀和字段获取该方法
	 * @param clz
	 * @param startWithName
	 * @param param
	 * @return
	 */
	private static Method getMethod(Class clz, String startWithName, String param) {
		List<Method> methods = getMethods(clz, startWithName);
		String name = new StringBuilder()
						.append(Character.toUpperCase(param.charAt(0)))
						.append(param.substring(1))
						.toString();
		for (Method m : methods) {
			if (m.getName().equals(startWithName + name)) {
				return m;
			}
		}
		return null;
	}
	
	/**
	 * 通过class,前缀获取方法列表
	 * @param clz
	 * @param startWithName
	 * @return
	 */
	private static List<Method> getMethods(Class clz, String startWithName) {
		List<Method> methods = new ArrayList<Method>();
		for (Method m : clz.getDeclaredMethods()) {
			if (m.getName().startsWith(startWithName)) {
				methods.add(m);
			}
		}
		return methods;
	}
}
