package com.example.coen390.fridgefriends.model;

import org.json.JSONException;
import org.json.JSONObject;

public class GroceryItem {
    private long mId = -1;
    private String mName;
    private boolean mIsOwned;
    private String mExpiryDate;
    private boolean mIsStriked;
    private boolean mIsEmptyRow;
    private boolean mIsSelected;
    private String mImageUrl;
    private String mCategory;
    private int mQuantity = 1;
    private String mUnit = "pcs";
    private String mMemo;
    private String mDeviceId = "None";

    public GroceryItem() {
        this.mIsEmptyRow = true;
        this.mIsSelected = false;
    }

    public GroceryItem(String name, boolean isOwned, String expiryDate) {
        this.mName = name;
        this.mIsOwned = isOwned;
        this.mExpiryDate = expiryDate;
        this.mIsEmptyRow = false;
        this.mIsStriked = false;
        this.mQuantity = 1;
        this.mUnit = "pcs";
        this.mDeviceId = "None";
    }

    public GroceryItem(String name, boolean isOwned, String expiryDate, String imageUrl, String category, int quantity, String unit, String memo) {
        this.mName = name;
        this.mIsOwned = isOwned;
        this.mExpiryDate = expiryDate;
        this.mImageUrl = imageUrl;
        this.mCategory = category;
        this.mQuantity = quantity > 0 ? quantity : 1;
        this.mUnit = (unit != null && !unit.isEmpty()) ? unit : "pcs";
        this.mMemo = memo;
        this.mIsEmptyRow = false;
        this.mIsStriked = false;
        this.mDeviceId = "None";
    }

    public GroceryItem(String name, boolean isOwned, String expiryDate, String imageUrl, String category, int quantity, String unit, String memo, String deviceId) {
        this.mName = name;
        this.mIsOwned = isOwned;
        this.mExpiryDate = expiryDate;
        this.mImageUrl = imageUrl;
        this.mCategory = category;
        this.mQuantity = quantity > 0 ? quantity : 1;
        this.mUnit = (unit != null && !unit.isEmpty()) ? unit : "pcs";
        this.mMemo = memo;
        this.mIsEmptyRow = false;
        this.mIsStriked = false;
        this.mDeviceId = (deviceId != null && !deviceId.trim().isEmpty()) ? deviceId : "None";
    }

    public GroceryItem(long id, String name, boolean isOwned, String expiryDate, String imageUrl, String category, int quantity, String unit, String memo, String deviceId) {
        this.mId = id;
        this.mName = name;
        this.mIsOwned = isOwned;
        this.mExpiryDate = expiryDate;
        this.mImageUrl = imageUrl;
        this.mCategory = category;
        this.mQuantity = quantity > 0 ? quantity : 1;
        this.mUnit = (unit != null && !unit.isEmpty()) ? unit : "pcs";
        this.mMemo = memo;
        this.mIsEmptyRow = false;
        this.mIsStriked = false;
        this.mDeviceId = (deviceId != null && !deviceId.trim().isEmpty()) ? deviceId : "None";
    }

    public long getId() { return mId; }
    public void setId(long id) { this.mId = id; }

    public String getName() { return mName; }
    public void setName(String name) { this.mName = name; this.mIsEmptyRow = false; }

    public boolean isOwned() { return mIsOwned; }
    public void setOwned(boolean owned) { mIsOwned = owned; }

    public String getExpiryDate() { return mExpiryDate; }
    public void setExpiryDate(String expiryDate) { this.mExpiryDate = expiryDate; }

    public boolean isEmptyRow() { return mIsEmptyRow; }
    public void setEmptyRow(boolean emptyRow) { mIsEmptyRow = emptyRow; }

    public boolean isStriked() { return mIsStriked; }
    public void setStriked(boolean striked) { mIsStriked = striked; }

    public boolean isSelected() { return mIsSelected; }
    public void setSelected(boolean selected) { mIsSelected = selected; }

    public String getImageUrl() { return mImageUrl; }
    public void setImageUrl(String imageUrl) { this.mImageUrl = imageUrl; }

    public String getCategory() { return mCategory; }
    public void setCategory(String category) { this.mCategory = category; }

    public int getQuantity() { return mQuantity; }
    public void setQuantity(int quantity) { this.mQuantity = quantity; }

    public String getUnit() { return mUnit; }
    public void setUnit(String unit) { this.mUnit = unit; }

    public String getMemo() { return mMemo; }
    public void setMemo(String memo) { this.mMemo = memo; }

    public String getDeviceId() { return mDeviceId; }
    public void setDeviceId(String deviceId) { this.mDeviceId = (deviceId != null && !deviceId.trim().isEmpty()) ? deviceId : "None"; }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        if (mId != -1) obj.put("id", mId);
        obj.put("name", mName);
        obj.put("isOwned", mIsOwned);
        obj.put("expiryDate", mExpiryDate == null ? "" : mExpiryDate);
        obj.put("imageUrl", mImageUrl == null ? "" : mImageUrl);
        obj.put("category", mCategory == null ? "" : mCategory);
        obj.put("quantity", mQuantity);
        obj.put("unit", mUnit == null ? "pcs" : mUnit);
        obj.put("memo", mMemo == null ? "" : mMemo);
        obj.put("deviceId", mDeviceId == null ? "None" : mDeviceId);
        return obj;
    }

    public static GroceryItem fromJSON(JSONObject obj) throws JSONException {
        long id = obj.optLong("id", -1);
        String name = obj.getString("name");
        boolean isOwned = obj.getBoolean("isOwned");
        String expiryDate = obj.optString("expiryDate", "");
        String imageUrl = obj.optString("imageUrl", "");
        String category = obj.optString("category", "");
        int quantity = obj.optInt("quantity", 1);
        String unit = obj.optString("unit", "pcs");
        String memo = obj.optString("memo", "");
        String deviceId = obj.optString("deviceId", "None");
        return new GroceryItem(id, name, isOwned, expiryDate, imageUrl, category, quantity, unit, memo, deviceId);
    }
}
