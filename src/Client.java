import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.UUID;

public class Client {
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        while (true) {
            // Display menu options
            System.out.println("1. Stores near you");
            System.out.println("2. Filtering stores");
            System.out.println("3. Purchase products");
            System.out.println("4. Rate store");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            String option = sc.nextLine();

            if (option.equals("1")) {
                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    // Send to master
                    out.writeObject("client");
                    out.flush();

                    System.out.print("Enter your latitude: ");
                    double lat = Double.parseDouble(sc.nextLine());

                    System.out.print("Enter your longitude: ");
                    double lon = Double.parseDouble(sc.nextLine());

                    // Create MapReduceRequest with default filters
                    String requestId = UUID.randomUUID().toString();
                    MapReduceRequest request = new MapReduceRequest(
                            lat,
                            lon,
                            new ArrayList<>(), // No category filter
                            0,                // No minimum stars
                            "",               // No price filter
                            5.0,              // 5km radius
                            requestId
                    );

                    out.writeObject(request);
                    out.flush();

                    System.out.println("Searching for stores nearby...");

                    // Receive from master
                    ArrayList<Store> results = (ArrayList<Store>) in.readObject();

                    if (results.isEmpty()) {
                        System.out.println("No nearby stores found within 5 km.");
                    } else {
                        System.out.println("\nNearby Stores:");
                        for (Store store : results) {
                            System.out.println(store);
                            System.out.println("-----------\n");
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error connecting to server: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

            } else if (option.equals("2")) {
                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    // Send to master
                    out.writeObject("filter");
                    out.flush();

                    System.out.print("Enter your latitude: ");
                    double latitude = Double.parseDouble(sc.nextLine());

                    System.out.print("Enter your longitude: ");
                    double longitude = Double.parseDouble(sc.nextLine());

                    System.out.print("Enter food categories (comma-separated, leave empty for any): ");
                    String categoryInput = sc.nextLine().trim();

                    // Put the categoryInput into a list
                    ArrayList<String> categories = new ArrayList<>();
                    if (!categoryInput.isEmpty()) {
                        categories = new ArrayList<>(Arrays.asList(categoryInput.split("\\s*,\\s*")));
                    }

                    System.out.print("Enter minimum stars (1-5, or 0 for any): ");
                    String starsInput = sc.nextLine().trim();
                    double minStars = 0;
                    if (!starsInput.isEmpty()) {
                        minStars = Double.parseDouble(starsInput);
                    }

                    System.out.print("Enter price category ($, $$, $$$) or leave empty: ");
                    String price = sc.nextLine().trim();

                    String requestId = UUID.randomUUID().toString();
                    MapReduceRequest request = new MapReduceRequest(
                            latitude,
                            longitude,
                            categories,
                            minStars,
                            price,
                            5.0, // radius in km
                            requestId
                    );

                    out.writeObject(request);
                    out.flush();

                    System.out.println("Searching with filters...");

                    // Receive from master
                    ArrayList<Store> results = (ArrayList<Store>) in.readObject();

                    if (results.isEmpty()) {
                        System.out.println("No stores found matching your filters.");
                    } else {
                        System.out.println("\nFiltered Stores:");
                        for (Store store : results) {
                            System.out.println(store);
                            System.out.println("-----------");
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error connecting to server: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

            } else if (option.equals("3")) {
                String storeName = null;
                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    System.out.print("Enter store name you want to buy from: ");
                    storeName = sc.nextLine();

                    // Send to master
                    out.writeObject("fetchProducts");
                    out.flush();

                    out.writeObject(storeName);
                    out.flush();

                    // Receive from master
                    ArrayList<Product> storeProducts = (ArrayList<Product>) in.readObject();

                    if (storeProducts.isEmpty()) {
                        System.out.println("No products available for this store.");
                        return;
                    }

                    System.out.println("\nAvailable products:");
                    for (Product p : storeProducts) {
                        System.out.println("- " + p.getName() + " (" + p.getCategory() + ") - " + p.getPrice() + "€ | Available: " + p.getQuantity());
                    }

                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                requestSocket = null;
                out = null;
                in = null;

                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    // Products to purchase
                    ArrayList<Product> products = new ArrayList<>();
                    while (true) {
                        System.out.print("Enter product name (or type 'done' to finish): ");
                        String name = sc.nextLine();
                        if (name.equalsIgnoreCase("done")) break;

                        System.out.print("Enter quantity: ");
                        int quantity = Integer.parseInt(sc.nextLine());

                        // Create a product with only name and quantity and worker will fill the rest
                        products.add(new Product(name, "", quantity, 0.0));
                    }

                    System.out.print("Enter your name: ");
                    String customerName = sc.nextLine();

                    System.out.print("Enter your email: ");
                    String email = sc.nextLine();

                    Purchase purchase = new Purchase(customerName, email, products);

                    // Send to master
                    out.writeObject("purchase");
                    out.flush();

                    out.writeObject(purchase);
                    out.flush();

                    out.writeObject(storeName);
                    out.flush();

                    // Receive from master
                    String response = (String) in.readObject();
                    System.out.println("Server response: " + response);

                } catch (Exception e) {
                    System.err.println("Error during purchase: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

            } else if (option.equals("4")) {
                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    // Send to master
                    out.writeObject("rate");
                    out.flush();

                    System.out.print("Enter store name to rate: ");
                    String storeName = sc.nextLine();

                    System.out.print("Enter rating (1 to 5): ");
                    int rating = Integer.parseInt(sc.nextLine());

                    while (rating < 1 || rating > 5) {
                        System.out.print("Invalid rating. Please enter a number between 1 and 5: ");
                        rating = Integer.parseInt(sc.nextLine());
                    }

                    out.writeObject(storeName);
                    out.flush();

                    out.writeObject(rating);
                    out.flush();

                    // Receive from master
                    String response = (String) in.readObject();
                    System.out.println("Server: " + response);

                } catch (Exception e) {
                    System.err.println("Error rating store: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

            } else if (option.equals("5")) {
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }
}
