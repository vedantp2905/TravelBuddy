package TravelBuddy.controller;

import TravelBuddy.model.TripTask;
import TravelBuddy.model.User;
import TravelBuddy.service.TripTaskService;
import TravelBuddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import TravelBuddy.service.ReminderService;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Trip Tasks", description = "APIs for managing trip tasks")
public class TripTaskController {

    @Autowired
    private TripTaskService tripTaskService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReminderService reminderService;

    @Operation(summary = "Create a new trip task",
            description = "Creates a new trip task for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping
    public ResponseEntity<TripTask> createTask(@RequestBody TripTask task) {
        Long userId = task.getUser().getId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        task.setUser(user);
        
        return ResponseEntity.ok(tripTaskService.createTask(task));
    }

    @Operation(summary = "Get tasks for a user",
            description = "Retrieves all tasks associated with a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTasks(@PathVariable Long userId) {
        List<TripTask> tasks = tripTaskService.getUserTasks(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("tasks", tasks);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing trip task",
            description = "Updates the details of an existing trip task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PutMapping("/{taskId}")
    public ResponseEntity<TripTask> updateTask(@PathVariable Long taskId, @RequestBody TripTask task) {
        task.setId(taskId);
        return ResponseEntity.ok(tripTaskService.updateTask(task));
    }

    @Operation(summary = "Delete a trip task",
            description = "Deletes a specific trip task by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        tripTaskService.deleteTask(taskId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Complete a trip task via email",
            description = "Marks a specific trip task as completed via email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task marked as completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired completion link"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/complete/{taskId}/{token}")
    public ResponseEntity<String> completeTaskViaEmail(
        @PathVariable Long taskId,
        @PathVariable String token) {
        
        // Verify the token is valid
        if (!reminderService.verifyCompletionToken(taskId, token)) {
            return ResponseEntity.badRequest().body("Invalid or expired completion link");
        }
        
        TripTask task = tripTaskService.getTaskById(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        task.setCompleted(true);
        tripTaskService.updateTask(task);
        
        return ResponseEntity.ok("Task marked as completed successfully!");
    }
}