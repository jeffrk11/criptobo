package com.jeff.cripto.database;


import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.config.DataBaseConnection;
import com.jeff.cripto.exceptions.DataBaseException;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.OrderParse;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class OrderRepository {
    static Logger log = Logger.getLogger(OrderRepository.class.getName());

    private ResultSet performSql(String sql){
        Connection connection = null;
        try {
            connection = DataBaseConnection.getConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(sql);
            connection.close();
            return result;
        } catch (SQLException e) {
            throw new DataBaseException(e, "Erro when peforming sql "+e.getMessage());
        }
    }

    public List<Order> getAllOrders(String botname, int limit){
        String sql = String.format("select * from orders where bot_name = '%s' order by order_id DESC limit %s", botname, limit);

        ResultSet result = performSql(sql);

        List<Order> orders = new ArrayList<>();
        try {
            while(result.next()){
                orders.add(OrderParse.parseFrom(result));
            }
        } catch (SQLException e) {
            throw new DataBaseException(e,"Error while iterating orders");
        }
        return orders;
    }

    public List<Order> getPendingOrdersByBot(String botname){
        String sql = String.format("select * from orders where bot_name = '%s' and status = 'pending' and binance_status = 'FILLED' order by created_at DESC", botname);

        ResultSet result = performSql(sql);

        List<Order> orders = new ArrayList<>();
        try {
            while(result.next()){
                orders.add(OrderParse.parseFrom(result));
            }
        } catch (SQLException e) {
            throw new DataBaseException(e,"Error while iterating orders");
        }
        return orders;
    }

    public List<Order> getLastBoughtOrdersByBot(String botname){
        String sql = String.format("select * from orders where bot_name = '%s' and order_type = 'buy' and binance_status = 'FILLED' order by created_at DESC", botname);

        ResultSet result = performSql(sql);

        List<Order> orders = new ArrayList<>();
        try {
            while(result.next()){
                orders.add(OrderParse.parseFrom(result));
            }
        } catch (SQLException e) {
            throw new DataBaseException(e,"Error while iterating orders");
        }
        return orders;
    }

    public List<Order> getOpenOrdersByBot(String botname){
        String sql = String.format("select * from orders where bot_name = '%s' and binance_status = 'NEW' order by created_at DESC", botname);

        ResultSet result = performSql(sql);

        List<Order> orders = new ArrayList<>();
        try {
            while(result.next()){
                orders.add(OrderParse.parseFrom(result));
            }
        } catch (SQLException e) {
            throw new DataBaseException(e,"Error while iterating orders");
        }
        return orders;
    }

    public List<Order> getOrdersBelowPrice(String botName, double price){
        String sql = String.format("select * from orders where bot_name = '%s' and status = 'pending' and price < %s order by created_at DESC", botName, price);

        ResultSet result = performSql(sql);

        List<Order> orders = new ArrayList<>();
        try {
            while(result.next()){
                orders.add(OrderParse.parseFrom(result));
            }
        } catch (SQLException e) {
            throw new DataBaseException(e,"Error while iterating orders");
        }
        return orders;
    }
    public Order getOrderById(int id){
        String sql = String.format("select * from orders where order_id =%s", id);

        ResultSet result = performSql(sql);
        try {
            result.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return OrderParse.parseFrom(result);
    }

    public void insertOrder(Order order){

        if (order.getOrderId() == 0)
            order.setOrderId(nextId());

        String sql =
                """ 
                INSERT INTO public.orders
                (order_id, bot_name, symbol, order_type, price, profit, status, commission, commission_asset, binance_order_id, created_at, executed_at, paid_value, quantity, binance_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try {
            Connection connection = DataBaseConnection.getConnection();
            PreparedStatement prepared = connection.prepareStatement(sql);
            OrderParse.parseTo(order, prepared);
            prepared.executeUpdate();
            connection.close();
            log.info(String.format("Inserted %s", order.toString()));

        } catch (SQLException e) {
            throw new DataBaseException(e,"Error when inserting order :"+e.getMessage() );
        }
    }

    public void updateOrder(Order order ) {
        try {
            String sqlContent = """
                    UPDATE public.orders
                    SET bot_name='%s', symbol='%s', order_type='%s', price=%s, profit=%s, status='%s', commission=%s, commission_asset='%s', binance_order_id=%s, created_at=%s, executed_at=%s, paid_value=%s, quantity=%s, binance_status='%s'
                    WHERE order_id=%s;
                    """;
            String sql = String.format(sqlContent, order.getBotName(), order.getSymbol(), order.getOrderType(), order.getPrice(), order.getProfit(), order.getStatus(), order.getCommission(), order.getCommissionAsset(), order.getBinanceOrderId(), order.getCreatedAt(), order.getExecutedAt(), order.getPaidValue(), order.getQuantity(), order.getBinanceStatus(), order.getOrderId());
            Connection connection = null;
            connection = DataBaseConnection.getConnection();
            PreparedStatement prepared = connection.prepareStatement(sql);
            prepared.executeUpdate();
            connection.close();
        } catch (SQLException e) {
            throw new DataBaseException(e, "Error while updating orders");
        }
    }

    public void createDependency(int id, int idDependency){

        String sql = """
                INSERT INTO public.dependent_order
                (id_order, id_dependent)
                VALUES(%s, %s)
                """;

        try{
            Connection connection = DataBaseConnection.getConnection();
            PreparedStatement prepared = connection.prepareStatement(String.format(sql, id, idDependency));
            prepared.executeUpdate();
            connection.close();
        }catch (Exception e){
            throw new DataBaseException(e);
        }
    }

    public List<Integer> getDependencies(int id){
        ResultSet resultSet= performSql(String.format("select id_order from dependent_order where id_dependent = %s",id));

        try {
            List<Integer> ids = new ArrayList<>();
            while (resultSet.next()){
                ids.add(resultSet.getInt("id_order"));
            }
            return ids;
        } catch (SQLException e) {
            throw new DataBaseException(e,"Error while iterating orders");
        }
    }

    public int nextId(){
        String sql = "SELECT nextval('orders_order_id_seq')";
        ResultSet result = performSql(sql);
        try {
            result.next();
            return result.getInt(1);
        } catch (SQLException e) {
            throw new DataBaseException(e);
        }
    }

    public int getCountOpenBuyOrders(){
        String sql = "select count(*) from orders where order_type ='buy' and status ='pending' and bot_name = '%s'".formatted(ConfigLoader.get("bot.name"));
        ResultSet result = performSql(sql);
        try {
            result.next();
            return  result.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public double getAllProfitByDate(LocalDateTime date){
        String sql = """
                select sum(profit) as total from orders o\s
                where order_type = 'sell'
                and bot_name = '%s'
                """.formatted(ConfigLoader.get("bot.name"));
        if(date != null)
            sql += " and TO_TIMESTAMP(executed_at / 1000)::DATE = '%s-%s-%s'".formatted(date.getYear(),date.getMonthValue(),date.getDayOfMonth());
        ResultSet result = performSql(sql);
        try {
            result.next();
            return  result.getDouble("total");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
