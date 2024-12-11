package com.example.finalapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.example.finalapp.models.MessageReaction;

public class ReactionDialog extends Dialog {
    private OnReactionSelectedListener listener;

    public interface OnReactionSelectedListener {
        void onReactionSelected(MessageReaction.ReactionType type);
    }

    public ReactionDialog(@NonNull Context context, OnReactionSelectedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_reactions);

        // Remove dialog background
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set up reaction clicks
        findViewById(R.id.thumbsUpReaction).setOnClickListener(v -> {
            listener.onReactionSelected(MessageReaction.ReactionType.THUMBS_UP);
            dismiss();
        });

        findViewById(R.id.thumbsDownReaction).setOnClickListener(v -> {
            listener.onReactionSelected(MessageReaction.ReactionType.THUMBS_DOWN);
            dismiss();
        });

        findViewById(R.id.heartReaction).setOnClickListener(v -> {
            listener.onReactionSelected(MessageReaction.ReactionType.HEART);
            dismiss();
        });

        findViewById(R.id.exclamationReaction).setOnClickListener(v -> {
            listener.onReactionSelected(MessageReaction.ReactionType.EXCLAMATION);
            dismiss();
        });

        findViewById(R.id.laughReaction).setOnClickListener(v -> {
            listener.onReactionSelected(MessageReaction.ReactionType.LAUGH);
            dismiss();
        });
    }
} 