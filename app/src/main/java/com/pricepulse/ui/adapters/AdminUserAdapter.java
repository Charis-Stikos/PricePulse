package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemAdminUserBinding;
import com.pricepulse.model.User;

public class AdminUserAdapter extends ListAdapter<User, AdminUserAdapter.VH> {

    public interface OnRevoke {
        void onRevoke(User user);
    }

    private final OnRevoke listener;
    private final String currentUserUid;

    public AdminUserAdapter(String currentUserUid, OnRevoke listener) {
        super(DIFF);
        this.currentUserUid = currentUserUid;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<User> DIFF = new DiffUtil.ItemCallback<User>() {
        @Override public boolean areItemsTheSame(@NonNull User a, @NonNull User b) {
            return a.getUid().equals(b.getUid());
        }
        @Override public boolean areContentsTheSame(@NonNull User a, @NonNull User b) {
            return a.getEmail().equals(b.getEmail())
                    && a.getDisplayName().equals(b.getDisplayName())
                    && a.isAdmin() == b.isAdmin();
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemAdminUserBinding b;

        VH(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(User user) {
            String label = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                    ? user.getDisplayName()
                    : (user.getEmail() != null ? user.getEmail() : "User");
            b.adminUserName.setText(label);
            b.adminUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            b.adminUserInitial.setText(label.substring(0, 1).toUpperCase());

            boolean isSelf = currentUserUid != null && currentUserUid.equals(user.getUid());
            b.adminUserRevoke.setEnabled(!isSelf);
            b.adminUserRevoke.setAlpha(isSelf ? 0.4f : 1f);
            b.adminUserRevoke.setOnClickListener(v -> {
                if (!isSelf) listener.onRevoke(user);
            });
        }
    }
}
