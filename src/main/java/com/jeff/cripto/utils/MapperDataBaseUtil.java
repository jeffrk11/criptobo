package com.jeff.cripto.utils;

import com.jeff.cripto.exceptions.DataBaseException;
import com.jeff.cripto.utils.annotations.Column;
import com.jeff.cripto.utils.annotations.PrimaryKey;
import com.jeff.cripto.utils.annotations.Table;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperDataBaseUtil {

    public static <T> String createSqlInsert(T entity){
        if(!entity.getClass().isAnnotationPresent(Table.class))
            throw new DataBaseException("Error trying to parse %s no annotation".formatted(entity.getClass().getName()));

        StringBuilder fields = new StringBuilder();
        StringBuilder fieldsCount = new StringBuilder();
        for(Field field : entity.getClass().getDeclaredFields()) {
            if (Boolean.FALSE.equals(field.isAnnotationPresent(Column.class)))
                continue;
            Column column = field.getAnnotation(Column.class);

            fields.append("%s,".formatted(column.value()));
            fieldsCount.append("?,");
        }
       return " INSERT INTO %s (%s) VALUES (%s)"
                .formatted(entity.getClass().getAnnotation(Table.class).value(),
                        fields.substring(0,fields.length()-1),
                        fieldsCount.substring(0,fieldsCount.length()-1));
    }

//UPDATE public.orders
//SET bot_name='%s', symbol='%s', order_type='%s', price=%s, profit=%s, status='%s', commission=%s, commission_asset='%s',
// binance_order_id=%s, created_at=%s, executed_at=%s, paid_value=%s, quantity=%s, binance_status='%s'
//WHERE order_id=%s;
    public static <T> String createSqlUpdate(T entity){
        if(!entity.getClass().isAnnotationPresent(Table.class))
            throw new DataBaseException("Error trying to parse %s no annotation".formatted(entity.getClass().getName()));

        StringBuilder fields = new StringBuilder();
        String pk = null;
        for(Field field : entity.getClass().getDeclaredFields()) {
            if (Boolean.FALSE.equals(field.isAnnotationPresent(Column.class)))
                continue;
            if (field.isAnnotationPresent(PrimaryKey.class)){
                pk = field.getAnnotation(Column.class).value();
                continue;
            }

            Column column = field.getAnnotation(Column.class);

            fields.append("%s=?,".formatted(column.value()));

        }
       return " UPDATE %s SET %s WHERE %s=?"
                .formatted(entity.getClass().getAnnotation(Table.class).value(),
                        fields.substring(0,fields.length()-1), pk);
    }

    public static Map<String, Object> mapRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> rowMap = new HashMap<>();
        ResultSetMetaData metaData = resultSet.getMetaData();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            Object columnValue = resultSet.getObject(i);
            rowMap.put(columnName, columnValue);
        }

        return rowMap;
    }

    public static <T> List<T> mapFromRows(List<Map<String, Object>> rows, Class<T> entity)  {
        if(!entity.isAnnotationPresent(Table.class))
            throw new DataBaseException("Error trying to parse %s no annotation".formatted(entity.getName()));

        Table table = entity.getAnnotation(Table.class);

        List<T> result = new ArrayList<>();
        try{
            for(Map<String, Object> row : rows){
                //order_id - 123 ..
                T obj = entity.getDeclaredConstructor().newInstance();
                for(Field field : entity.getDeclaredFields()){
                    if(Boolean.FALSE.equals(field.isAnnotationPresent(Column.class)))
                        continue;
                    Column column = field.getAnnotation(Column.class);

                    if(Boolean.FALSE.equals(row.containsKey(column.value())))
                        continue;

                    String nameMethod = "set" + field.getName().substring(0,1).toUpperCase() + field.getName().substring(1);

                    obj.getClass().getMethod(nameMethod, field.getType()).invoke(obj, row.get(column.value()));
                }
                result.add(obj);
            }
        } catch (Exception e) {
            throw new DataBaseException(e);
        }
        return result;
    }

}
