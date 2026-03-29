package com.example.waterintaketracker;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.List;

public class HistoryAdapter extends BaseAdapter {
    private final HistoryActivity activity;
    private final List<HistoryEntry> historyList;
    private final LayoutInflater inflater;

    public HistoryAdapter(HistoryActivity activity, List<HistoryEntry> historyList) {
        this.activity = activity;
        this.historyList = historyList;
        this.inflater = LayoutInflater.from(activity);
    }

    @Override
    public int getCount() {
        return this.historyList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.historyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.item_history, parent, false);
        }
        
        HistoryEntry entry = this.historyList.get(position);
        
        TextView dateText = convertView.findViewById(R.id.item_date);
        TextView intakeText = convertView.findViewById(R.id.item_intake);
        TextView goalText = convertView.findViewById(R.id.item_goal);
        ProgressBar progressBar = convertView.findViewById(R.id.item_progress);

        dateText.setText(entry.getFormattedDate());
        intakeText.setText(this.activity.getString(R.string.format_ml, entry.getIntake()));
        goalText.setText(this.activity.getString(R.string.format_goal, entry.getGoal()));
        progressBar.setProgress(entry.getPercentage());

        int color;
        if (entry.isGoalMet()) {
            color = ContextCompat.getColor(this.activity, android.R.color.holo_green_dark);
        } else {
            color = ContextCompat.getColor(this.activity, android.R.color.holo_blue_dark);
        }
        progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        return convertView;
    }
}
