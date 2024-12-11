package com.example.finalapp.utils;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int verticalSpaceHeight;

    public VerticalSpaceItemDecoration(int verticalSpaceHeight) {
        this.verticalSpaceHeight = verticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                             RecyclerView.State state) {
        outRect.bottom = verticalSpaceHeight;
        // Add top margin only for the first item
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = verticalSpaceHeight;
        }
    }
} 