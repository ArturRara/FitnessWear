package com.example.fitnesswear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;




public class SpeedPickerActivity extends Activity implements SpeedPickerRecyclerAdapter.Callbacks {

    public static final String EXTRA_NEW_SPEED_LIMIT =
            "com.example.android.wearable.speedtracker.extra.NEW_SPEED_LIMIT";

    /* Speeds, in mph, that will be shown on the list */
    private int[] speeds = {25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speed_picker_activity);

        final TextView header = findViewById(R.id.header);

        // Get the list component from the layout of the activity
        WearableRecyclerView recyclerView = findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        recyclerView.setEdgeItemsCenteringEnabled(true);

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Assign an adapter to the list
        recyclerView.setAdapter(new SpeedPickerRecyclerAdapter(speeds, this));
    }

    @Override
    public void speedLimitChange(int speedChange) {
        Intent resultIntent = new Intent(Intent.ACTION_PICK);
        resultIntent.putExtra(EXTRA_NEW_SPEED_LIMIT, speedChange);
        setResult(RESULT_OK, resultIntent);

        finish();
    }

/*

    @Override
    public void onClick(WearableRecyclerView.ViewHolder viewHolder) {
        int newSpeedLimit = speeds[viewHolder.getLayoutPosition()];
        Intent resultIntent = new Intent(Intent.ACTION_PICK);
        resultIntent.putExtra(EXTRA_NEW_SPEED_LIMIT, newSpeedLimit);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onTopEmptyRegionClick() {
    }
*/

}
