package com.example.finalapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;
import android.widget.EditText;
import android.util.Log;
import com.android.volley.toolbox.StringRequest;
import android.view.View;
import android.widget.ImageView;

public class TaskListActivity extends AppCompatActivity {
    private TaskAdapter pendingTasksAdapter;
    private TaskAdapter completedTasksAdapter;
    private List<Task> pendingTasks;
    private List<Task> completedTasks;
    private RequestQueue requestQueue;
    private String userId;
    private boolean isPendingTasksExpanded = true;
    private boolean isCompletedTasksExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        requestQueue = Volley.newRequestQueue(this);
        userId = String.valueOf(getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getInt("userId", -1));

        setupRecyclerViews();
        setupClickListeners();
        fetchTasks();
    }

    private void setupRecyclerViews() {
        pendingTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();

        pendingTasksAdapter = new TaskAdapter(pendingTasks, new TaskAdapter.TaskListener() {
            @Override
            public void onTaskStatusChanged(Task task, boolean isCompleted) {
                updateTaskStatus(task, isCompleted);
            }

            @Override
            public void onTaskDeleted(Task task) {
                deleteTask(task);
            }

            @Override
            public void onTaskClicked(Task task) {
                showEditTaskDialog(task);
            }
        });

        completedTasksAdapter = new TaskAdapter(completedTasks, new TaskAdapter.TaskListener() {
            @Override
            public void onTaskStatusChanged(Task task, boolean isCompleted) {
                updateTaskStatus(task, isCompleted);
            }

            @Override
            public void onTaskDeleted(Task task) {
                deleteTask(task);
            }

            @Override
            public void onTaskClicked(Task task) {
                showEditTaskDialog(task);
            }
        });

        RecyclerView pendingTasksRecyclerView = findViewById(R.id.pendingTasksRecyclerView);
        pendingTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pendingTasksRecyclerView.setAdapter(pendingTasksAdapter);

        RecyclerView completedTasksRecyclerView = findViewById(R.id.completedTasksRecyclerView);
        completedTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completedTasksRecyclerView.setAdapter(completedTasksAdapter);
    }

    private void setupClickListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageButton btnAddTask = findViewById(R.id.btnAddTask);
        btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        View pendingTasksHeader = findViewById(R.id.pendingTasksHeader);
        View completedTasksHeader = findViewById(R.id.completedTasksHeader);
        ImageView pendingTasksArrow = findViewById(R.id.pendingTasksArrow);
        ImageView completedTasksArrow = findViewById(R.id.completedTasksArrow);
        RecyclerView pendingTasksRecyclerView = findViewById(R.id.pendingTasksRecyclerView);
        RecyclerView completedTasksRecyclerView = findViewById(R.id.completedTasksRecyclerView);

        pendingTasksHeader.setOnClickListener(v -> {
            isPendingTasksExpanded = !isPendingTasksExpanded;
            pendingTasksRecyclerView.setVisibility(isPendingTasksExpanded ? View.VISIBLE : View.GONE);
            pendingTasksArrow.setImageResource(isPendingTasksExpanded ? 
                R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        });

        completedTasksHeader.setOnClickListener(v -> {
            isCompletedTasksExpanded = !isCompletedTasksExpanded;
            completedTasksRecyclerView.setVisibility(isCompletedTasksExpanded ? View.VISIBLE : View.GONE);
            completedTasksArrow.setImageResource(isCompletedTasksExpanded ? 
                R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        });
    }

    private void fetchTasks() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/tasks/user/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    pendingTasks.clear();
                    completedTasks.clear();

                    try {
                        JSONArray tasksArray = response.getJSONArray("tasks");
                        Log.d("TaskList", "Tasks fetched: " + tasksArray.toString());

                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskJson = tasksArray.getJSONObject(i);
                            Task task = new Task(
                                    taskJson.getInt("id"),
                                    taskJson.getString("title"),
                                    taskJson.getString("description"),
                                    taskJson.getString("dueDate"),
                                    taskJson.getBoolean("completed")
                            );

                            if (task.isCompleted()) {
                                completedTasks.add(task);
                            } else {
                                pendingTasks.add(task);
                            }
                        }

                        // Sort tasks by due date
                        Collections.sort(pendingTasks, (t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()));
                        Collections.sort(completedTasks, (t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()));

                        pendingTasksAdapter.notifyDataSetChanged();
                        completedTasksAdapter.notifyDataSetChanged();

                        Log.d("TaskList", "Pending tasks: " + pendingTasks.size());
                        Log.d("TaskList", "Completed tasks: " + completedTasks.size());

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("TaskList", "Error parsing tasks: " + e.getMessage());
                        Toast.makeText(this, "Error parsing tasks", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMessage = "";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String errorStr = new String(error.networkResponse.data);
                            Log.e("TaskList", "Error response: " + errorStr);
                            errorMessage = errorStr;
                        } catch (Exception e) {
                            errorMessage = error.toString();
                        }
                    } else {
                        errorMessage = error.toString();
                    }
                    Log.e("TaskList", "Error fetching tasks: " + errorMessage);
                    Toast.makeText(this, "Error fetching tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void updateTaskStatus(Task task, boolean isCompleted) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/tasks/" + task.getId();

        try {
            // Create request body with updated completion status
            JSONObject requestBody = new JSONObject();
            requestBody.put("id", task.getId());
            requestBody.put("title", task.getTitle());
            requestBody.put("description", task.getDescription());
            requestBody.put("dueDate", task.getDueDate());
            requestBody.put("completed", isCompleted);
            requestBody.put("dayReminderSent", false);
            requestBody.put("hourReminderSent", false);
            requestBody.put("overdueReminderSent", false);
            
            JSONObject userObj = new JSONObject();
            userObj.put("id", Integer.parseInt(userId));
            requestBody.put("user", userObj);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, requestBody,
                    response -> {
                        // Update local lists
                        if (isCompleted) {
                            pendingTasks.remove(task);
                            task.setCompleted(true);
                            completedTasks.add(task);
                        } else {
                            completedTasks.remove(task);
                            task.setCompleted(false);
                            pendingTasks.add(task);
                        }

                        // Sort tasks by due date
                        Collections.sort(pendingTasks, (t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()));
                        Collections.sort(completedTasks, (t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()));

                        // Update both adapters
                        pendingTasksAdapter.notifyDataSetChanged();
                        completedTasksAdapter.notifyDataSetChanged();

                        Log.d("TaskList", "Task status updated successfully");
                    },
                    error -> {
                        // Revert the checkbox state on error
                        task.setCompleted(!isCompleted);
                        if (isCompleted) {
                            completedTasksAdapter.notifyDataSetChanged();
                        } else {
                            pendingTasksAdapter.notifyDataSetChanged();
                        }

                        String errorMessage = "";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorStr = new String(error.networkResponse.data);
                                Log.e("TaskList", "Error response: " + errorStr);
                                errorMessage = errorStr;
                            } catch (Exception e) {
                                errorMessage = error.toString();
                            }
                        } else {
                            errorMessage = error.toString();
                        }
                        Log.e("TaskList", "Error updating task status: " + errorMessage);
                        Toast.makeText(TaskListActivity.this, 
                            "Error updating task status: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });

            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing task update", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTask(Task task) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/tasks/" + task.getId();

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    // Remove the task from the list
                    if (task.isCompleted()) {
                        completedTasks.remove(task);
                        completedTasksAdapter.notifyDataSetChanged();
                    } else {
                        pendingTasks.remove(task);
                        pendingTasksAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errorMessage = "";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String errorStr = new String(error.networkResponse.data);
                            Log.e("TaskList", "Error response: " + errorStr);
                            errorMessage = errorStr;
                        } catch (Exception e) {
                            errorMessage = error.toString();
                        }
                    } else {
                        errorMessage = error.toString();
                    }
                    Log.e("TaskList", "Error deleting task: " + errorMessage);
                    Toast.makeText(this, "Error deleting task: " + errorMessage, Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void showAddTaskDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_task);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etTaskTitle = dialog.findViewById(R.id.etTaskTitle);
        EditText etTaskDescription = dialog.findViewById(R.id.etTaskDescription);
        Button btnSelectDate = dialog.findViewById(R.id.btnSelectDate);
        Button btnSelectTime = dialog.findViewById(R.id.btnSelectTime);
        Button btnSaveTask = dialog.findViewById(R.id.btnSaveTask);
        
        btnSelectDate.setText("Select Date");
        btnSelectTime.setText("Select Time");
        
        btnSelectDate.setOnClickListener(v -> {
            Log.d("TaskList", "Date button clicked");
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                TaskListActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%d-%02d-%02d", 
                        selectedYear, selectedMonth + 1, selectedDay);
                    btnSelectDate.setText(date);
                    Log.d("TaskList", "Date selected: " + date);
                },
                year, month, day);
            datePickerDialog.show();
        });

        btnSelectTime.setOnClickListener(v -> {
            Log.d("TaskList", "Time button clicked");
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                TaskListActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", 
                        selectedHour, selectedMinute);
                    btnSelectTime.setText(time);
                    Log.d("TaskList", "Time selected: " + time);
                },
                hour, minute, true);
            timePickerDialog.show();
        });

        btnSaveTask.setOnClickListener(v -> {
            Log.d("TaskList", "Save button clicked");
            String title = etTaskTitle.getText().toString().trim();
            String description = etTaskDescription.getText().toString().trim();
            String date = btnSelectDate.getText().toString();
            String time = btnSelectTime.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }

            if (date.equals("Select Date") || time.equals("Select Time")) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert date and time to ISO 8601 format
            String isoDateTime = String.format("%sT%s:00.000Z", date, time);
            Log.d("TaskList", "Creating task with due date: " + isoDateTime);

            // Create JSON object for the new task
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("title", title);
                requestBody.put("description", description);
                requestBody.put("dueDate", isoDateTime);
                requestBody.put("completed", false);
                requestBody.put("dayReminderSent", false);
                requestBody.put("hourReminderSent", false);
                requestBody.put("overdueReminderSent", false);
                
                JSONObject userObj = new JSONObject();
                userObj.put("id", Integer.parseInt(userId));
                requestBody.put("user", userObj);
                
                Log.d("TaskList", "Request body: " + requestBody.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Send POST request to create task
            String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/tasks";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                    response -> {
                        Log.d("TaskList", "Task created successfully: " + response.toString());
                        dialog.dismiss();
                        fetchTasks();
                        Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        String errorMessage = "";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorStr = new String(error.networkResponse.data);
                                Log.e("TaskList", "Error response: " + errorStr);
                                errorMessage = errorStr;
                            } catch (Exception e) {
                                errorMessage = error.toString();
                            }
                        } else {
                            errorMessage = error.toString();
                        }
                        Log.e("TaskList", "Error creating task: " + errorMessage);
                        Toast.makeText(this, "Error creating task: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });

            requestQueue.add(request);
        });

        dialog.show();
    }

    private void showEditTaskDialog(Task task) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_task);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etTaskTitle = dialog.findViewById(R.id.etTaskTitle);
        EditText etTaskDescription = dialog.findViewById(R.id.etTaskDescription);
        Button btnSelectDate = dialog.findViewById(R.id.btnSelectDate);
        Button btnSelectTime = dialog.findViewById(R.id.btnSelectTime);
        Button btnUpdateTask = dialog.findViewById(R.id.btnUpdateTask);

        // Set existing task data
        etTaskTitle.setText(task.getTitle());
        etTaskDescription.setText(task.getDescription());
        
        // Parse the ISO datetime string
        String currentDueDate = task.getDueDate();
        String initialDate = currentDueDate.substring(0, 10); // Extract YYYY-MM-DD
        String initialTime = currentDueDate.substring(11, 16); // Extract HH:mm
        
        btnSelectDate.setText(initialDate);
        btnSelectTime.setText(initialTime);

        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                TaskListActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", 
                        selectedYear, selectedMonth + 1, selectedDay);
                    btnSelectDate.setText(formattedDate);
                },
                year, month, day);
            datePickerDialog.show();
        });

        btnSelectTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                TaskListActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", 
                        selectedHour, selectedMinute);
                    btnSelectTime.setText(formattedTime);
                },
                hour, minute, true);
            timePickerDialog.show();
        });

        btnUpdateTask.setOnClickListener(v -> {
            String updatedTitle = etTaskTitle.getText().toString().trim();
            String updatedDescription = etTaskDescription.getText().toString().trim();
            String updatedDate = btnSelectDate.getText().toString();
            String updatedTime = btnSelectTime.getText().toString();

            if (updatedTitle.isEmpty()) {
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }

            if (updatedDate.equals("Select Date") || updatedTime.equals("Select Time")) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert to ISO 8601 format
            String isoDateTime = String.format("%sT%s:00.000Z", updatedDate, updatedTime);

            // Create JSON object for updating the task
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("id", task.getId());
                requestBody.put("title", updatedTitle);
                requestBody.put("description", updatedDescription);
                requestBody.put("dueDate", isoDateTime);
                requestBody.put("completed", task.isCompleted());
                requestBody.put("dayReminderSent", false);
                requestBody.put("hourReminderSent", false);
                requestBody.put("overdueReminderSent", false);
                
                JSONObject userObj = new JSONObject();
                userObj.put("id", Integer.parseInt(userId));
                requestBody.put("user", userObj);

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(TaskListActivity.this, "Error preparing task update", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send PUT request to update task
            String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/tasks/" + task.getId();
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, requestBody,
                    response -> {
                        dialog.dismiss();
                        fetchTasks();
                        Toast.makeText(TaskListActivity.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        String errorMessage = "";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                errorMessage = new String(error.networkResponse.data);
                            } catch (Exception e) {
                                errorMessage = error.toString();
                            }
                        } else {
                            errorMessage = error.toString();
                        }
                        Toast.makeText(TaskListActivity.this, "Error updating task: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });

            requestQueue.add(request);
        });

        dialog.show();
    }
}