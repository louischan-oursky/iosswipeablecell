package com.github.iawaknahc.iosswipeablecell;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewViewHolder> implements View.OnClickListener, ISCCellView.ISOCellViewDelegate<RecyclerViewCell> {

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

        // rightButtonFirst
        TextView archiveButton = new TextView(mRecyclerView.getContext());
        archiveButton.setPadding(10, 10, 10, 10);
        archiveButton.setGravity(Gravity.CENTER);
        archiveButton.setText("Archive");
        archiveButton.setTextColor(0xFF_FF_FF_FF);
        archiveButton.setBackgroundColor(0xFF_00_00_FF);

        // cellView
        ISCCellView<RecyclerViewCell> view = new ISCCellView<>(mRecyclerView, contentView, archiveButton);
        view.setDelegate(this);

        // holder
        RecyclerViewViewHolder holder = new RecyclerViewViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerViewViewHolder holder, int position) {
        String content = Integer.toString(mData.get(position));
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
    public void onWillSwipeFromRightToLeft(ISCCellView<RecyclerViewCell> cellView) {
        int adapterPosition = mRecyclerView.getChildAdapterPosition(cellView);
        Log.d(LOG_TAG, "onWillSwipeFromRightToLeft " + adapterPosition);
        if (adapterPosition != RecyclerView.NO_POSITION) {
            mData.remove(adapterPosition);
            this.notifyItemRemoved(adapterPosition);
        }
    }

    @Override
    public void onDidSwipeFromRightToLeft(ISCCellView<RecyclerViewCell> cellView) {

    }
}
