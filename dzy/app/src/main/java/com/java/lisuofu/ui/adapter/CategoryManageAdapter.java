package com.java.lisuofu.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.java.lisuofu.R;

import java.util.List;

public class CategoryManageAdapter extends RecyclerView.Adapter<CategoryManageAdapter.ViewHolder> {

    public static class CategoryItem {
        private String name;
        private boolean isSelected;
        private boolean isRequired; // 是否为必选项（如"全部"）

        public CategoryItem(String name, boolean isSelected, boolean isRequired) {
            this.name = name;
            this.isSelected = isSelected;
            this.isRequired = isRequired;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { isSelected = selected; }
        
        public boolean isRequired() { return isRequired; }
        public void setRequired(boolean required) { isRequired = required; }
    }

    private List<CategoryItem> categoryList;

    public CategoryManageAdapter(List<CategoryItem> categoryList) {
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryItem item = categoryList.get(position);
        
        holder.categoryName.setText(item.getName());
        holder.checkbox.setChecked(item.isSelected());
        
        // 如果是必选项，禁用复选框并显示提示
        if (item.isRequired()) {
            holder.checkbox.setEnabled(false);
            holder.categoryDescription.setVisibility(View.VISIBLE);
            holder.categoryDescription.setText("(必选)");
        } else {
            holder.checkbox.setEnabled(true);
            holder.categoryDescription.setVisibility(View.GONE);
        }
        
        // 设置复选框点击监听
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!item.isRequired()) {
                item.setSelected(isChecked);
            }
        });
        
        // 设置整个项目点击监听
        holder.itemView.setOnClickListener(v -> {
            if (!item.isRequired()) {
                boolean newState = !item.isSelected();
                item.setSelected(newState);
                holder.checkbox.setChecked(newState);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public List<CategoryItem> getCategoryList() {
        return categoryList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        TextView categoryName;
        TextView categoryDescription;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox_category);
            categoryName = itemView.findViewById(R.id.tv_category_name);
            categoryDescription = itemView.findViewById(R.id.tv_category_description);
        }
    }
}
