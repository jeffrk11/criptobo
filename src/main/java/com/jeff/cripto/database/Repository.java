package com.jeff.cripto.database;

import com.jeff.cripto.config.DataBaseConnection;
import com.jeff.cripto.exceptions.DataBaseException;
import com.jeff.cripto.utils.annotations.Column;
import com.jeff.cripto.utils.MapperDataBaseUtil;
import com.jeff.cripto.utils.annotations.PrimaryKey;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Repository {


    public List<Map<String, Object>> query(String sql){
        try (Connection connection = DataBaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result =  statement.executeQuery(sql);){

            List<Map<String, Object>> rows = new ArrayList<>();

            while (result.next()){
                rows.add(MapperDataBaseUtil.mapRow(result));
            }

            return rows;
        } catch (SQLException e) {
            throw new DataBaseException(e, "Erro when peforming sql "+e.getMessage());
        }
    }

    public <T> void  insert(T entity){
        String sql = MapperDataBaseUtil.createSqlInsert(entity);
        try(Connection connection = DataBaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)){

            int i = 1;
            for(Field field : entity.getClass().getDeclaredFields()){
                if (Boolean.FALSE.equals(field.isAnnotationPresent(Column.class)))
                    continue;

                String nameMethod = "get" + field.getName().substring(0,1).toUpperCase() + field.getName().substring(1);
                statement.setObject(i++, entity.getClass().getMethod(nameMethod).invoke(entity));
            }
            statement.execute();

        } catch (SQLException e) {
            throw new DataBaseException(e, "Erro when inserting/updating sql "+e.getMessage());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void  update(T entity){
        String sql = MapperDataBaseUtil.createSqlUpdate(entity);
        try(Connection connection = DataBaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)){

            int i = 1;
            long pk = 0;
            for(Field field : entity.getClass().getDeclaredFields()){
                if (Boolean.FALSE.equals(field.isAnnotationPresent(Column.class)))
                    continue;
                String nameMethod = "get" + field.getName().substring(0,1).toUpperCase() + field.getName().substring(1);

                if (field.isAnnotationPresent(PrimaryKey.class)){
                    pk = (long) entity.getClass().getMethod(nameMethod).invoke(entity);
                    continue;
                }

                statement.setObject(i++, entity.getClass().getMethod(nameMethod).invoke(entity));
            }
            statement.setObject(i, pk);
            statement.execute();

        } catch (SQLException e) {
            throw new DataBaseException(e, "Erro when inserting/updating sql "+e.getMessage());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }



}
