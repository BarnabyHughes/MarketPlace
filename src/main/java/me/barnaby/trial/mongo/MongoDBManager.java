package me.barnaby.trial.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDBManager handles connecting to MongoDB using settings from mongo.yml,
 * and provides helper methods to store and retrieve player data, item listings,
 * and transaction history.
 *
 * The mongo.yml file should have a structure similar to:
 *   uri: "mongodb://localhost:27017"
 *   database: "marketplace"
 */
public class MongoDBManager {
    private final MarketPlace plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;

    // File configuration for mongo.yml
    private FileConfiguration mongoConfig;

    public MongoDBManager(MarketPlace plugin) {
        this.plugin = plugin;
    }

    /**
     * Connects to the MongoDB database using settings from mongo.yml.
     */
    public void connect() {
        // Retrieve the mongo configuration using the Config Manager
        // (Assumes you have a ConfigType.MONGO defined in your ConfigType enum)
        FileConfiguration mongoConfig = plugin.getConfigManager().getConfig(ConfigType.MONGO);

        // Get the URI and database name from the configuration, or use defaults if not set.
        String uri = mongoConfig.getString("uri", "mongodb://localhost:27017");
        String dbName = mongoConfig.getString("database", "marketplace");

        // Optionally retrieve username and password if provided in the config
        String username = mongoConfig.getString("username", "");
        String password = mongoConfig.getString("password", "");

        MongoClientSettings settings;
        if (!username.isEmpty() && !password.isEmpty()) {
            // If credentials are provided, create a credential object.
            MongoCredential credential = MongoCredential.createCredential(username, dbName, password.toCharArray());
            settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .credential(credential)
                    .build();
        } else {
            // Otherwise, connect without authentication.
            settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .build();
        }

        // Create the MongoDB client and get the specified database.
        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(dbName);

        plugin.getLogger().info("Connected to MongoDB database: " + dbName);
    }


    /**
     * Disconnects from MongoDB.
     */
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("Disconnected from MongoDB.");
        }
    }

    /**
     * Gets the MongoDatabase instance.
     *
     * @return The MongoDatabase instance.
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Returns a collection from the database.
     *
     * @param collectionName The name of the collection.
     * @return The MongoCollection<Document> instance.
     */
    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    /**
     * Inserts or updates a document in the specified collection.
     *
     * @param collectionName The collection where the document will be stored.
     * @param query The query to identify the document.
     * @param update The new data to set.
     */
    public void setValue(String collectionName, Document query, Document update) {
        MongoCollection<Document> collection = getCollection(collectionName);
        collection.updateOne(query, new Document("$set", update), new UpdateOptions().upsert(true));
    }

    /**
     * Retrieves a document from the specified collection.
     *
     * @param collectionName The collection to search.
     * @param query The query used to find the document.
     * @return The found document, or null if not found.
     */
    public Document getValue(String collectionName, Document query) {
        MongoCollection<Document> collection = getCollection(collectionName);
        return collection.find(query).first();
    }

    /**
     * Deletes a document from the specified collection.
     *
     * @param collectionName The collection from which to delete.
     * @param query The query used to identify the document.
     */
    public void deleteValue(String collectionName, Document query) {
        MongoCollection<Document> collection = getCollection(collectionName);
        collection.deleteOne(query);
    }

    // ----------------------------
    // Specific Methods Based on the PDF Requirements
    // ----------------------------

    /**
     * Inserts new player data into the "playerData" collection.
     *
     * @param playerId The unique identifier of the player.
     * @param data A Document containing the player's data.
     */
    public void insertPlayerData(String playerId, Document data) {
        MongoCollection<Document> collection = getCollection("playerData");
        data.append("playerId", playerId);
        collection.insertOne(data);
    }

    /**
     * Updates an existing player's data.
     *
     * @param playerId The player's unique identifier.
     * @param data A Document containing the updated player data.
     */
    public void updatePlayerData(String playerId, Document data) {
        MongoCollection<Document> collection = getCollection("playerData");
        collection.updateOne(
                new Document("playerId", playerId),
                new Document("$set", data),
                new UpdateOptions().upsert(true)
        );
    }

    /**
     * Retrieves player data from the "playerData" collection.
     *
     * @param playerId The player's unique identifier.
     * @return The Document containing player data, or null if not found.
     */
    public Document getPlayerData(String playerId) {
        MongoCollection<Document> collection = getCollection("playerData");
        return collection.find(new Document("playerId", playerId)).first();
    }

    /**
     * Inserts a new item listing into the "itemListings" collection.
     *
     * @param itemData A Document containing the item listing details.
     */
    public void insertItemListing(Document itemData) {
        MongoCollection<Document> collection = getCollection("itemListings");
        collection.insertOne(itemData);
    }

    /**
     * Retrieves an item listing by its unique item ID.
     *
     * @param itemId The unique identifier for the item listing.
     * @return The Document representing the item listing, or null if not found.
     */
    public Document getItemListing(String itemId) {
        MongoCollection<Document> collection = getCollection("itemListings");
        return collection.find(new Document("itemId", itemId)).first();
    }

    /**
     * Inserts a new transaction into the "transactions" collection.
     *
     * @param transactionData A Document containing transaction details.
     */
    public void insertTransaction(Document transactionData) {
        MongoCollection<Document> collection = getCollection("transactions");
        collection.insertOne(transactionData);
    }

    /**
     * Retrieves the transaction history for a given player.
     *
     * @param playerId The player's unique identifier.
     * @return A list of Documents representing the player's transaction history.
     */
    public List<Document> getTransactionHistory(String playerId) {
        MongoCollection<Document> collection = getCollection("transactions");
        return collection.find(new Document("playerId", playerId)).into(new ArrayList<>());
    }
}
