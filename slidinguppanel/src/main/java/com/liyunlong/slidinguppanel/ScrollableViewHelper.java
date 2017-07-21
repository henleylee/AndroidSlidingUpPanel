package com.liyunlong.slidinguppanel;

import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * 确定滚动视图当前的滚动位置辅助类(目前仅支持部分常用滚动视图，但是可以通过重写来添加对其他视图的支持)
 * <ul>
 * <strong>目前支持的滚动视图：</strong>
 * <li>{@link ScrollView}
 * <li>{@link NestedScrollView}
 * <li>{@link ListView}
 * <li>{@link RecyclerView}
 * </ul>
 *
 * @author liyunlong
 * @date 2017/7/20 10:42
 */
public class ScrollableViewHelper {
    /**
     * 滚动视图当前的滚动位置辅助类
     * <ul>
     * <strong>注意：</strong>
     * <li>如果返回值小于等于零，则表明滑动面板在处理滚动事件
     * <li>如果返回值大于零，则表明滑动面板让滚动视图处理滚动事件
     * </ul>
     *
     * @param scrollableView 滚动视图
     * @param isSlidingUp    滑动面板是否向上滑动为展开
     * @return 动视图当前的滚动位置
     */
    public int getScrollableViewScrollPosition(View scrollableView, boolean isSlidingUp) {
        if (scrollableView == null) {
            return 0;
        }
        if (scrollableView instanceof ScrollView) {
            if (isSlidingUp) {
                return scrollableView.getScrollY();
            } else {
                ScrollView scrollView = ((ScrollView) scrollableView);
                View child = scrollView.getChildAt(0);
                return (child.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
            }
        } else if (scrollableView instanceof NestedScrollView) {
            if (isSlidingUp) {
                return scrollableView.getScrollY();
            } else {
                NestedScrollView scrollView = ((NestedScrollView) scrollableView);
                View child = scrollView.getChildAt(0);
                return (child.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
            }
        } else if (scrollableView instanceof ListView && ((ListView) scrollableView).getChildCount() > 0) {
            ListView listView = ((ListView) scrollableView);
            if (listView.getAdapter() == null) return 0;
            if (isSlidingUp) {
                View firstChild = listView.getChildAt(0);
                // Approximate the scroll position based on the top child and the first visible item
                return listView.getFirstVisiblePosition() * firstChild.getHeight() - firstChild.getTop();
            } else {
                View lastChild = listView.getChildAt(listView.getChildCount() - 1);
                // Approximate the scroll position based on the bottom child and the last visible item
                return (listView.getAdapter().getCount() - listView.getLastVisiblePosition() - 1) * lastChild.getHeight() + lastChild.getBottom() - listView.getBottom();
            }
        } else if (scrollableView instanceof RecyclerView && ((RecyclerView) scrollableView).getChildCount() > 0) {
            RecyclerView recyclerView = ((RecyclerView) scrollableView);
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (recyclerView.getAdapter() == null) return 0;
            if (isSlidingUp) {
                View firstChild = recyclerView.getChildAt(0);
                // Approximate the scroll position based on the top child and the first visible item
                return recyclerView.getChildLayoutPosition(firstChild) * layoutManager.getDecoratedMeasuredHeight(firstChild) - layoutManager.getDecoratedTop(firstChild);
            } else {
                View lastChild = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
                // Approximate the scroll position based on the bottom child and the last visible item
                return (recyclerView.getAdapter().getItemCount() - 1) * layoutManager.getDecoratedMeasuredHeight(lastChild) + layoutManager.getDecoratedBottom(lastChild) - recyclerView.getBottom();
            }
        } else {
            return 0;
        }
    }
}
