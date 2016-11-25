package com.github.iawaknahc.iosswipeablecell;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewViewHolder> implements View.OnClickListener, ISCCellView.ISCCellViewActionDelegate<RecyclerViewCell> {

    private static final String LOG_TAG = "RecyclerViewAdapter";

    protected RecyclerView mRecyclerView;
    protected ArrayList<Integer> mData;


    public RecyclerViewAdapter(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mData = new ArrayList<>();

        for (int i = 0; i < 1000; ++i) {
            mData.add(i);
        }
    }

    @Override
    public RecyclerViewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // contentView
        RecyclerViewCell contentView = new RecyclerViewCell(parent.getContext());
        contentView.setOnClickListener(this);



        // cellView
        ISCCellView<RecyclerViewCell> view = new ISCCellView<>(mRecyclerView, contentView);
        view.setActionDelegate(this);

        // holder
        RecyclerViewViewHolder holder = new RecyclerViewViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerViewViewHolder holder, int position) {
        String content = Integer.toString(mData.get(position));
        ArrayList<View> rightButtons = new ArrayList<>();

        // rightButtonFirst
        TextView archiveButton = new TextView(mRecyclerView.getContext());
        archiveButton.setPadding(10, 10, 10, 10);
        archiveButton.setGravity(Gravity.CENTER);
        archiveButton.setText("Archive");
        archiveButton.setTextColor(0xFF_FF_FF_FF);
        archiveButton.setBackgroundColor(0xFF_00_00_FF);
        rightButtons.add(archiveButton);

        TextView moreButton = new TextView(mRecyclerView.getContext());
        moreButton.setPadding(10, 10, 10, 10);
        moreButton.setGravity(Gravity.CENTER);
        moreButton.setText("More");
        moreButton.setTextColor(0xFF_FF_FF_FF);
        moreButton.setBackgroundColor(0xFF_00_FF_00);
        rightButtons.add(moreButton);

        holder.getCell().setRightButtons(rightButtons);
        holder.bindView(content);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "contentView onClick");
    }

    @Override
    public void onActionDone(ISCCellView<RecyclerViewCell> cellView, Bundle eventData) {
        Log.d(LOG_TAG, "eventData=" + eventData.toString());
        if (eventData.getInt("index", 0) == 0) {
            int adapterPosition = mRecyclerView.getChildAdapterPosition(cellView);
            if (adapterPosition != RecyclerView.NO_POSITION) {
                mData.remove(adapterPosition);
                this.notifyItemRemoved(adapterPosition);
            }
        }
    }
}
