import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreData {
    public static Store parseStoreJson(String jsonFilePath) throws IOException, ParseException {
        // Initialize JSON Parser
        JSONParser parser = new JSONParser();

        // Read and parse the JSON file
        try (FileReader reader = new FileReader(jsonFilePath)) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            // Extract Store details
            String storeName = (String) jsonObject.get("StoreName");
            double latitude = ((Number) jsonObject.get("Latitude")).doubleValue();
            double longitude = ((Number) jsonObject.get("Longitude")).doubleValue();
            String foodCategory = (String) jsonObject.get("FoodCategory");
            int stars = ((Number) jsonObject.get("Stars")).intValue();
            int noOfVotes = ((Number) jsonObject.get("NoOfVotes")).intValue();
            String storeLogoPath = (String) jsonObject.get("StoreLogo");

            // Extract Product details
            JSONArray productArray = (JSONArray) jsonObject.get("Products");
            ArrayList<Product> products = new ArrayList<>();
            for (Object productObj : productArray) {
                JSONObject productJson = (JSONObject) productObj;

                String productName = (String) productJson.get("ProductName");
                String productType = (String) productJson.get("ProductType");
                int availableAmount = ((Number) productJson.get("Available Amount")).intValue();
                double price = ((Number) productJson.get("Price")).doubleValue();

                Product product = new Product(productName, productType, availableAmount, price);
                products.add(product);
            }

            // Extract Purchases (if present)
            ArrayList<Purchase> purchases = new ArrayList<>();
            if (jsonObject.containsKey("Purchases")) {
                JSONArray purchasesArray = (JSONArray) jsonObject.get("Purchases");
                for (Object purchaseObj : purchasesArray) {
                    JSONObject purchaseJson = (JSONObject) purchaseObj;
                    String customerName = (String) purchaseJson.get("CustomerName");
                    String customerEmail = (String) purchaseJson.get("CustomerEmail");
                    double totalPrice = purchaseJson.containsKey("TotalPrice") ? ((Number) purchaseJson.get("TotalPrice")).doubleValue() : 0.0;
                    ArrayList<Product> purchasedProducts = new ArrayList<>();
                    JSONArray productsArray = (JSONArray) purchaseJson.get("Products");
                    for (Object prodObj : productsArray) {
                        JSONObject prodJson = (JSONObject) prodObj;
                        String prodName = (String) prodJson.get("ProductName");
                        String prodType = (String) prodJson.get("ProductType");
                        int quantity = ((Number) prodJson.get("Quantity")).intValue();
                        double price = ((Number) prodJson.get("Price")).doubleValue();
                        purchasedProducts.add(new Product(prodName, prodType, quantity, price));
                    }
                    Purchase purchase = new Purchase(customerName, customerEmail, purchasedProducts);
                    // Overwrite totalPrice if present
                    if (totalPrice > 0) {
                        purchase.setPurchasedProducts(purchasedProducts); // recalc
                    }
                    purchases.add(purchase);
                }
            }

            // Calculate price category
            String priceCategory = calculatePriceCategory(products);

            // Create and return the Store object
            Store store = new Store(storeName, latitude, longitude, foodCategory, stars, noOfVotes, storeLogoPath, products);
            store.setPurchases(purchases);
            return store;
        }
    }

    private static String calculatePriceCategory(List<Product> products) {
        double totalPrice = 0;
        for (Product product : products) {
            totalPrice += product.getPrice();
        }
        double averagePrice = totalPrice / products.size();

        if (averagePrice <= 5) {
            return "$";
        } else if (averagePrice <= 15) {
            return "$$";
        } else {
            return "$$$";
        }
    }
}
