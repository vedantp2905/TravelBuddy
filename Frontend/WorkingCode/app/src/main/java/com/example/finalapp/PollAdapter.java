package com.example.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class PollAdapter extends BaseAdapter {

    private Context context;
    private List<Poll> polls;

    public PollAdapter(Context context, List<Poll> polls) {
        this.context = context;
        this.polls = polls;
    }

    @Override
    public int getCount() {
        return polls.size();
    }

    @Override
    public Object getItem(int position) {
        return polls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return polls.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.poll_item, parent, false);
        }

        // Find views in the layout
        TextView titleTextView = convertView.findViewById(R.id.pollTitleTextView);
        TextView option1TextView = convertView.findViewById(R.id.option1TextView);
        TextView option2TextView = convertView.findViewById(R.id.option2TextView);
        SeekBar option1SeekBar = convertView.findViewById(R.id.option1SeekBar);
        SeekBar option2SeekBar = convertView.findViewById(R.id.option2SeekBar);

        // Get the current poll
        Poll poll = polls.get(position);
        titleTextView.setText(poll.getTitle());

        // Set the options and SeekBars
        List<String> options = poll.getOptions();

        // Set option 1
        if (options.size() > 0) {
            option1TextView.setText(options.get(0)); // Display the option name
            // Set progress based on your logic (assuming you have some way to determine vote counts)
            option1SeekBar.setProgress(0); // Replace with actual vote count if available
        } else {
            option1TextView.setText("No first option available");
            option1SeekBar.setProgress(0); // Default progress
        }

        // Set option 2
        if (options.size() > 1) {
            option2TextView.setText(options.get(1)); // Display the option name
            // Set progress based on your logic (assuming you have some way to determine vote counts)
            option2SeekBar.setProgress(0); // Replace with actual vote count if available
        } else {
            option2TextView.setText("No second option available");
            option2SeekBar.setProgress(0); // Default progress
        }

        return convertView;
    }
}
