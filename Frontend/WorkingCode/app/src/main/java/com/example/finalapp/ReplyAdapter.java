package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private List<Reply> replyList;

    public ReplyAdapter(List<Reply> replyList) {
        this.replyList = replyList;
    }

    @Override
    public ReplyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reply_item, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReplyViewHolder holder, int position) {
        Reply reply = replyList.get(position);
        holder.replyMessage.setText(reply.getMessage());
        holder.replyId.setText(String.valueOf(reply.getId())); // Optionally, display the reply ID
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView replyMessage, replyId;

        public ReplyViewHolder(View itemView) {
            super(itemView);
            replyMessage = itemView.findViewById(R.id.replyMessage);
            replyId = itemView.findViewById(R.id.replyId);
        }
    }
}

