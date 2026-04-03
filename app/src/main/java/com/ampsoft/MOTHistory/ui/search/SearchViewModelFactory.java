package com.ampsoft.MOTHistory.ui.search;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ampsoft.MOTHistory.data.repository.MotRepository;

public class SearchViewModelFactory implements ViewModelProvider.Factory {

    private final MotRepository motRepository;

    public SearchViewModelFactory(MotRepository motRepository) {
        this.motRepository = motRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SearchViewModel.class)) {
            return (T) new SearchViewModel(motRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
