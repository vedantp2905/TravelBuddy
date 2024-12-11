package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks;
    private TaskListener listener;

    public interface TaskListener {
        void onTaskStatusChanged(Task task, boolean isCompleted);
        void onTaskDeleted(Task task);
        void onTaskClicked(Task task);
    }

    public TaskAdapter(List<Task> tasks, TaskListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvDueDate.setText(task.getDueDate());
        holder.cbTask.setChecked(task.isCompleted());
        holder.tvTaskDescription.setText(task.getDescription());

        holder.cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Only trigger if user changed it
                listener.onTaskStatusChanged(task, isChecked);
            }
        });

        holder.btnDeleteTask.setOnClickListener(v -> listener.onTaskDeleted(task));
        
        holder.itemView.setOnClickListener(v -> listener.onTaskClicked(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTask;
        TextView tvTaskTitle;
        TextView tvDueDate;
        ImageButton btnDeleteTask;
        TextView tvTaskDescription;

        TaskViewHolder(View itemView) {
            super(itemView);
            cbTask = itemView.findViewById(R.id.cbTask);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
        }
    }
}