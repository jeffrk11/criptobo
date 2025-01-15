package com.jeff.cripto.utils.processor;

import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.annotations.Column;
import com.jeff.cripto.utils.annotations.Table;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MapperProcessor {

    private static final Map<String, Class<?>> tableMap = new HashMap<>();

    static {
        registry(Order.class);
    }

    private static void registry(Class<?> clazz){
        if(clazz.isAnnotationPresent(Table.class)){
            Table table = clazz.getAnnotation(Table.class);
            tableMap.put(table.value(), clazz);
        }

        for(Field field : clazz.getDeclaredFields()){
            if(!field.isAnnotationPresent(Column.class))
                continue;

            Column column = field.getAnnotation(Column.class);
        }
    }

    public static Class<?> getClass(String tableName){
        return tableMap.get(tableName);
    }
}
