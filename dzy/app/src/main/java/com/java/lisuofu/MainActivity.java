package com.java.lisuofu;

//用于fragment之间传递数据
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

//底部导航栏
import com.google.android.material.bottomnavigation.BottomNavigationView;

//3个fragment
import com.java.lisuofu.ui.fragment.HomeFragment;
import com.java.lisuofu.ui.fragment.FavoriteFragment;
import com.java.lisuofu.ui.fragment.HistoryFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    // Fragment实例
    private HomeFragment homeFragment;
    private FavoriteFragment favoriteFragment;
    private HistoryFragment historyFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupBottomNavigation();

        // 默认显示首页
        if (savedInstanceState == null) {
            showFragment(getHomeFragment());
        }
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(getHomeFragment());
                return true;
            } else if (itemId == R.id.nav_favorite) {
                showFragment(getFavoriteFragment());
                return true;
            } else if (itemId == R.id.nav_history) {
                showFragment(getHistoryFragment());
                return true;
            }

            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 隐藏所有Fragment
        hideAllFragments(transaction);

        // 如果Fragment还没有添加，则添加它
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment);
        } else {
            transaction.show(fragment);
        }

        transaction.commit();
    }

    private void hideAllFragments(FragmentTransaction transaction) {
        if (homeFragment != null && homeFragment.isAdded()) {
            transaction.hide(homeFragment);
        }
        if (favoriteFragment != null && favoriteFragment.isAdded()) {
            transaction.hide(favoriteFragment);
        }
        if (historyFragment != null && historyFragment.isAdded()) {
            transaction.hide(historyFragment);
        }
    }

    // Fragment单例获取方法
    private HomeFragment getHomeFragment() {
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        return homeFragment;
    }

    private FavoriteFragment getFavoriteFragment() {
        if (favoriteFragment == null) {
            favoriteFragment = new FavoriteFragment();
        }
        return favoriteFragment;
    }

    private HistoryFragment getHistoryFragment() {
        if (historyFragment == null) {
            historyFragment = new HistoryFragment();
        }
        return historyFragment;
    }
}
