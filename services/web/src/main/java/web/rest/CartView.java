package web.rest;

import java.util.List;
import java.util.Map;

public class CartView {
    public String storeIcon;
    public String title = "TeaStore Cart";
    public List<Category> categoryList;
    public List<OrderItem> orderItems;
    public Map<Long, Product> products;
    public boolean isLoggedIn;
    public List<Product> advertisments;
    public List<String> productImages;
}
