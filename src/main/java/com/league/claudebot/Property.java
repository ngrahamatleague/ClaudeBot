package com.league.claudebot;

public class Property {

    private String id;
    private String url;
    private String homeType;
    private String status;
    private double price;
    private int beds;
    private double baths;
    private double area;
    private int daysOnZillow;
    private String street;
    private String city;
    private String state;
    private String zipcode;

    public Property(String id, String url, String homeType, String status,
                    double price, int beds, double baths, double area,
                    int daysOnZillow, String street, String city,
                    String state, String zipcode) {
        this.id = id;
        this.url = url;
        this.homeType = homeType;
        this.status = status;
        this.price = price;
        this.beds = beds;
        this.baths = baths;
        this.area = area;
        this.daysOnZillow = daysOnZillow;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
    }

    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getHomeType() { return homeType; }
    public String getStatus() { return status; }
    public double getPrice() { return price; }
    public int getBeds() { return beds; }
    public double getBaths() { return baths; }
    public double getArea() { return area; }
    public int getDaysOnZillow() { return daysOnZillow; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipcode() { return zipcode; }

    public String getFullAddress() {
        return street + ", " + city + ", " + state + " " + zipcode;
    }

    public String getFormattedPrice() {
        if (price <= 0) return "N/A";
        return String.format("$%,.0f", price);
    }
}
