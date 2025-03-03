package com.jeff.cripto.database;

import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.model.DependentOrder;
import com.jeff.cripto.model.Order;
import com.jeff.cripto.utils.MapperDataBaseUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OrderRepository extends Repository{
    static Logger log = Logger.getLogger(OrderRepository.class.getName());

    public List<Order> getAllOrders(int limit){
        String sql = String.format("select * from orders where bot_name = '%s' order by order_id DESC limit %s", ConfigLoader.get("bot.name"), limit);

        List<Map<String, Object>> rows = query(sql);

        return MapperDataBaseUtil.mapFromRows(rows, Order.class);
    }

    public List<Order> getPendingOrders(){
        String sql = String.format("select * from orders where bot_name = '%s' and status = 'pending' and binance_status = 'FILLED' order by created_at DESC", ConfigLoader.get("bot.name"));

        List<Map<String, Object>> rows = query(sql);

        return MapperDataBaseUtil.mapFromRows(rows, Order.class);
    }

    public Order getLastPendingOrder(){
        List<Order> orders = getPendingOrders();
        if(orders.isEmpty())
            return null;

        return orders.get(0);
    }

    public Order getLastBoughtOrder(){
        String sql = String.format("select * from orders where bot_name = '%s' and order_type = 'buy' and binance_status = 'FILLED' order by created_at DESC LIMIT 1", ConfigLoader.get("bot.name"));

        List<Map<String, Object>> rows = query(sql);
        if(rows.isEmpty())
            return null;

        return MapperDataBaseUtil.mapFromRows(rows, Order.class).get(0);
    }

    public Order getLastSoldOrder(){
        String sql = String.format("select * from orders where bot_name = '%s' and order_type = 'sell' and binance_status = 'FILLED' order by created_at DESC LIMIT 1", ConfigLoader.get("bot.name"));

        List<Map<String, Object>> rows = query(sql);
        if(rows.isEmpty())
            return null;

        return MapperDataBaseUtil.mapFromRows(rows, Order.class).get(0);
    }

    public Order getLastOperationFilledOrder(){
        String sql = String.format("select * from orders where bot_name = '%s' and binance_status = 'FILLED' order by executed_at DESC LIMIT 1", ConfigLoader.get("bot.name"));

        List<Map<String, Object>> rows = query(sql);


        return MapperDataBaseUtil.mapFromRows(rows, Order.class).get(0);
    }

    public List<Order> getOpenOrders(){
        String sql = String.format("select * from orders where bot_name = '%s' and binance_status = 'NEW' order by created_at DESC", ConfigLoader.get("bot.name"));

        List<Map<String, Object>> rows = query(sql);
        if(rows.isEmpty())
            return null;

        return MapperDataBaseUtil.mapFromRows(rows, Order.class);
    }

    public List<Order> getOrdersBelowPrice(double price){
        String sql = String.format("select * from orders where bot_name = '%s' and status = 'pending' and price < %s order by created_at DESC", ConfigLoader.get("bot.name"), price);

        List<Map<String, Object>> rows = query(sql);

        return MapperDataBaseUtil.mapFromRows(rows, Order.class);
    }

    public Order getOrderById(long id){
        String sql = String.format("select * from orders where order_id =%s", id);
        List<Map<String, Object>> rows = query(sql);

        return MapperDataBaseUtil.mapFromRows(rows, Order.class).get(0);
    }

    public void insertOrder(Order order){

        if (order.getOrderId() == 0)
            order.setOrderId(nextId());

        insert(order);
    }

    public void updateOrder(Order order ) {
        update(order);
    }

    public void createDependency(long id, long idDependency){
        insert(new DependentOrder(id,idDependency));
    }

    public List<Integer> getDependencies(long id){
        return query("select id_order from dependent_order where id_dependent = %s".formatted(id))
                .stream().map( m -> (Integer)  m.get("id_order")).toList();
    }

    public long nextId(){
        String sql = "SELECT nextval('orders_order_id_seq')";
        return (long) query(sql).get(0).get("nextval");
    }

    public long getCountOpenBuyOrders(){
        String sql = """
                select count(*) from orders 
                where order_type ='buy' 
                    AND status ='pending' 
                    AND bot_name = '%s' 
                    AND created_at > (EXTRACT(EPOCH FROM NOW() - INTERVAL '1 hours') * 1000)
                """.formatted(ConfigLoader.get("bot.name"));
        long count = (long) query(sql).get(0).get("count");
        return  count == 0 ? 1 : count;
    }

    public BigDecimal getAllProfitByDate(LocalDateTime date){
        String sql = """
                select sum(profit) as total from orders o\s
                where order_type = 'sell'
                and bot_name = '%s'
                """.formatted(ConfigLoader.get("bot.name"));
        if(date != null)
            sql += " and TO_TIMESTAMP(executed_at / 1000)::DATE = '%s-%s-%s'".formatted(date.getYear(),date.getMonthValue(),date.getDayOfMonth());

        return (BigDecimal) query(sql).get(0).get("total");
    }


}
