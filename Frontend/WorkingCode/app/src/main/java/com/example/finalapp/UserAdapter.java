package com.example.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class UserAdapter extends ArrayAdapter<User> {
    private Context context;
    private ArrayList<User> users;

    public UserAdapter(Context context, ArrayList<User> users) {
        super(context, R.layout.user_item_layout, users);
        this.context = context;
        this.users = users;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.user_item_layout, parent, false);
            holder = new ViewHolder();
            holder.newsletterCheckBox = convertView.findViewById(R.id.newsletterCheckBox); // Assuming there's a CheckBox in your layout
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User user = users.get(position);
        holder.newsletterCheckBox.setChecked(user.isNewsletterSubscribed()); // This should match your User class method

        return convertView;
    }

    static class ViewHolder {
        CheckBox newsletterCheckBox; // Adjust this based on your layout
    }
}
