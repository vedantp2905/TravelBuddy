package com.example.finalapp;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable; // Import Serializable
import java.util.ArrayList;
import java.util.List;

public class Poll implements Parcelable, Serializable { // Implement Serializable
    private static final long serialVersionUID = 1L; // Add serialVersionUID for version control

    private int id;
    private String title;
    private List<String> options;
    private int creatorId;

    public Poll(int id, String title, List<String> options, int creatorId) {
        this.id = id;
        this.title = title;
        this.options = options;
        this.creatorId = creatorId;
    }

    // Parcelable implementation
    protected Poll(Parcel in) {
        id = in.readInt();
        title = in.readString();
        options = in.createStringArrayList(); // Use createStringArrayList for List<String>
        creatorId = in.readInt(); // Read creatorId from Parcel
    }

    public static final Creator<Poll> CREATOR = new Creator<Poll>() {
        @Override
        public Poll createFromParcel(Parcel in) {
            return new Poll(in);
        }

        @Override
        public Poll[] newArray(int size) {
            return new Poll[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeStringList(options); // Use writeStringList for List<String>
        dest.writeInt(creatorId); // Write creatorId to Parcel
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCreatorId() {
        return creatorId;
    }

    // Method to create a Poll from a JSON object
    public static Poll fromJson(JSONObject jsonObject) {
        try {
            int id = jsonObject.getInt("id");
            String title = jsonObject.getString("title");

            // Correctly parse the options
            JSONArray optionsArray = jsonObject.getJSONArray("options");
            List<String> options = new ArrayList<>();
            for (int i = 0; i < optionsArray.length(); i++) {
                options.add(optionsArray.getString(i));
            }

            int creatorId = jsonObject.getInt("creatorId");
            return new Poll(id, title, options, creatorId);
        } catch (JSONException e) {
            Log.e("Poll", "JSON Exception while parsing Poll: " + e.getMessage());
            return null;
        }
    }
}
