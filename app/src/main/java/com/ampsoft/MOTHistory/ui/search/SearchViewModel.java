package com.ampsoft.MOTHistory.ui.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.data.repository.MotRepository;
import com.ampsoft.MOTHistory.data.repository.RepositoryResult;
import com.ampsoft.MOTHistory.util.RegistrationValidator;

public class SearchViewModel extends ViewModel {

    private final MotRepository motRepository;
    private final MediatorLiveData<RepositoryResult<Vehicle>> lookupResult = new MediatorLiveData<>();

    public SearchViewModel(MotRepository motRepository) {
        this.motRepository = motRepository;
    }

    public LiveData<RepositoryResult<Vehicle>> getLookupResult() {
        return lookupResult;
    }

    public void clearLookupResult() {
        lookupResult.setValue(null);
    }

    public void search(String registrationInput) {
        String normalized = RegistrationValidator.normalize(registrationInput);
        if (!RegistrationValidator.isValid(normalized)) {
            lookupResult.setValue(RepositoryResult.error("Enter a valid UK registration.", 0));
            return;
        }

        LiveData<RepositoryResult<Vehicle>> source = motRepository.lookupByRegistration(normalized);
        Observer<RepositoryResult<Vehicle>> observer = new Observer<RepositoryResult<Vehicle>>() {
            @Override
            public void onChanged(RepositoryResult<Vehicle> result) {
                lookupResult.setValue(result);
                if (result.getStatus() != RepositoryResult.Status.LOADING) {
                    lookupResult.removeSource(source);
                }
            }
        };
        lookupResult.addSource(source, observer);
    }
}
