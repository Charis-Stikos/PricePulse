package com.pricepulse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.model.Product;
import com.pricepulse.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final FirebaseRepository repository = new FirebaseRepository();

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(new HomeUiState.Loading());
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("All");

    private ListenerRegistration registration;

    public HomeViewModel() {
        listenToProducts();
    }

    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    public void onCategorySelected(String category) {
        selectedCategory.setValue(category);
        listenToProducts();
    }

    private void listenToProducts() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        uiState.setValue(new HomeUiState.Loading());
        String category = selectedCategory.getValue();
        if (category == null) category = "All";
        registration = repository.listenToProductsByCategory(category, 20, products ->
                uiState.setValue(new HomeUiState.Success(filterActive(products))));
    }

    private static List<Product> filterActive(List<Product> products) {
        if (products == null) return new ArrayList<>();
        List<Product> out = new ArrayList<>(products.size());
        for (Product p : products) {
            if (p.isShopActive()) out.add(p);
        }
        return out;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
