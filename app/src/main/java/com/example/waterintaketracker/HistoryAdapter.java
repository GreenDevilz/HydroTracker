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

/* JADX INFO: loaded from: classes3.dex */
public class HistoryAdapter extends BaseAdapter {
    private final HistoryActivity activity;
    private final List<HistoryEntry> historyList;
    private final LayoutInflater inflater;

    public HistoryAdapter(HistoryActivity activity, List<HistoryEntry> historyList) {
        this.activity = activity;
        this.historyList = historyList;
        this.inflater = LayoutInflater.from(activity);
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.historyList.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int position) {
        return this.historyList.get(position);
    }

    @Override // android.widget.Adapter
    public long getItemId(int position) {
        return position;
    }

    @Override // android.widget.Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        int color;
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.item_history, parent, false);
        }
        HistoryEntry entry = this.historyList.get(position);
        TextView dateText = (TextView) convertView.findViewById(R.id.item_date);
        TextView intakeText = (TextView) convertView.findViewById(R.id.item_intake);
        TextView goalText = (TextView) convertView.findViewById(R.id.item_goal);
        ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.item_progress);
        dateText.setText(entry.getFormattedDate());
        intakeText.setText(this.activity.getString(R.string.format_ml, new Object[]{Integer.valueOf(entry.getIntake())}));
        goalText.setText(this.activity.getString(R.string.format_goal, new Object[]{Integer.valueOf(entry.getGoal())}));
        progressBar.setProgress(entry.getPercentage());
        boolean zIsGoalMet = entry.isGoalMet();
        HistoryActivity historyActivity = this.activity;
        if (zIsGoalMet) {
            color = ContextCompat.getColor(historyActivity, android.R.color.holo_green_dark);
        } else {
            color = ContextCompat.getColor(historyActivity, android.R.color.holo_blue_dark);
        }
        progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return convertView;
    }
}
