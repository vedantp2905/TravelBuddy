package com.example.finalapp;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;


public class PollOption implements Parcelable {
    private String optionId;
    private String optionName;
    private int voteCount;

    public PollOption(String optionId, String optionName, int voteCount) {
        this.optionId = optionId;
        this.optionName = optionName;
        this.voteCount = voteCount;
    }

    // Parcelable implementation
    protected PollOption(Parcel in) {
        optionId = in.readString();
        optionName = in.readString();
        voteCount = in.readInt();
    }

    public static final Creator<PollOption> CREATOR = new Creator<PollOption>() {
        @Override
        public PollOption createFromParcel(Parcel in) {
            return new PollOption(in);
        }

        @Override
        public PollOption[] newArray(int size) {
            return new PollOption[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(optionId);
        dest.writeString(optionName);
        dest.writeInt(voteCount);
    }

    // Getters
    public String getOptionId() {
        return optionId;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getVoteCount() {
        return voteCount;
    }

    // Method to create a PollOption from a JSON object
    public static PollOption fromJson(JSONObject json) {
        try {
            String optionId = json.getString("id");
            String optionName = json.getString("name");
            int voteCount = json.getInt("voteCount");

            return new PollOption(optionId, optionName, voteCount);
        } catch (JSONException e) {
            e.printStackTrace();
            return null; // Handle this appropriately in your app
        }
    }
}
