package chocano.pa3.org.models;

public class Product {
    public String id;
    public String name;
    public int stock;
    public double price;

    public Product() {}
    public Product(String id, String name, int stock, double price) {
        this.id = id;
        this.name = name;
        this.stock = stock;
        this.price = price;
    }
}
