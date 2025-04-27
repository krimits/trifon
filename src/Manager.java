import org.json.JSONArray;
import org.json.JSONObject;


import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class Manager {
    public static void main(String[] args) throws ParseException, FileNotFoundException {
        Scanner sc = new Scanner(System.in);

        boolean flag = true;

        while (flag) {
            // Display menu options
            System.out.println("1.Add store");
            System.out.println("2.Add Product");
            System.out.println("3.Remove Product");
            System.out.println("4.Total sales by store type");
            System.out.println("5.Total sales by product category");
            System.out.println("6.Exit");
            System.out.print("Choose an option: ");
            String number = sc.nextLine();

            if (number.equals("1")) {
                System.out.print("Do you want to add a store from file (f) or manually (m)? ");
                String mode = sc.nextLine().trim().toLowerCase();
                if (mode.equals("f")) {
                    // --- Υφιστάμενη λογική για προσθήκη από αρχείο ---
                    ArrayList<Store> stores = new ArrayList<>();
                    System.out.print("Give the json file of the store: ");
                    String jsonPath = sc.nextLine();
                    try (FileReader reader = new FileReader(jsonPath)) {
                        StringBuilder contentBuilder = new StringBuilder();
                        int c;
                        while ((c = reader.read()) != -1) {
                            contentBuilder.append((char) c); // Read JSON file content
                        }
                        String jsonContent = contentBuilder.toString();
                        JSONArray jsonArray = new JSONArray(jsonContent);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);

                            // Read store info
                            String name = (String) jsonObject.get("StoreName");
                            double latitude = ((Number) jsonObject.get("Latitude")).doubleValue();
                            double longitude = ((Number) jsonObject.get("Longitude")).doubleValue();
                            String category = (String) jsonObject.get("FoodCategory");
                            double stars = ((Number) jsonObject.get("Stars")).doubleValue();
                            int reviews = ((Number) jsonObject.get("NoOfVotes")).intValue();
                            String storeLogoPath = (String) jsonObject.get("StoreLogo");

                            // Read product list
                            ArrayList<Product> products = new ArrayList<>();
                            JSONArray productsArray = (JSONArray) jsonObject.get("Products");
                            for (Object prodObj : productsArray) {
                                JSONObject productJson = (JSONObject) prodObj;

                                // Extract product fields
                                String productName = (String) productJson.get("ProductName");
                                String productType = (String) productJson.get("ProductType");
                                int amount = ((Number) productJson.get("AvailableAmount")).intValue();
                                double productPrice = ((Number) productJson.get("Price")).doubleValue();

                                products.add(new Product(productName, productType, amount, productPrice));
                            }

                            Store s = new Store(name, latitude, longitude, category, stars, reviews, storeLogoPath, products);

                            stores.add(s);
                            System.out.println(s);
                            // Εμφάνιση logo στην κονσόλα αν υπάρχει path
                            if (storeLogoPath != null && !storeLogoPath.isEmpty()) {
                                File logoFile = new File(storeLogoPath);
                                if (logoFile.exists()) {
                                    System.out.println("[Logo: " + storeLogoPath + "]");
                                } else {
                                    System.out.println("[Logo not found: " + storeLogoPath + "]");
                                }
                            }

                            Socket requestSocket = null;
                            ObjectOutputStream out = null;
                            ObjectInputStream in = null;
                            try {
                                // Connect to master
                                requestSocket = new Socket("127.0.0.1", 4321);
                                out = new ObjectOutputStream(requestSocket.getOutputStream());
                                in = new ObjectInputStream(requestSocket.getInputStream());

                                // Send to master
                                out.writeObject("manager");
                                out.flush();

                                out.writeObject(stores);
                                out.flush();

                                // Receive from master
                                String res = (String) in.readObject();
                                System.out.println(res);
                                System.out.print("\n");

                                // --- Ενημέρωση store2.json ---
                                // Αντί να προσθέτουμε μόνο αν δεν υπάρχει, να γράφουμε ΠΑΝΤΑ το τελευταίο κατάστημα
                                JSONArray store2Array;
                                try {
                                    String store2Content = new String(Files.readAllBytes(Paths.get("store2.json")));
                                    store2Array = new JSONArray(store2Content);
                                } catch (Exception e) {
                                    store2Array = new JSONArray(); // Αν δεν υπάρχει, ξεκίνα νέο
                                }
                                // ΠΡΟΣΘΗΚΗ ΠΑΝΤΑ του νέου καταστήματος (χωρίς έλεγχο διπλοεγγραφής)
                                JSONObject newStore = new JSONObject();
                                newStore.put("StoreName", s.getStoreName());
                                newStore.put("Latitude", s.getLatitude());
                                newStore.put("Longitude", s.getLongitude());
                                newStore.put("FoodCategory", s.getCategory());
                                newStore.put("Stars", s.getStars());
                                newStore.put("NoOfVotes", s.getNoOfReviews());
                                newStore.put("StoreLogo", storeLogoPath);
                                JSONArray productsArrayJson = new JSONArray();
                                for (Product product : s.getProducts()) {
                                    JSONObject prodObj = new JSONObject();
                                    prodObj.put("ProductName", product.getName());
                                    prodObj.put("ProductType", product.getCategory());
                                    prodObj.put("AvailableAmount", product.getQuantity());
                                    prodObj.put("Price", product.getPrice());
                                    productsArrayJson.put(prodObj);
                                }
                                newStore.put("Products", productsArrayJson);
                                store2Array.put(newStore);
                                // Γράψε πίσω το ενημερωμένο store2.json
                                try (FileWriter file = new FileWriter("store2.json")) {
                                    file.write(store2Array.toString(2));
                                }
                                // --- Τέλος ενημέρωσης store2.json ---

                            } catch (UnknownHostException unknownHost) {
                                System.err.println("You are trying to connect to an unknown host!");
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            } catch (ClassNotFoundException e) {
                                // TODO Auto-generated catch block
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
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (mode.equals("m")) {
                    // --- Νέα λογική για προσθήκη καταστήματος από το console ---
                    System.out.print("Store Name: ");
                    String name = sc.nextLine();
                    System.out.print("Latitude: ");
                    double latitude = Double.parseDouble(sc.nextLine());
                    System.out.print("Longitude: ");
                    double longitude = Double.parseDouble(sc.nextLine());
                    System.out.print("Food Category: ");
                    String category = sc.nextLine();
                    System.out.print("Stars (1-5): ");
                    double stars = Double.parseDouble(sc.nextLine());
                    System.out.print("No Of Votes: ");
                    int reviews = Integer.parseInt(sc.nextLine());
                    System.out.print("Store Logo Path: ");
                    String storeLogoPath = sc.nextLine();
                    ArrayList<Product> products = new ArrayList<>();
                    while (true) {
                        System.out.print("Add product? (y/n): ");
                        String addProd = sc.nextLine().trim().toLowerCase();
                        if (!addProd.equals("y")) break;
                        System.out.print("  Product Name: ");
                        String productName = sc.nextLine();
                        System.out.print("  Product Type: ");
                        String productType = sc.nextLine();
                        System.out.print("  Available Amount: ");
                        int amount = Integer.parseInt(sc.nextLine());
                        System.out.print("  Price: ");
                        double productPrice = Double.parseDouble(sc.nextLine());
                        products.add(new Product(productName, productType, amount, productPrice));
                    }
                    Store s = new Store(name, latitude, longitude, category, stars, reviews, storeLogoPath, products);
                    System.out.println(s);
                    // Εμφάνιση logo στην κονσόλα αν υπάρχει path
                    if (storeLogoPath != null && !storeLogoPath.isEmpty()) {
                        File logoFile = new File(storeLogoPath);
                        if (logoFile.exists()) {
                            System.out.println("[Logo: " + storeLogoPath + "]");
                        } else {
                            System.out.println("[Logo not found: " + storeLogoPath + "]");
                        }
                    }
                    // Στείλε στον Master
                    try {
                        Socket requestSocket = new Socket("127.0.0.1", 4321);
                        ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
                        ArrayList<Store> stores = new ArrayList<>();
                        stores.add(s);
                        out.writeObject("manager");
                        out.flush();
                        out.writeObject(stores);
                        out.flush();
                        String res = (String) in.readObject();
                        System.out.println(res);
                        System.out.print("\n");
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Ενημέρωσε το store2.json
                    JSONArray store2Array;
                    try {
                        String store2Content = new String(Files.readAllBytes(Paths.get("store2.json")));
                        store2Array = new JSONArray(store2Content);
                    } catch (Exception e) {
                        store2Array = new JSONArray();
                    }
                    JSONObject newStore = new JSONObject();
                    newStore.put("StoreName", s.getStoreName());
                    newStore.put("Latitude", s.getLatitude());
                    newStore.put("Longitude", s.getLongitude());
                    newStore.put("FoodCategory", s.getCategory());
                    newStore.put("Stars", s.getStars());
                    newStore.put("NoOfVotes", s.getNoOfReviews());
                    newStore.put("StoreLogo", storeLogoPath);
                    JSONArray productsArrayJson = new JSONArray();
                    for (Product product : s.getProducts()) {
                        JSONObject prodObj = new JSONObject();
                        prodObj.put("ProductName", product.getName());
                        prodObj.put("ProductType", product.getCategory());
                        prodObj.put("AvailableAmount", product.getQuantity());
                        prodObj.put("Price", product.getPrice());
                        productsArrayJson.put(prodObj);
                    }
                    newStore.put("Products", productsArrayJson);
                    store2Array.put(newStore);
                    try (FileWriter file = new FileWriter("store2.json")) {
                        file.write(store2Array.toString(2));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } else if (number.equals("2")) {
                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;

                String s = null;
                String ex = null;

                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    System.out.print("Enter store name to add a product: ");
                    String storeName = sc.nextLine();

                    // Send to master
                    out.writeObject("findStore");
                    out.flush();

                    out.writeObject(storeName);
                    out.flush();

                    // Receive from master
                    s = (String) in.readObject();

                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

                //if the store doesn't exist the worker sends back null, otherwise sends the StoreName.
                if (s != null){
                    requestSocket = null;
                    out = null;
                    in = null;
                    String productName = null;

                    try {
                        // Connect to master
                        requestSocket = new Socket("127.0.0.1", 4321);
                        out = new ObjectOutputStream(requestSocket.getOutputStream());
                        in = new ObjectInputStream(requestSocket.getInputStream());

                        System.out.print("Enter Product Name: ");
                        productName = sc.nextLine();

                        // Send to master
                        out.writeObject("findProduct");
                        out.flush();

                        out.writeObject(s);
                        out.flush();

                        out.writeObject(productName);
                        out.flush();

                        // Receive from master
                        ex = (String) in.readObject();

                    } catch (UnknownHostException unknownHost) {
                        System.err.println("You are trying to connect to an unknown host!");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
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

                    //if the product doesn't exist the worker sends back "doesnt exist", otherwise sends "exists"
                    if (ex.equalsIgnoreCase("exists")) {
                        try {
                            // Connect to master
                            requestSocket = new Socket("127.0.0.1", 4321);
                            out = new ObjectOutputStream(requestSocket.getOutputStream());
                            in = new ObjectInputStream(requestSocket.getInputStream());

                            System.out.print("Product already exists. How much would you like to add to the quantity? ");
                            int additionalAmount = Integer.parseInt(sc.nextLine());

                            // Send to master
                            out.writeObject("AmountInc");
                            out.flush();

                            out.writeObject(s);
                            out.flush();

                            out.writeObject(productName);
                            out.flush();

                            out.writeInt(additionalAmount);
                            out.flush();

                            // Receive from master
                            String res = (String) in.readObject();
                            System.out.println(res);
                            System.out.print("\n");

                        } catch (UnknownHostException unknownHost) {
                            System.err.println("You are trying to connect to an unknown host!");
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } finally {
                            try {
                                in.close();
                                out.close();
                                requestSocket.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    } else {

                        Product pro = null;
                        try {
                            // Connect to master
                            requestSocket = new Socket("127.0.0.1", 4321);
                            out = new ObjectOutputStream(requestSocket.getOutputStream());
                            in = new ObjectInputStream(requestSocket.getInputStream());

                            System.out.print("Enter Product Type: ");
                            String productType = sc.nextLine();

                            System.out.print("Enter Available Amount: ");
                            int amount = Integer.parseInt(sc.nextLine());

                            System.out.print("Enter Product Price: ");
                            double productPrice = Double.parseDouble(sc.nextLine());

                            pro = new Product(productName, productType, amount, productPrice);

                            // Send to master
                            out.writeObject("NewProduct");
                            out.flush();

                            out.writeObject(s);
                            out.flush();

                            out.writeObject(pro);
                            out.flush();

                            // Receive from master
                            String res = (String) in.readObject();
                            System.out.println(res);
                            System.out.print("\n");

                        } catch (UnknownHostException unknownHost) {
                            System.err.println("You are trying to connect to an unknown host!");
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } finally {
                            try {
                                in.close();
                                out.close();
                                requestSocket.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }

                }else System.out.println("Store not found.");


            } else if (number.equals("3")) {
                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;

                String storeName = null;

                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    System.out.print("Enter store name to remove a product: ");
                    String s = sc.nextLine();

                    // Send to master
                    out.writeObject("findStore");
                    out.flush();

                    out.writeObject(s);
                    out.flush();

                    // Receive from master
                    storeName = (String) in.readObject();

                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

                //if the store doesn't exist the worker sends back null, otherwise sends the StoreName.
                if (storeName != null) {
                    requestSocket = null;
                    out = null;
                    in = null;

                    String productName = null;

                    try {
                        // Connect to master
                        requestSocket = new Socket("127.0.0.1", 4321);
                        out = new ObjectOutputStream(requestSocket.getOutputStream());
                        in = new ObjectInputStream(requestSocket.getInputStream());

                        System.out.print("Enter Product Name:");
                        String p = sc.nextLine();

                        // Send to master
                        out.writeObject("findProduct2");
                        out.flush();

                        out.writeObject(storeName);
                        out.flush();

                        out.writeObject(p);
                        out.flush();

                        // Receive from master
                        productName = (String) in.readObject();

                    } catch (UnknownHostException unknownHost) {
                        System.err.println("You are trying to connect to an unknown host!");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            in.close();
                            out.close();
                            requestSocket.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }

                    //if the product doesn't exist the worker sends back null, otherwise sends the ProductName.
                    if (productName != null && productName!= "hidden") {

                        requestSocket = null;
                        out = null;
                        in = null;

                        try {
                            // Connect to master
                            requestSocket = new Socket("127.0.0.1", 4321);
                            out = new ObjectOutputStream(requestSocket.getOutputStream());
                            in = new ObjectInputStream(requestSocket.getInputStream());

                            System.out.println("1. Remove the product");
                            System.out.println("2. Decrease the quantity of the product");
                            System.out.print("Choose an option: ");
                            String num = sc.nextLine();

                            if(num.equals("1")){
                                // Send to master
                                out.writeObject("remove");
                                out.flush();

                                out.writeObject(storeName);
                                out.flush();

                                out.writeObject(productName);
                                out.flush();

                                // Receive from master
                                String res = (String) in.readObject();
                                System.out.println(res);
                                System.out.print("\n");

                            } else if (num.equals("2")) {

                                System.out.print("How much would you like to decrease the quantity?");
                                int amount = Integer.parseInt(sc.nextLine());

                                // Send to master
                                out.writeObject("AmountDec");
                                out.flush();

                                out.writeObject(storeName);
                                out.flush();

                                out.writeObject(productName);
                                out.flush();

                                out.writeInt(amount);
                                out.flush();

                                // Receive from master
                                String res = (String) in.readObject();
                                System.out.println(res);
                                System.out.print("\n");

                            }

                        } catch (UnknownHostException unknownHost) {
                            System.err.println("You are trying to connect to an unknown host!");
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } finally {
                            try {
                                in.close();
                                out.close();
                                requestSocket.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }

                    } else if (productName == null){
                        System.out.println("The product doesn't exist");
                    } else if (productName == "hidden") {
                        System.out.println("The product is already removed");
                    }

                }else System.out.println("Store not found.");


            } else if (number.equals("4")) {
                System.out.print("Enter the store type (e.g., pizzeria, burger):");
                String storeType = sc.nextLine();

                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                try{
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    // Send to master
                    out.writeObject("storeType");
                    out.flush();

                    out.writeObject(storeType);
                    out.flush();

                    // Receive from master
                    Map<String, Integer> result = (Map<String, Integer>) in.readObject();

                    int total = 0;
                    System.out.println("Sales by Store for type: " + storeType);
                    for (Map.Entry<String, Integer> entry : result.entrySet()) { // Print for every store
                        System.out.println("• " + entry.getKey() + ": " + entry.getValue());
                        total += entry.getValue();
                    }
                    System.out.println("Total Sales: " + total + "\n");

                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }


            } else if (number.equals("5")) {
                System.out.print("Enter the product category (e.g., pizza, salad, burger): ");
                String productCategory = sc.nextLine();

                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                try {
                    // Connect to master
                    requestSocket = new Socket("127.0.0.1", 4321);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    // Send to master
                    out.writeObject("productCategory");
                    out.flush();

                    out.writeObject(productCategory);
                    out.flush();

                    // Receive from master
                    Map<String, Integer> result = (Map<String, Integer>) in.readObject();

                    int total = 0;
                    System.out.println("Sales by Store for product category: " + productCategory);
                    for (Map.Entry<String, Integer> entry : result.entrySet()) { // Print for every store
                        System.out.println("• " + entry.getKey() + ": " + entry.getValue());
                        total += entry.getValue();
                    }
                    System.out.println("Total Sales: " + total + "\n");



                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        in.close();
                        out.close();
                        requestSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }


            } else if (number.equals("6")) {
                System.out.println("Exit");
                flag = false;
            } else {
                System.out.println("Wrong number. Try again");
            }
        }

    }
}