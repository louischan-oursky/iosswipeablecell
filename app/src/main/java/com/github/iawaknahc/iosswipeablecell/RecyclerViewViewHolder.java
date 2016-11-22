package com.github.iawaknahc.iosswipeablecell;


import android.support.v7.widget.RecyclerView;

public class RecyclerViewViewHolder extends RecyclerView.ViewHolder {

    private ISCCellView<RecyclerViewCell> mCell;

    public RecyclerViewViewHolder(ISCCellView<RecyclerViewCell> itemView) {
        super(itemView);
        this.mCell = itemView;
    }

    public void bindView(String content) {
        this.mCell.getContentView().bindContent(content);
    }

}
