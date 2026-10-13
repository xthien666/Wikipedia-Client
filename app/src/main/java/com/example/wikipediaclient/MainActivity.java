package com.example.wikipediaclient;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.wikipediaclient.databinding.ActivityMainBinding;

public final class MainActivity extends AppCompatActivity {
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private Insets systemInsets = Insets.NONE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat systemBars = new WindowInsetsControllerCompat(
                getWindow(), getWindow().getDecorView());
        systemBars.setAppearanceLightStatusBars(true);
        systemBars.setAppearanceLightNavigationBars(true);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            systemInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            applySystemInsets(binding);
            return windowInsets;
        });

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (host == null) throw new IllegalStateException("Navigation host is missing.");
        navController = host.getNavController();

        appBarConfiguration = new AppBarConfiguration.Builder(R.id.searchFragment,
                R.id.libraryFragment, R.id.historyFragment, R.id.settingsFragment).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            boolean isArticle = destination.getId() == R.id.articleFragment;
            binding.bottomNavigation.setVisibility(isArticle ? View.GONE : View.VISIBLE);
            applySystemInsets(binding);
            ViewCompat.requestApplyInsets(binding.getRoot());
        });

        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    private void applySystemInsets(ActivityMainBinding binding) {
        binding.toolbar.setPadding(0, systemInsets.top, 0, 0);
        // adjustResize handles IME. Apply cached system insets when navigation visibility changes,
        // since pre-30 ViewCompat may skip an unchanged inset dispatch.
        int bottom = binding.bottomNavigation.getVisibility() == View.GONE ? systemInsets.bottom : 0;
        binding.getRoot().setPadding(systemInsets.left, 0, systemInsets.right, bottom);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
