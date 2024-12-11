package com.example.finalapp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.finalapp.R;
import com.example.finalapp.interfaces.MessageActionListener;
import com.example.finalapp.models.Message;
import com.example.finalapp.models.MessageReaction;
import android.view.Window;
import android.view.ViewGroup;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.view.WindowManager;
import android.view.Gravity;
import android.util.Log;

public class MessageActionDialog extends Dialog {
    private MessageActionListener listener;
    private Message message;
    private boolean isOwnMessage;

    public MessageActionDialog(Context context, Message message, boolean isOwnMessage, MessageActionListener listener) {
        super(context);
        this.message = message;
        this.listener = listener;
        this.isOwnMessage = isOwnMessage;
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_message_actions);
        
        LinearLayout reactionContainer = findViewById(R.id.reactionContainer);
        Button replyButton = findViewById(R.id.replyButton);
        Button deleteButton = findViewById(R.id.deleteButton);
        
        if (deleteButton != null) {
            Log.d("MessageActionDialog", "isOwnMessage: " + isOwnMessage);
            
            if (isOwnMessage) {
                deleteButton.setVisibility(View.VISIBLE);
                reactionContainer.setVisibility(View.GONE);
            } else {
                deleteButton.setVisibility(View.GONE);
                reactionContainer.setVisibility(View.VISIBLE);
            }
        }
        
        setupReactionButtons(reactionContainer);
        setupActionButtons(replyButton, deleteButton);
        
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }
    }

    private void setupReactionButtons(LinearLayout container) {
        for (MessageReaction.ReactionType type : MessageReaction.ReactionType.values()) {
            TextView reactionView = new TextView(getContext());
            reactionView.setText(getReactionEmoji(type.toString()));
            reactionView.setTextSize(24);
            reactionView.setPadding(24, 12, 24, 12);
            
            reactionView.setOnClickListener(v -> {
                listener.onReaction(message, type);
                dismiss();
            });
            
            container.addView(reactionView);
        }
    }

    private void setupActionButtons(Button replyButton, Button deleteButton) {
        replyButton.setOnClickListener(v -> {
            listener.onReply(message);
            dismiss();
        });
        
        if (isOwnMessage) {
            deleteButton.setOnClickListener(v -> {
                listener.onDelete(message);
                dismiss();
            });
        }
    }

    private String getReactionEmoji(String reactionType) {
        switch (reactionType) {
            case "THUMBS_UP": return "👍";
            case "THUMBS_DOWN": return "👎";
            case "HEART": return "❤️";
            case "EXCLAMATION": return "❗";
            case "LAUGH": return "😄";
            default: return "";
        }
    }
} 