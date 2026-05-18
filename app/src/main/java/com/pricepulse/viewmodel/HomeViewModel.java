package com.pricepulse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pricepulse.repository.FirebaseRepository;

public class HomeViewModel extends ViewModel {

    private final FirebaseRepository repository = new FirebaseRepository();

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(new HomeUiState.Loading());
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("All");

    public HomeViewModel() {
        fetchProducts();
    }

    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    public void onCategorySelected(String category) {
        selectedCategory.setValue(category);
        fetchProducts();
    }

    private void fetchProducts() {
        uiState.setValue(new HomeUiState.Loading());
        String category = selectedCategory.getValue();
        if (category == null) category = "All";
        repository.getProductsByCategory(category, 20, products ->
                uiState.setValue(new HomeUiState.Success(products)));
    }
}
