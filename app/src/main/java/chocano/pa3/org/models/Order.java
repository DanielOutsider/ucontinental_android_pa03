package chocano.pa3.org.models;


import java.util.HashMap;
import java.util.Map;


public class Order {
    public String id;
    public String contactId;
    public String notes;
    public String status; // pendiente | pagado | cancelado
    public long createdAt;
    public double total; // qty * price (simple)
    public Map<String, OrderItem> items = new HashMap<>();


    public Order() {}
}