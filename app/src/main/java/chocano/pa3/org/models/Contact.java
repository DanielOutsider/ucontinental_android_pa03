package chocano.pa3.org.models;

public class Contact {
    public String id;
    public String name;
    public String phone;

    // Constructor vacío requerido por Firebase
    public Contact() {}

    public Contact(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }
}
