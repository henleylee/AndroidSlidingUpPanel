package com.henley.slidinguppanel.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.henley.slidinguppanel.SlidingUpPanelLayout;
import com.henley.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.henley.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.util.ArrayList;
import java.util.List;

public class RecycleViewActivity extends AppCompatActivity implements OnItemClickListener {

    private static final String TAG = "RecycleViewActivity";
    private static final int LIST_ITEM_COUNT = 20;
    private SlidingUpPanelLayout mLayout;
    private Toast toast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycleview);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<String> arrayList = new ArrayList<>(LIST_ITEM_COUNT);
        for (int i = 0; i < LIST_ITEM_COUNT; i++) {
            arrayList.add("SlidingUpPanelLayout--->" + i);
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(arrayList);
        arrayAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(arrayAdapter);

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.addPanelSlideListener(new PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i(TAG, "onPanelSlide, offset " + slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, PanelState previousState, PanelState cunrentState) {
                Log.i(TAG, "onPanelStateChanged " + cunrentState);
            }
        });
        mLayout.setFadeOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(PanelState.COLLAPSED);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.action_toggle);
        if (mLayout != null) {
            if (mLayout.getPanelState() == PanelState.HIDDEN) {
                item.setTitle(R.string.action_show);
            } else {
                item.setTitle(R.string.action_hide);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle: {
                if (mLayout != null) {
                    if (mLayout.getPanelState() != PanelState.HIDDEN) {
                        mLayout.setPanelState(PanelState.HIDDEN);
                        item.setTitle(R.string.action_show);
                    } else {
                        mLayout.setPanelState(PanelState.COLLAPSED);
                        item.setTitle(R.string.action_hide);
                    }
                }
                return true;
            }
            case R.id.action_anchor: {
                if (mLayout != null) {
                    if (mLayout.getAnchorPoint() == 1.0f) {
                        mLayout.setAnchorPoint(0.7f);
                        mLayout.setPanelState(PanelState.ANCHORED);
                        item.setTitle(R.string.action_anchor_disable);
                    } else {
                        mLayout.setAnchorPoint(1.0f);
                        mLayout.setPanelState(PanelState.COLLAPSED);
                        item.setTitle(R.string.action_anchor_enable);
                    }
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == PanelState.EXPANDED || mLayout.getPanelState() == PanelState.ANCHORED)) {
            mLayout.setPanelState(PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemClick(RecyclerView.Adapter adapter, View itemView, int position) {
        showToast("onItemClick--->" + position);
    }

    private void showToast(CharSequence text) {
        if (toast == null) {
            toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            toast.setText(text);
        }
        toast.show();
    }

    private class ArrayAdapter extends RecyclerView.Adapter<ArrayAdapter.ArrayViewHolder> {

        private List<String> items;
        private OnItemClickListener mListener; // Item点击事件

        ArrayAdapter(List<String> items) {
            this.items = items;
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.mListener = listener;
        }

        @Override
        public ArrayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ArrayViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ArrayViewHolder holder, int position) {
            holder.textView.setText(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        class ArrayViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

            private final TextView textView;

            ArrayViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(android.R.id.text1);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(ArrayAdapter.this, v, getPosition());
                }
            }
        }
    }

}
