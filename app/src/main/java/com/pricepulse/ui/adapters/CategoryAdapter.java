package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemCategoryBinding;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface OnSelected {
        void onSelected(String category);
    }

    private final List<String> categories;
    private final OnSelected onSelected;
    private int selectedIndex = 0;

    public CategoryAdapter(List<String> categories, OnSelected onSelected) {
        this.categories = categories;
        this.onSelected = onSelected;
    }

    public void setSelected(String category) {
        int newIndex = categories.indexOf(category);
        if (newIndex < 0 || newIndex == selectedIndex) return;
        int old = selectedIndex;
        selectedIndex = newIndex;
        notifyItemChanged(old);
        notifyItemChanged(newIndex);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding b = ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String category = categories.get(position);
        boolean isSelected = position == selectedIndex;
        holder.b.getRoot().setSelected(isSelected);
        holder.b.categoryText.setText(category);
        holder.b.categoryText.setSelected(isSelected);
        holder.b.getRoot().setOnClickListener(v -> onSelected.onSelected(category));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemCategoryBinding b;

        VH(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }
}
