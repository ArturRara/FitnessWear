package com.example.fitnesswear;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class SpeedPickerListAdapter extends WearableListView.Adapter {

    private int[] mDataSet;
    private final Context mContext;
    private final LayoutInflater mInflater;

    public SpeedPickerListAdapter(Context context, int[] dataset) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mDataSet = dataset;
    }

    public static class ItemViewHolder extends WearableListView.ViewHolder {

        private TextView mTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.name);
        }
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // Inflate our custom layout for list items
        return new ItemViewHolder(mInflater.inflate(R.layout.speed_picker_item_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder,
                                 int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        TextView view = itemHolder.mTextView;
        view.setText(mContext.getString(R.string.speed_for_list, mDataSet[position]));
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mDataSet.length;
    }

}
