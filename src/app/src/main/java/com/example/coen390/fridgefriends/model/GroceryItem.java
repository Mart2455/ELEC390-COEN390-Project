package com.example.coen390.fridgefriends.model;

import org.json.JSONException;
import org.json.JSONObject;

public class GroceryItem {
    private String name;
    private boolean isOwned;
    private String expiryDate;
    private boolean isStriked;
    private boolean isEmptyRow;
    private boolean isSelected;
    private String imageUrl;
    private String category;
    private int quantity = 1;
    private String unit = "pcs";
    private String memo;

    public GroceryItem() {
        this.isEmptyRow = true;
        this.isSelected = false;
    }

    public GroceryItem(String name, boolean isOwned, String expiryDate) {
        this.name = name;
        this.isOwned = isOwned;
        this.expiryDate = expiryDate;
        this.isEmptyRow = false;
        this.isStriked = false;
        this.quantity = 1;
        this.unit = "pcs";
    }

    public GroceryItem(String name, boolean isOwned, String expiryDate, String imageUrl, String category, int quantity, String unit, String memo) {
        this.name = name;
        this.isOwned = isOwned;
        this.expiryDate = expiryDate;
        this.imageUrl = imageUrl;
        this.category = category;
        this.quantity = quantity > 0 ? quantity : 1;
        this.unit = (unit != null && !unit.isEmpty()) ? unit : "pcs";
        this.memo = memo;
        this.isEmptyRow = false;
        this.isStriked = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.isEmptyRow = false; }

    public boolean isOwned() { return isOwned; }
    public void setOwned(boolean owned) { isOwned = owned; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public boolean isEmptyRow() { return isEmptyRow; }
    public void setEmptyRow(boolean emptyRow) { isEmptyRow = emptyRow; }

    public boolean isStriked() { return isStriked; }
    public void setStriked(boolean striked) { isStriked = striked; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("isOwned", isOwned);
        obj.put("expiryDate", expiryDate == null ? "" : expiryDate);
        obj.put("imageUrl", imageUrl == null ? "" : imageUrl);
        obj.put("category", category == null ? "" : category);
        obj.put("quantity", quantity);
        obj.put("unit", unit == null ? "pcs" : unit);
        obj.put("memo", memo == null ? "" : memo);
        return obj;
    }

    public static GroceryItem fromJSON(JSONObject obj) throws JSONException {
        String name = obj.getString("name");
        boolean isOwned = obj.getBoolean("isOwned");
        String expiryDate = obj.optString("expiryDate", "");
        String imageUrl = obj.optString("imageUrl", "");
        String category = obj.optString("category", "");
        int quantity = obj.optInt("quantity", 1);
        String unit = obj.optString("unit", "pcs");
        String memo = obj.optString("memo", "");
        return new GroceryItem(name, isOwned, expiryDate, imageUrl, category, quantity, unit, memo);
    }
}
