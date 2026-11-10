package com.example.coen390.fridgefriends.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coen390.fridgefriends.R;
import com.example.coen390.fridgefriends.model.GroceryItem;

import java.util.List;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.ViewHolder> {
    private List<GroceryItem> items;
    private boolean isOwnedTab;
    private boolean isSelectionMode = false;
    private OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onItemAdded(String name, int position, boolean focusNextLine);
        void onItemEdited(GroceryItem item, String newName, int position);
        void onItemMovedToOwned(GroceryItem item);
        void onSetExpiryDate(GroceryItem item, int position);
        void onItemLongClicked(GroceryItem item, int position);
    }

    public GroceryAdapter(List<GroceryItem> items, boolean isOwnedTab, OnItemInteractionListener listener) {
        this.items = items;
        this.isOwnedTab = isOwnedTab;
        this.listener = listener;
    }
    
    public void setSelectionMode(boolean mode) {
        this.isSelectionMode = mode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery_line, parent, false);
        return new ViewHolder(view);
    }

    private void clearFocusAndHideKeyboard(View v) {
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            v.clearFocus();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroceryItem item = items.get(position);

        if (holder.textWatcher != null) {
            holder.etGroceryInput.removeTextChangedListener(holder.textWatcher);
            holder.textWatcher = null;
        }

        if (item.isEmptyRow()) {
            holder.etGroceryInput.setVisibility(View.VISIBLE);
            holder.tvItemName.setVisibility(View.GONE);
            holder.tvExpiryDate.setVisibility(View.GONE);
            
            int firstEmptyIndex = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isEmptyRow()) {
                    firstEmptyIndex = i;
                    break;
                }
            }

            boolean isFirstEmpty = (position == firstEmptyIndex);

            if (!isFirstEmpty) {
                holder.etGroceryInput.setEnabled(false);
                holder.etGroceryInput.setFocusable(false);
                holder.etGroceryInput.setFocusableInTouchMode(false);
                holder.cbItem.setVisibility(View.INVISIBLE);
                holder.etGroceryInput.setText("");
                holder.etGroceryInput.setOnFocusChangeListener(null);
                holder.etGroceryInput.setOnEditorActionListener(null);
                holder.cbItem.setOnClickListener(null);
                holder.itemView.setOnClickListener(this::clearFocusAndHideKeyboard);
                return;
            }

            holder.etGroceryInput.setEnabled(true);
            holder.etGroceryInput.setFocusable(true);
            holder.etGroceryInput.setFocusableInTouchMode(true);
            holder.etGroceryInput.setTag(position);
            if (!holder.etGroceryInput.getText().toString().isEmpty()) {
                holder.etGroceryInput.setText("");
            }

            if (isOwnedTab) {
                holder.cbItem.setVisibility(View.GONE);
            } else {
                boolean hasFocus = holder.etGroceryInput.hasFocus();
                String currentText = holder.etGroceryInput.getText().toString().trim();
                holder.cbItem.setVisibility((hasFocus || !currentText.isEmpty()) ? View.VISIBLE : View.INVISIBLE);
                holder.cbItem.setChecked(false);
            }

            holder.textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!isOwnedTab) {
                        boolean hasFocus = holder.etGroceryInput.hasFocus();
                        boolean hasText = !s.toString().trim().isEmpty();
                        holder.cbItem.setVisibility((hasFocus || hasText) ? View.VISIBLE : View.INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };
            holder.etGroceryInput.addTextChangedListener(holder.textWatcher);
            
            holder.etGroceryInput.setOnFocusChangeListener((v, hasFocus) -> {
                holder.etGroceryInput.setCursorVisible(hasFocus);
                if (!isOwnedTab) {
                    String text = holder.etGroceryInput.getText().toString().trim();
                    holder.cbItem.setVisibility((hasFocus || !text.isEmpty()) ? View.VISIBLE : View.INVISIBLE);
                }
                if (!hasFocus && item.isEmptyRow()) {
                    String text = holder.etGroceryInput.getText().toString().trim();
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && !text.isEmpty()) {
                        listener.onItemAdded(text, pos, false);
                    }
                }
            });

            View.OnClickListener onFirstEmptyClickListener = v -> {
                Context ctx = v.getContext();
                if (ctx instanceof androidx.appcompat.app.AppCompatActivity) {
                    View currentFocus = ((androidx.appcompat.app.AppCompatActivity) ctx).getCurrentFocus();
                    if (currentFocus instanceof EditText && currentFocus != holder.etGroceryInput) {
                        String text = ((EditText) currentFocus).getText().toString().trim();
                        Object tag = currentFocus.getTag();
                        if (!text.isEmpty() && tag instanceof Integer) {
                            listener.onItemAdded(text, (Integer) tag, true);
                        }
                    }
                }
                holder.etGroceryInput.setEnabled(true);
                holder.etGroceryInput.setFocusable(true);
                holder.etGroceryInput.setFocusableInTouchMode(true);
                holder.etGroceryInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(holder.etGroceryInput, InputMethodManager.SHOW_IMPLICIT);
                }
            };

            holder.itemView.setOnClickListener(onFirstEmptyClickListener);
            holder.etGroceryInput.setOnClickListener(onFirstEmptyClickListener);

            holder.cbItem.setOnClickListener(v -> {
                String text = holder.etGroceryInput.getText().toString().trim();
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    if (!text.isEmpty()) {
                        listener.onItemAdded(text, pos, false);
                        item.setStriked(true);
                        notifyItemChanged(pos);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            listener.onItemMovedToOwned(item);
                        }, 300);
                    } else {
                        holder.etGroceryInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(holder.etGroceryInput, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });

            holder.etGroceryInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || 
                   (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String text = holder.etGroceryInput.getText().toString().trim();
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        if (!text.isEmpty()) {
                            listener.onItemAdded(text, pos, true);
                        } else {
                            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            holder.etGroceryInput.clearFocus();
                        }
                    }
                    return true;
                }
                return false;
            });
        } else {
            holder.etGroceryInput.setEnabled(true);
            holder.etGroceryInput.setFocusable(true);
            holder.etGroceryInput.setFocusableInTouchMode(true);
            holder.etGroceryInput.setVisibility(View.GONE);
            holder.tvItemName.setVisibility(View.VISIBLE);
            holder.tvItemName.setText(item.getName());
            
            holder.etGroceryInput.setOnFocusChangeListener(null);
            holder.etGroceryInput.setOnEditorActionListener(null);
            holder.cbItem.setOnCheckedChangeListener(null);
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbItem.setOnClickListener(null);
            holder.tvItemName.setOnClickListener(null);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);

            if (isSelectionMode) {
                holder.cbSelect.setVisibility(View.VISIBLE);
                holder.cbSelect.setChecked(item.isSelected());
                holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> item.setSelected(isChecked));
                
                View.OnClickListener selectionClickListener = v -> {
                    boolean newSelected = !item.isSelected();
                    item.setSelected(newSelected);
                    holder.cbSelect.setChecked(newSelected);
                };
                
                holder.itemView.setOnClickListener(selectionClickListener);
                holder.tvItemName.setOnClickListener(selectionClickListener);
                holder.tvExpiryDate.setOnClickListener(selectionClickListener);
                
                holder.cbItem.setVisibility(View.GONE);
                holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                
                if (isOwnedTab) {
                    if (item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()) {
                        holder.tvExpiryDate.setVisibility(View.VISIBLE);
                        holder.tvExpiryDate.setText(" - Expiry " + item.getExpiryDate());
                    } else {
                        holder.tvExpiryDate.setVisibility(View.VISIBLE);
                        holder.tvExpiryDate.setText(" - Add expiry date");
                    }
                } else {
                    holder.tvExpiryDate.setVisibility(View.GONE);
                }
            } else {
                holder.cbSelect.setVisibility(View.GONE);
                
                View.OnLongClickListener longClickListener = v -> {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemLongClicked(item, pos);
                    }
                    return true;
                };

                if (isOwnedTab) {
                    holder.cbItem.setVisibility(View.GONE);
                    holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    if (item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()) {
                        holder.tvExpiryDate.setVisibility(View.VISIBLE);
                        holder.tvExpiryDate.setText(" - Expiry " + item.getExpiryDate());
                    } else {
                        holder.tvExpiryDate.setVisibility(View.VISIBLE);
                        holder.tvExpiryDate.setText(" - Add expiry date");
                    }
                    
                    holder.itemView.setOnLongClickListener(longClickListener);
                    holder.tvItemName.setOnLongClickListener(longClickListener);
                    holder.tvExpiryDate.setOnLongClickListener(longClickListener);

                    View.OnClickListener dateClickListener = v -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            listener.onSetExpiryDate(item, pos);
                        }
                    };

                    holder.itemView.setOnClickListener(dateClickListener);
                    holder.tvItemName.setOnClickListener(dateClickListener);
                    holder.tvExpiryDate.setOnClickListener(dateClickListener);
                } else {
                    holder.cbItem.setVisibility(View.VISIBLE);
                    holder.cbItem.setChecked(item.isStriked());
                    holder.tvExpiryDate.setVisibility(View.GONE);
                    if (item.isStriked()) {
                        holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        holder.tvItemName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.medium_grey, null));
                    } else {
                        holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                        holder.tvItemName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.black, null));
                    }
                    
                    holder.itemView.setOnLongClickListener(longClickListener);
                    holder.tvItemName.setOnLongClickListener(longClickListener);
                    
                    View.OnClickListener strikethroughListener = v -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;

                        if (holder.etGroceryInput.getVisibility() == View.VISIBLE) {
                            String text = holder.etGroceryInput.getText().toString().trim();
                            if (!text.isEmpty()) {
                                item.setName(text);
                                listener.onItemEdited(item, text, pos);
                            }
                        }

                        if (!item.isStriked()) {
                            item.setStriked(true);
                            notifyItemChanged(pos);
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                listener.onItemMovedToOwned(item);
                            }, 300);
                        } else {
                            item.setStriked(false);
                            notifyItemChanged(pos);
                        }
                    };
                    
                    holder.cbItem.setOnClickListener(strikethroughListener);
                    
                    holder.itemView.setOnLongClickListener(v -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            listener.onItemLongClicked(item, pos);
                        }
                        return true;
                    });
                    
                    holder.tvItemName.setOnClickListener(v -> {
                        holder.etGroceryInput.setEnabled(true);
                        holder.etGroceryInput.setFocusable(true);
                        holder.etGroceryInput.setFocusableInTouchMode(true);
                        holder.etGroceryInput.setCursorVisible(true);
                        holder.etGroceryInput.setVisibility(View.VISIBLE);
                        holder.tvItemName.setVisibility(View.GONE);
                        holder.tvExpiryDate.setVisibility(View.GONE);
                        holder.etGroceryInput.setText(item.getName());
                        holder.etGroceryInput.requestFocus();
                        holder.etGroceryInput.setSelection(holder.etGroceryInput.getText().length());
                        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(holder.etGroceryInput, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });

                    holder.etGroceryInput.setOnFocusChangeListener((v, hasFocus) -> {
                        holder.etGroceryInput.setCursorVisible(hasFocus);
                        if (!hasFocus) {
                            String text = holder.etGroceryInput.getText().toString().trim();
                            int pos = holder.getBindingAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                if (!text.isEmpty()) {
                                    item.setName(text);
                                    listener.onItemEdited(item, text, pos);
                                }
                                holder.etGroceryInput.setVisibility(View.GONE);
                                holder.tvItemName.setVisibility(View.VISIBLE);
                                holder.tvItemName.setText(item.getName());
                            }
                        }
                    });

                    holder.etGroceryInput.setOnEditorActionListener((v, actionId, event) -> {
                        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || 
                           (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            String text = holder.etGroceryInput.getText().toString().trim();
                            int pos = holder.getBindingAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                if (!text.isEmpty()) {
                                    listener.onItemEdited(item, text, pos);
                                } else {
                                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                                    holder.etGroceryInput.clearFocus();
                                    holder.etGroceryInput.setVisibility(View.GONE);
                                    holder.tvItemName.setVisibility(View.VISIBLE);
                                }
                            }
                            return true;
                        }
                        return false;
                    });
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public EditText etGroceryInput;
        public LinearLayout llGroceryItem;
        public android.widget.CheckBox cbItem;
        public android.widget.CheckBox cbSelect;
        public TextView tvItemName;
        public TextView tvExpiryDate;
        public TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            etGroceryInput = itemView.findViewById(R.id.etGroceryInput);
            llGroceryItem = itemView.findViewById(R.id.llGroceryItem);
            cbItem = itemView.findViewById(R.id.cbItem);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
        }
    }
}
