package com.example.coen390.fridgefriends.controller;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.coen390.fridgefriends.model.GroceryItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static DatabaseHelper sInstance;

    private static final String DATABASE_NAME = "fridge_friends.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_GROCERY_ITEMS = "grocery_items";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_IS_OWNED = "is_owned";
    private static final String COLUMN_EXPIRY_DATE = "expiry_date";
    private static final String COLUMN_IMAGE_URL = "image_url";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_UNIT = "unit";
    private static final String COLUMN_MEMO = "memo";
    private static final String COLUMN_DEVICE_ID = "device_id";
    private static final String COLUMN_IS_STRIKED = "is_striked";

    private final Context mContext;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_GROCERY_ITEMS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_IS_OWNED + " INTEGER NOT NULL DEFAULT 0, " +
                COLUMN_EXPIRY_DATE + " TEXT, " +
                COLUMN_IMAGE_URL + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_QUANTITY + " INTEGER DEFAULT 1, " +
                COLUMN_UNIT + " TEXT DEFAULT 'pcs', " +
                COLUMN_MEMO + " TEXT, " +
                COLUMN_DEVICE_ID + " TEXT DEFAULT 'None', " +
                COLUMN_IS_STRIKED + " INTEGER DEFAULT 0" +
                ");";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_GROCERY_ITEMS + " ADD COLUMN " + COLUMN_DEVICE_ID + " TEXT DEFAULT 'None';");
            } catch (Exception e) {
                Log.e(TAG, "Failed to alter table onUpgrade, recreating...", e);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROCERY_ITEMS);
                onCreate(db);
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Ensure column exists for safety
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_GROCERY_ITEMS + ")", null)) {
            boolean hasDeviceId = false;
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIndex != -1 && COLUMN_DEVICE_ID.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    hasDeviceId = true;
                    break;
                }
            }
            if (!hasDeviceId) {
                db.execSQL("ALTER TABLE " + TABLE_GROCERY_ITEMS + " ADD COLUMN " + COLUMN_DEVICE_ID + " TEXT DEFAULT 'None';");
            }
        } catch (Exception ignored) {}
    }

    public long insertGroceryItem(GroceryItem item) {
        if (item == null || item.isEmptyRow()) return -1;
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, item.getName());
        values.put(COLUMN_IS_OWNED, item.isOwned() ? 1 : 0);
        values.put(COLUMN_EXPIRY_DATE, item.getExpiryDate() == null ? "" : item.getExpiryDate());
        values.put(COLUMN_IMAGE_URL, item.getImageUrl() == null ? "" : item.getImageUrl());
        values.put(COLUMN_CATEGORY, item.getCategory() == null ? "" : item.getCategory());
        values.put(COLUMN_QUANTITY, item.getQuantity());
        values.put(COLUMN_UNIT, item.getUnit() == null ? "pcs" : item.getUnit());
        values.put(COLUMN_MEMO, item.getMemo() == null ? "" : item.getMemo());
        values.put(COLUMN_DEVICE_ID, item.getDeviceId() == null ? "None" : item.getDeviceId());
        values.put(COLUMN_IS_STRIKED, item.isStriked() ? 1 : 0);

        long id = db.insert(TABLE_GROCERY_ITEMS, null, values);
        if (id != -1) {
            item.setId(id);
        }
        return id;
    }

    public int updateGroceryItem(GroceryItem item) {
        if (item == null) return 0;
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, item.getName());
        values.put(COLUMN_IS_OWNED, item.isOwned() ? 1 : 0);
        values.put(COLUMN_EXPIRY_DATE, item.getExpiryDate() == null ? "" : item.getExpiryDate());
        values.put(COLUMN_IMAGE_URL, item.getImageUrl() == null ? "" : item.getImageUrl());
        values.put(COLUMN_CATEGORY, item.getCategory() == null ? "" : item.getCategory());
        values.put(COLUMN_QUANTITY, item.getQuantity());
        values.put(COLUMN_UNIT, item.getUnit() == null ? "pcs" : item.getUnit());
        values.put(COLUMN_MEMO, item.getMemo() == null ? "" : item.getMemo());
        values.put(COLUMN_DEVICE_ID, item.getDeviceId() == null ? "None" : item.getDeviceId());
        values.put(COLUMN_IS_STRIKED, item.isStriked() ? 1 : 0);

        if (item.getId() != -1) {
            return db.update(TABLE_GROCERY_ITEMS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(item.getId())});
        } else {
            return db.update(TABLE_GROCERY_ITEMS, values, COLUMN_NAME + " = ?", new String[]{item.getName()});
        }
    }

    public int deleteGroceryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_GROCERY_ITEMS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int deleteGroceryItemByName(String name) {
        if (name == null) return 0;
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_GROCERY_ITEMS, COLUMN_NAME + " = ?", new String[]{name});
    }

    public List<GroceryItem> getAllGroceryItems() {
        List<GroceryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(TABLE_GROCERY_ITEMS, null, null, null, null, null, COLUMN_ID + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int nameIdx = cursor.getColumnIndex(COLUMN_NAME);
                int ownedIdx = cursor.getColumnIndex(COLUMN_IS_OWNED);
                int expiryIdx = cursor.getColumnIndex(COLUMN_EXPIRY_DATE);
                int imageIdx = cursor.getColumnIndex(COLUMN_IMAGE_URL);
                int categoryIdx = cursor.getColumnIndex(COLUMN_CATEGORY);
                int quantityIdx = cursor.getColumnIndex(COLUMN_QUANTITY);
                int unitIdx = cursor.getColumnIndex(COLUMN_UNIT);
                int memoIdx = cursor.getColumnIndex(COLUMN_MEMO);
                int deviceIdx = cursor.getColumnIndex(COLUMN_DEVICE_ID);
                int strikedIdx = cursor.getColumnIndex(COLUMN_IS_STRIKED);

                do {
                    long id = idIdx != -1 ? cursor.getLong(idIdx) : -1;
                    String name = nameIdx != -1 ? cursor.getString(nameIdx) : "";
                    boolean isOwned = ownedIdx != -1 && cursor.getInt(ownedIdx) == 1;
                    String expiryDate = expiryIdx != -1 ? cursor.getString(expiryIdx) : "";
                    String imageUrl = imageIdx != -1 ? cursor.getString(imageIdx) : "";
                    String category = categoryIdx != -1 ? cursor.getString(categoryIdx) : "";
                    int quantity = quantityIdx != -1 ? cursor.getInt(quantityIdx) : 1;
                    String unit = unitIdx != -1 ? cursor.getString(unitIdx) : "pcs";
                    String memo = memoIdx != -1 ? cursor.getString(memoIdx) : "";
                    String deviceId = deviceIdx != -1 ? cursor.getString(deviceIdx) : "None";
                    boolean isStriked = strikedIdx != -1 && cursor.getInt(strikedIdx) == 1;

                    GroceryItem item = new GroceryItem(id, name, isOwned, expiryDate, imageUrl, category, quantity, unit, memo, deviceId);
                    item.setStriked(isStriked);
                    items.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying all grocery items", e);
        }
        return items;
    }

    public List<GroceryItem> getItemsByDevice(String deviceName) {
        List<GroceryItem> items = new ArrayList<>();
        if (deviceName == null || deviceName.trim().isEmpty() || "None".equalsIgnoreCase(deviceName)) {
            return items;
        }

        SQLiteDatabase db = getReadableDatabase();
        String selection = COLUMN_DEVICE_ID + " = ? AND " + COLUMN_DEVICE_ID + " != 'None'";
        String[] selectionArgs = new String[]{deviceName.trim()};

        try (Cursor cursor = db.query(TABLE_GROCERY_ITEMS, null, selection, selectionArgs, null, null, COLUMN_NAME + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int nameIdx = cursor.getColumnIndex(COLUMN_NAME);
                int ownedIdx = cursor.getColumnIndex(COLUMN_IS_OWNED);
                int expiryIdx = cursor.getColumnIndex(COLUMN_EXPIRY_DATE);
                int imageIdx = cursor.getColumnIndex(COLUMN_IMAGE_URL);
                int categoryIdx = cursor.getColumnIndex(COLUMN_CATEGORY);
                int quantityIdx = cursor.getColumnIndex(COLUMN_QUANTITY);
                int unitIdx = cursor.getColumnIndex(COLUMN_UNIT);
                int memoIdx = cursor.getColumnIndex(COLUMN_MEMO);
                int deviceIdx = cursor.getColumnIndex(COLUMN_DEVICE_ID);
                int strikedIdx = cursor.getColumnIndex(COLUMN_IS_STRIKED);

                do {
                    long id = idIdx != -1 ? cursor.getLong(idIdx) : -1;
                    String name = nameIdx != -1 ? cursor.getString(nameIdx) : "";
                    boolean isOwned = ownedIdx != -1 && cursor.getInt(ownedIdx) == 1;
                    String expiryDate = expiryIdx != -1 ? cursor.getString(expiryIdx) : "";
                    String imageUrl = imageIdx != -1 ? cursor.getString(imageIdx) : "";
                    String category = categoryIdx != -1 ? cursor.getString(categoryIdx) : "";
                    int quantity = quantityIdx != -1 ? cursor.getInt(quantityIdx) : 1;
                    String unit = unitIdx != -1 ? cursor.getString(unitIdx) : "pcs";
                    String memo = memoIdx != -1 ? cursor.getString(memoIdx) : "";
                    String deviceId = deviceIdx != -1 ? cursor.getString(deviceIdx) : "None";
                    boolean isStriked = strikedIdx != -1 && cursor.getInt(strikedIdx) == 1;

                    GroceryItem item = new GroceryItem(id, name, isOwned, expiryDate, imageUrl, category, quantity, unit, memo, deviceId);
                    item.setStriked(isStriked);
                    items.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying items by device: " + deviceName, e);
        }
        return items;
    }

    public void syncFromSharedPreferences() {
        try {
            SharedPreferences prefs = mContext.getSharedPreferences("GroceryPrefs", Context.MODE_PRIVATE);
            String jsonStr = prefs.getString("grocery_list", "[]");
            JSONArray array = new JSONArray(jsonStr);

            List<GroceryItem> dbItems = getAllGroceryItems();
            if (dbItems.isEmpty() && array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    GroceryItem item = GroceryItem.fromJSON(obj);
                    insertGroceryItem(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing from SharedPreferences", e);
        }
    }
}
