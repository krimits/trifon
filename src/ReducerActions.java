import java.io.*;
import java.net.*;
import java.util.*;

public class ReducerActions extends Thread {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket connection;
    private static Map<String, ArrayList<Store>> resultsMap = new HashMap<>();

    public ReducerActions(Socket connection) {
        try {
            this.connection = connection;
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String role = (String) in.readObject();

            if (role.equals("client") || role.equals("filter")) {
                String requestId = (String) in.readObject();
                int totalWorkers = (int) in.readObject();

                ArrayList<Store> merged = new ArrayList<>();
                Set<String> addedNames = new HashSet<>();

                for (int i = 0; i < totalWorkers; i++) { // for all workers
                    ArrayList<Store> partial = (ArrayList<Store>) in.readObject();

                    for (Store store : partial) { // merge all the partial results
                        if (!addedNames.contains(store.getStoreName().toLowerCase())) {
                            merged.add(store);
                            addedNames.add(store.getStoreName().toLowerCase());
                        }
                    }
                }

                resultsMap.put(requestId, merged);

                // Send to master
                out.writeObject(merged);
                out.flush();

            } else if (role.equals("storeType")) {
                // Receive from master
                int totalWorkers = (int) in.readObject();

                Map<String, Integer> merged = new HashMap<>();

                for (int i = 0; i < totalWorkers; i++) { // for all workers
                    Map<String, Integer> partial = (Map<String, Integer>) in.readObject();

                    for (Map.Entry<String, Integer> entry : partial.entrySet()) { // merge all the partial results
                        merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }

                // Send to master
                out.writeObject(merged);
                out.flush();

            } else if (role.equals("productCategory")) {
                // Receive from master
                int totalWorkers = (int) in.readObject();

                Map<String, Integer> merged = new HashMap<>();

                for (int i = 0; i < totalWorkers; i++) { // for all workers
                    Map<String, Integer> partial = (Map<String, Integer>) in.readObject();

                    for (Map.Entry<String, Integer> entry : partial.entrySet()) { // merge all the partial results
                        merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }

                // Send to master
                out.writeObject(merged);
                out.flush();
            }

        } catch (Exception e) {
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
