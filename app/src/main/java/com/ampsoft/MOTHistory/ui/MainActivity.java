package com.ampsoft.MOTHistory.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.ads.AdsManager;
import com.ampsoft.MOTHistory.billing.BillingManager;
import com.ampsoft.MOTHistory.data.local.VehicleStore;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.reminders.MotReminderScheduler;
import com.ampsoft.MOTHistory.util.ThemePreferences;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_RESULT_REGISTRATION = "open_result_registration";

    private AppBarConfiguration appBarConfiguration;
    private boolean suppressBottomNavCallbacks;
    private MaterialToolbar toolbar;
    private Drawable whiteUpArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferences.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.top_app_bar);
        whiteUpArrow = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_white_24);
        AppBarLayout topAppBarContainer = findViewById(R.id.top_app_bar_container);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setSupportActionBar(toolbar);
        applySystemBarBranding(topAppBarContainer, toolbar, bottomNavigationView);
        BillingManager.getInstance().initialize(this);
        AdsManager.getInstance().initialize(this);
        MotReminderScheduler.ensureNotificationChannel(this);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.searchFragment,
                R.id.savedCarsFragment,
                R.id.settingsFragment
        ).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        updateNavigationIcon(navController.getCurrentDestination() != null
                ? navController.getCurrentDestination().getId()
                : R.id.searchFragment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (suppressBottomNavCallbacks) {
                return true;
            }
            return navigateToRootDestination(navController, item.getItemId());
        });
        bottomNavigationView.setOnItemReselectedListener(item -> {
            if (suppressBottomNavCallbacks) {
                return;
            }
            navigateToRootDestination(navController, item.getItemId());
        });
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            int targetMenuItemId;
            if (destinationId == R.id.searchFragment
                    || destinationId == R.id.resultFragment
                    || destinationId == R.id.motHistoryFragment
                    || destinationId == R.id.mileageFragment) {
                targetMenuItemId = R.id.searchFragment;
            } else if (destinationId == R.id.savedCarsFragment) {
                targetMenuItemId = R.id.savedCarsFragment;
            } else if (destinationId == R.id.settingsFragment) {
                targetMenuItemId = R.id.settingsFragment;
            } else {
                return;
            }

            if (bottomNavigationView.getSelectedItemId() != targetMenuItemId) {
                suppressBottomNavCallbacks = true;
                bottomNavigationView.setSelectedItemId(targetMenuItemId);
                suppressBottomNavCallbacks = false;
            }
            updateNavigationIcon(destinationId);
        });
        handleLaunchIntent(navController);
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }
        handleLaunchIntent(navHostFragment.getNavController());
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return super.onSupportNavigateUp();
        }
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private boolean navigateToRootDestination(NavController navController, int itemId) {
        if (itemId == R.id.searchFragment) {
            navController.popBackStack(R.id.searchFragment, false);
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.searchFragment) {
                navController.navigate(R.id.searchFragment);
            }
            return true;
        }
        if (itemId == R.id.savedCarsFragment) {
            navController.popBackStack(R.id.savedCarsFragment, false);
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.savedCarsFragment) {
                navController.navigate(R.id.savedCarsFragment);
            }
            return true;
        }
        if (itemId == R.id.settingsFragment) {
            navController.popBackStack(R.id.settingsFragment, false);
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.settingsFragment) {
                navController.navigate(R.id.settingsFragment);
            }
            return true;
        }
        return false;
    }

    private void applySystemBarBranding(
            AppBarLayout topAppBarContainer,
            MaterialToolbar toolbar,
            BottomNavigationView bottomNavigationView
    ) {
        final int toolbarBaseHeight = toolbar.getLayoutParams().height;
        final int toolbarBasePaddingStart = toolbar.getPaddingStart();
        final int toolbarBasePaddingTop = toolbar.getPaddingTop();
        final int toolbarBasePaddingEnd = toolbar.getPaddingEnd();
        final int toolbarBasePaddingBottom = toolbar.getPaddingBottom();
        final int bottomNavBasePaddingLeft = bottomNavigationView.getPaddingLeft();
        final int bottomNavBasePaddingTop = bottomNavigationView.getPaddingTop();
        final int bottomNavBasePaddingRight = bottomNavigationView.getPaddingRight();
        final int bottomNavBasePaddingBottom = bottomNavigationView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(topAppBarContainer, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams toolbarLayoutParams = toolbar.getLayoutParams();
            toolbarLayoutParams.height = toolbarBaseHeight + systemBars.top;
            toolbar.setLayoutParams(toolbarLayoutParams);
            toolbar.setPaddingRelative(
                    toolbarBasePaddingStart,
                    toolbarBasePaddingTop + systemBars.top,
                    toolbarBasePaddingEnd,
                    toolbarBasePaddingBottom
            );

            bottomNavigationView.setPadding(
                    bottomNavBasePaddingLeft,
                    bottomNavBasePaddingTop,
                    bottomNavBasePaddingRight,
                    bottomNavBasePaddingBottom + systemBars.bottom
            );

            return insets;
        });
        ViewCompat.requestApplyInsets(topAppBarContainer);
    }

    private void updateNavigationIcon(int destinationId) {
        if (toolbar == null) {
            return;
        }

        boolean isTopLevel = destinationId == R.id.searchFragment
                || destinationId == R.id.savedCarsFragment
                || destinationId == R.id.settingsFragment;

        if (isTopLevel) {
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
            return;
        }

        toolbar.setNavigationIcon(whiteUpArrow);
        toolbar.setNavigationOnClickListener(v -> onSupportNavigateUp());
    }

    private void handleLaunchIntent(NavController navController) {
        String registration = getIntent() != null
                ? getIntent().getStringExtra(EXTRA_OPEN_RESULT_REGISTRATION)
                : null;
        if (registration == null || registration.trim().isEmpty()) {
            return;
        }
        Vehicle vehicle = VehicleStore.getSavedVehicleByRegistration(this, registration);
        if (vehicle == null) {
            clearLaunchIntentExtra();
            return;
        }
        Bundle args = new Bundle();
        args.putSerializable("vehicle", vehicle);
        navController.navigate(R.id.resultFragment, args);
        clearLaunchIntentExtra();
    }

    private void clearLaunchIntentExtra() {
        if (getIntent() != null) {
            getIntent().removeExtra(EXTRA_OPEN_RESULT_REGISTRATION);
        }
    }
}
