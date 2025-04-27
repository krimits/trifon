import java.io.Serializable;

/**
 * Defines shared constants and Enums for the FoodApp application.
 */
public class Constants {

    /**
     * Represents the different roles or actions that can be sent over the network.
     * Implements Serializable to be sent via ObjectOutputStream.
     */
    public enum Role implements Serializable {
        MANAGER,          // Manager adding stores
        CLIENT,           // Client requesting nearby stores (default filter)
        FILTER,           // Client requesting stores with specific filters
        FIND_STORE,       // Manager checking if a store exists
        FIND_PRODUCT,     // Manager checking if a product exists (visible only)
        FIND_PRODUCT2,    // Manager checking product status (visible or hidden)
        AMOUNT_INC,       // Manager increasing product quantity
        AMOUNT_DEC,       // Manager decreasing product quantity
        NEW_PRODUCT,      // Manager adding a new product
        REMOVE,           // Manager marking a product as hidden
        STORE_TYPE,       // Manager requesting sales aggregation by store type
        PRODUCT_CATEGORY, // Manager requesting sales aggregation by product category
        FETCH_PRODUCTS,   // Client requesting products for a specific store
        PURCHASE,         // Client making a purchase
        RATE              // Client rating a store
    }

    /**
     * Represents the visibility status of a product.
     * Implements Serializable to be potentially included in objects sent over the network.
     */
    public enum ProductStatus implements Serializable {
        VISIBLE,
        HIDDEN
    }

    // Default network configuration
    public static final String DEFAULT_MASTER_IP = "127.0.0.1";
    public static final int DEFAULT_MASTER_PORT = 4321;

    // Private constructor to prevent instantiation
    private Constants() {}
}
