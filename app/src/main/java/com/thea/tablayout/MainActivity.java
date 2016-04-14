
package com.thea.tablayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.thea.widget.tablayout.BottomTabLayout;
import com.thea.widget.tablayout.TabView;

public class MainActivity extends AppCompatActivity {
    private BottomTabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTabLayout = (BottomTabLayout) findViewById(R.id.btl);
        mTabLayout.addTab(new TabView.Tab(this).setIcon(R.drawable.tab_cup));
        mTabLayout.addTab(new TabView.Tab(this).setIcon(R.drawable.tab_cake));
        mTabLayout.addTab(new TabView.Tab(this).setIcon(R.drawable.tab_apple));
        mTabLayout.addTab(new TabView.Tab(this).setIcon(R.drawable.tab_hamburger));

    }
}
