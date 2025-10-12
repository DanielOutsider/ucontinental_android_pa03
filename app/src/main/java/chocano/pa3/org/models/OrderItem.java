package chocano.pa3.org.models;


public class OrderItem {
    public String name;
    public int qty;
    public double price;


    public OrderItem() {}
    public OrderItem(String name, int qty, double price) {
        this.name = name; this.qty = qty; this.price = price;
    }
}