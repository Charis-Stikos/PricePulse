package com.pricepulse.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pricepulse.repository.FirebaseRepository;

public class SearchViewModel extends ViewModel {

    private static final long DEBOUNCE_MS = 400L;

    private final FirebaseRepository repository = new FirebaseRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<SearchUiState> uiState = new MutableLiveData<>(new SearchUiState.Idle());

    private Runnable pendingSearch;
    private long lastQueryToken;

    public LiveData<String> getQuery() {
        return query;
    }

    public LiveData<SearchUiState> getUiState() {
        return uiState;
    }

    public void onQueryChanged(String newQuery) {
        query.setValue(newQuery);
        if (newQuery.isEmpty()) {
            cancelPending();
            uiState.setValue(new SearchUiState.Idle());
            return;
        }
        scheduleSearch(newQuery);
    }

    private void scheduleSearch(String q) {
        cancelPending();
        if (q.length() <= 2) {
            uiState.setValue(new SearchUiState.Idle());
            return;
        }
        final long token = ++lastQueryToken;
        pendingSearch = () -> {
            if (token != lastQueryToken) return;
            uiState.setValue(new SearchUiState.Loading());
            repository.searchProducts(q, results -> {
                if (token != lastQueryToken) return;
                if (results.isEmpty()) {
                    uiState.setValue(new SearchUiState.Empty());
                } else {
                    uiState.setValue(new SearchUiState.Success(results));
                }
            });
        };
        handler.postDelayed(pendingSearch, DEBOUNCE_MS);
    }

    private void cancelPending() {
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
    }

    @Override
    protected void onCleared() {
        cancelPending();
        super.onCleared();
    }
}
