package TravelBuddy.service;

import TravelBuddy.model.TripTask;
import TravelBuddy.repositories.TripTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripTaskService {

    @Autowired
    private TripTaskRepository tripTaskRepository;

    public TripTask createTask(TripTask task) {
        return tripTaskRepository.save(task);
    }

    public List<TripTask> getUserTasks(Long userId) {
        return tripTaskRepository.findByUserId(userId);
    }

    public TripTask updateTask(TripTask task) {
        return tripTaskRepository.save(task);
    }

    public void deleteTask(Long taskId) {
        tripTaskRepository.deleteById(taskId);
    }

    public List<TripTask> getIncompleteTasks() {
        return tripTaskRepository.findByCompletedFalse();
    }

    public TripTask getTaskById(Long taskId) {
        return tripTaskRepository.findById(taskId).orElse(null);
    }
} 