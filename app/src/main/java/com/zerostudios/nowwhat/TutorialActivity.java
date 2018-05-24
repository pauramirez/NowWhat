package com.zerostudios.nowwhat;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout linearLayout;

    private TextView[] mDots;
    private Button mBack, mNext;

    private SlideAdapter slideAdapter;

    private int mCurrentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        viewPager =  findViewById(R.id.slideView);
        linearLayout =  findViewById(R.id.dotsLayout);
        mBack = findViewById(R.id.back_btn);
        mNext = findViewById(R.id.next_btn);

        slideAdapter = new SlideAdapter(this);
        viewPager.setAdapter(slideAdapter);
        addDotsIndicator(0);

        viewPager.addOnPageChangeListener(onPageChangeListener);

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(mCurrentPage - 1);
            }
        });

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCurrentPage == mDots.length - 1)
                {
                    Intent intent = new Intent(TutorialActivity.this, MapsActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
                else
                {
                    viewPager.setCurrentItem(mCurrentPage + 1);
                }

            }
        });

    }

    public void addDotsIndicator(int position)
    {
        mDots = new TextView[3];
        linearLayout.removeAllViews();

        for(int i = 0 ; i < mDots.length ; i++)
        {
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226;"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.White));

            linearLayout.addView(mDots[i]);
        }

        if(mDots.length>0)
        {
            mDots[position].setTextColor(getResources().getColor(R.color.Red));
        }
    }

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDotsIndicator(position);
            mCurrentPage = position;

            if(mCurrentPage == 0)
            {
                mBack.setEnabled(false);
                mBack.setVisibility(View.INVISIBLE);
                mBack.setText("");

                mNext.setEnabled(true);
                mNext.setText("Next");
            }

            else if(position == mDots.length - 1)
            {
                mBack.setEnabled(true);
                mBack.setVisibility(View.VISIBLE);
                mBack.setText("Back");

                mNext.setEnabled(true);
                mNext.setText("Finish");
            }
            else
            {
                mBack.setEnabled(true);
                mBack.setVisibility(View.VISIBLE);
                mBack.setText("Back");

                mNext.setEnabled(true);
                mNext.setText("Next");
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

}
