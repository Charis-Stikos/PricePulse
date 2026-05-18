package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemFaqBinding;

import java.util.List;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.VH> {

    public static class Faq {
        public final String question;
        public final String answer;

        public Faq(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    private final List<Faq> faqs;

    public FaqAdapter(List<Faq> faqs) {
        this.faqs = faqs;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFaqBinding b = ItemFaqBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Faq faq = faqs.get(position);
        holder.b.faqQuestion.setText(faq.question);
        holder.b.faqAnswer.setText(faq.answer);
    }

    @Override
    public int getItemCount() {
        return faqs.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemFaqBinding b;

        VH(ItemFaqBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }
}
