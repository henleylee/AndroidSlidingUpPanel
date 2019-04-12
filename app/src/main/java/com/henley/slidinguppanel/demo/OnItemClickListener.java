package com.henley.slidinguppanel.demo;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * RecycleView的Item点击事件
 *
 * @author Henley
 * @date 2017/7/21 14:25
 */
public interface OnItemClickListener {

    void onItemClick(RecyclerView.Adapter adapter, View itemView, int position);

}
