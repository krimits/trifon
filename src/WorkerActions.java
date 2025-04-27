import java.awt.print.Book;
import java.io.*;
import java.net.*;
import java.util.*;

public class WorkerActions extends Thread {
    ObjectInputStream in;
    ObjectOutputStream out;
    private final ArrayList<Store> stores;
    private final Object lock;
    private final Socket connection;

    public WorkerActions(Socket connection, ArrayList<Store> stores, Object lock) {
        this.connection = connection;
        this.stores = stores;
        this.lock = lock;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Χρησιμοποίησε Haversine για πραγματική απόσταση σε km
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // ακτίνα γης σε km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public void run() {
        try {
            String role = (String) in.readObject();

            if (role.equals("manager")) {
                // Receive from master
                Store s = (Store) in.readObject();

                stores.add(s);

                // Send to master
                out.writeObject("Store(s) added successfully");
                out.flush();

            } else if (role.equals("findStore")) {
                // Receive from master
                String storeName = (String) in.readObject();

                boolean storeFound = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            storeFound = true;
                            break; // exit after store is found
                        }
                    }
                }

                if (!storeFound) {
                    storeName = null;
                }

                // Send to master
                out.writeObject(storeName);
                out.flush();

            } else if (role.equals("findProduct")) {
                // Receive from master
                String storeName = (String) in.readObject();
                String ProductName = (String) in.readObject();

                boolean productFound = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            for (Product pro : store.getProducts()) {
                                if (pro.getName().equalsIgnoreCase(ProductName)) {
                                    productFound = true;
                                    break; // exit after product is found
                                }
                            }
                        }
                    }
                }

                // Send to master
                if (productFound) {
                    out.writeObject("exists");
                } else {
                    out.writeObject("doesnt exist");
                }
                out.flush();

            } else if (role.equals("findProduct2")) {
                // Receive from master
                String storeName = (String) in.readObject();
                String ProductName = (String) in.readObject();

                boolean productFound = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            for (Product pro : store.getProducts()) {
                                if (pro.getName().equalsIgnoreCase(ProductName)) {
                                    productFound = true;
                                    // Send to master
                                    if (pro.getQuantity() == -1) {
                                        out.writeObject("hidden");
                                        out.flush();
                                    } else {
                                        out.writeObject(ProductName);
                                        out.flush();
                                    }
                                    break; // exit after product is found
                                }
                            }
                        }
                    }
                }

                if (!productFound) {
                    ProductName = null;
                    out.writeObject(ProductName);
                    out.flush();
                }

            } else if (role.equals("AmountInc")) {
                // Receive from master
                String storeName = (String) in.readObject();
                String ProductName = (String) in.readObject();
                int amount = (int) in.readInt();

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            for (Product pro : store.getProducts()) {
                                if (pro.getName().equalsIgnoreCase(ProductName)) {
                                    pro.setQuantity(amount + pro.getQuantity());
                                    break; // exit after quantity is changed
                                }
                            }
                        }
                    }
                }

                // Send to master
                out.writeObject("Amount changed successfully");
                out.flush();

            } else if (role.equals("NewProduct")) {
                // Receive from master
                String storeName = (String) in.readObject();
                Product pro = (Product) in.readObject();

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            store.getProducts().add(pro);
                            System.out.println(store.getProducts());
                            break; // exit after product is added

                        }
                    }
                }

                // Send to master
                out.writeObject("Product added successfully");
                out.flush();

            } else if (role.equals("remove")) {
                // Receive from master
                String storeName = (String) in.readObject();
                String pro = (String) in.readObject();

                boolean prodFound = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            prodFound = true;
                            for (Product prod : store.getProducts()) {
                                if (prod.getName().equalsIgnoreCase(pro)) {
                                    prod.setQuantity(-1);
                                    prod.setStatus("hidden");
                                    break; // exit after quantity is changed
                                }
                            }

                        }
                    }
                }

                // Send to master
                if (prodFound) {
                    out.writeObject("Product removed or updated successfully.");
                } else {
                    out.writeObject("Product not found.");
                }
                out.flush();

            } else if (role.equals("AmountDec")) {
                // Receive from master
                String storeName = (String) in.readObject();
                String ProductName = (String) in.readObject();
                int amount = (int) in.readInt();

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            for (Product pro : store.getProducts()) {
                                if (pro.getName().equalsIgnoreCase(ProductName)) {
                                    if ((pro.getQuantity() - amount) >= 0) {
                                        pro.setQuantity(pro.getQuantity() - amount);
                                        out.writeObject("Amount changed successfully");
                                        out.flush();
                                    } else {
                                        out.writeObject("Amount is greater than the quantity");
                                        out.flush();
                                    }
                                }
                            }
                        }
                    }
                }

            } else if (role.equals("storeType")) {
                // Receive from master
                String requestedType = (String) in.readObject(); // e.g., "pizzeria"

                Map<String, Integer> result = new HashMap<>();

                synchronized (lock) {
                    int totalSold = 0;
                    for (Store store : stores) {
                        if (store.getCategory().equalsIgnoreCase(requestedType)) {
                            for (Purchase purchase : store.getPurchases()) {
                                for (Product p : purchase.getPurchasedProducts()) {
                                    totalSold += p.getQuantity(); // Sum all quantities
                                }
                            }
                            result.put(store.getStoreName(), totalSold);
                        }
                    }
                }

                // Send to master
                out.writeObject(result);
                out.flush();

            } else if (role.equals("productCategory")) {
                // Receive from master
                String requestedCategory = (String) in.readObject(); // e.g., "pizza"

                Map<String, Integer> result = new HashMap<>();

                synchronized (lock) {
                    for (Store store : stores) {
                        int totalCategorySales = 0;
                        for (Purchase purchase : store.getPurchases()) {
                            for (Product product : purchase.getPurchasedProducts()) {
                                if (product.getCategory().equalsIgnoreCase(requestedCategory)) {
                                    totalCategorySales += product.getQuantity();
                                }
                            }
                        }

                        if (totalCategorySales > 0) {
                            result.put(store.getStoreName(), totalCategorySales);
                        }
                    }
                }

                // Send to master
                out.writeObject(result);
                out.flush();

            } else if (role.equals("client") || role.equals("filter")) {
                // Receive from master
                MapReduceRequest request = (MapReduceRequest) in.readObject();
                String requestId = request.getRequestId();

                double userLat = request.getClientLatitude();
                double userLon = request.getClientLongitude();
                double radius = request.getRadius();

                ArrayList<String> categories = (ArrayList<String>) request.getFoodCategories();
                double minStars = request.getMinStars();
                String price = request.getPriceCategory();

                ArrayList<Store> result = new ArrayList<>();

                synchronized (lock) {
                    for (Store store : stores) {
                        double distance = haversine(userLat, userLon, store.getLatitude(), store.getLongitude());
                        boolean matchesDistance = distance <= radius;
                        boolean matchesCategory = categories.isEmpty() || categories.contains(store.getCategory());
                        boolean matchesStars = minStars == 0 || store.getStars() >= minStars;
                        boolean matchesPrice = price.isEmpty() || store.calculatePriceCategory().equalsIgnoreCase(price);

                        if (matchesDistance && matchesCategory && matchesStars && matchesPrice) {
                            result.add(store);
                        }
                    }
                }

                // Send to master
                out.writeObject(result);
                out.flush();
                // Send ack
                out.writeObject("ack:" + requestId);
                out.flush();

            } else if (role.equals("fetchProducts")) {
                // Receive from master
                String storeName = (String) in.readObject();

                ArrayList<Product> available = new ArrayList<>();

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            for (Product product : store.getProducts()) {
                                if (product.getStatus().equalsIgnoreCase("visible")) {
                                    available.add(product);
                                }
                            }
                            break;
                        }
                    }
                }

                // Send to master
                out.writeObject(available);
                out.flush();

            } else if (role.equals("purchase")) {
                // Receive from master
                Purchase purchase = (Purchase) in.readObject();
                String storeName = (String) in.readObject();

                ArrayList<Product> requestedProducts = purchase.getPurchasedProducts();

                String message = "";

                synchronized (lock) {
                    Store targetStore = null;
                    for (Store s : stores) { // find the object store
                        if (s.getStoreName().equalsIgnoreCase(storeName)) {
                            targetStore = s;
                            break;
                        }
                    }

                    if (targetStore != null) { // if the store is found we store the products
                        boolean allValid = true;

                        for (Product req : requestedProducts) {
                            // Πιο ανεκτικό matching: βρες το προϊόν με όνομα που περιέχει το input (case-insensitive)
                            Product prod = null;
                            for (Product p : targetStore.getProducts()) {
                                if (p.getName().equalsIgnoreCase(req.getName()) ||
                                    p.getName().toLowerCase().contains(req.getName().toLowerCase())) {
                                    prod = p;
                                    break;
                                }
                            }

                            if (prod == null) {
                                message = "Product not found: " + req.getName();
                                allValid = false;
                                break;
                            }

                            if (!prod.getStatus().equalsIgnoreCase("visible")) {
                                message = "Product not available: " + req.getName();
                                allValid = false;
                                break;
                            }

                            if (prod.getQuantity() < req.getQuantity()) {
                                message = "Not enough quantity for: " + req.getName();
                                allValid = false;
                                break;
                            }

                            prod.setQuantity(prod.getQuantity() - req.getQuantity());

                            // Fill up the empty fields
                            req.setCategory(prod.getCategory());
                            req.setPrice(prod.getPrice());
                        }

                        if (allValid) {
                            targetStore.getPurchases().add(purchase);
                            message = "Purchase successful at " + targetStore.getStoreName();
                            if (requestedProducts.isEmpty()) {
                                message = "The purchase requested is empty";
                            }
                        }
                    }
                }

                // Send to master
                out.writeObject(message);
                out.flush();

            } else if (role.equals("rate")) {
                // Receive from master
                String storeName = (String) in.readObject();
                int rating = (int) in.readObject();

                boolean storeFound = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            double oldStars = store.getStars(); // current average rating
                            int oldReviews = store.getNoOfReviews(); // total reviews so far

                            int newReviews = oldReviews + 1;
                            double newAvg = (oldStars * oldReviews + rating) / newReviews;

                            // Update store fields
                            store.setStars(newAvg);
                            store.setNoOfReviews(newReviews);

                            storeFound = true;
                            break;
                        }
                    }
                }

                // Send to master
                if (storeFound) {
                    out.writeObject("Rating submitted successfully.");
                } else {
                    out.writeObject("Store not found.");
                }
                out.flush();
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
