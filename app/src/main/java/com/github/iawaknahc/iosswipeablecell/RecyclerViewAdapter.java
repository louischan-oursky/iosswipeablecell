package com.github.iawaknahc.iosswipeablecell;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewViewHolder> implements View.OnClickListener {

    private static final String LOG_TAG = "RecyclerViewAdapter";

    @Override
    public RecyclerViewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerViewCell cell = new RecyclerViewCell(parent.getContext());
//        cell.setOnClickListener(this);
        ISCCellView<RecyclerViewCell> view = new ISCCellView<>(cell, 200);
        RecyclerViewViewHolder holder = new RecyclerViewViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerViewViewHolder holder, int position) {
        holder.bindView(position);
    }

    @Override
    public int getItemCount() {
        return 100;
    }

    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "contentView onClick");
    }
}
