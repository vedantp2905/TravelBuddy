package TravelBuddy.controller;

import TravelBuddy.model.Poll;
import TravelBuddy.model.PollResponse;
import TravelBuddy.service.PollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/polls")
@Tag(name = "Poll Management", description = "APIs for managing group polls and voting")
public class PollController {

    @Autowired
    private PollService pollService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "Create new poll",
            description = "Creates a new poll for group voting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Poll created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid poll data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public Poll createPoll(@RequestBody Poll poll) {
        Poll createdPoll = pollService.createPoll(poll);
        createdPoll.setVotes(new HashMap<>());
        messagingTemplate.convertAndSend("/topic/polls", createdPoll);
        return createdPoll;
    }

    @Operation(summary = "Get active polls",
            description = "Retrieves all currently active polls")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved polls"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public List<Poll> getActivePolls() {
        return pollService.getActivePolls();
    }

    @Operation(summary = "Handle poll vote",
            description = "Processes a user's vote on a poll")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vote recorded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid vote data"),
        @ApiResponse(responseCode = "404", description = "Poll not found")
    })
    @MessageMapping("/poll/vote")
    @SendTo("/topic/polls")
    public Map<String, Object> handleVote(Map<String, Object> voteData) {
        Long pollId = Long.parseLong(voteData.get("pollId").toString());
        String option = (String) voteData.get("option");
        Long userId = Long.parseLong(voteData.get("userId").toString());
        
        PollResponse response = pollService.addVote(pollId, option, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("poll", response.getPoll());
        result.put("message", response.getMessage());
        result.put("success", response.isSuccess());
        result.put("voterId", userId);
        
        return result;
    }

    @Operation(summary = "Close poll",
            description = "Closes an active poll, calculates results, and rewards winning voters")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Poll closed successfully and rewards distributed"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient votes"),
        @ApiResponse(responseCode = "404", description = "Poll not found")
    })
    @PostMapping("/{pollId}/close")
    public ResponseEntity<?> closePoll(@PathVariable Long pollId, @RequestParam Long userId) {
        try {
            Poll closedPoll = pollService.closePoll(pollId);
            
            Map<String, Integer> votes = closedPoll.getVotes();
            int maxVotes = votes.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
                    
            List<String> winners = votes.entrySet().stream()
                    .filter(entry -> entry.getValue() == maxVotes)
                    .map(Map.Entry::getKey)
                    .toList();
            
            String winnerMessage = winners.size() > 1 
                ? "Poll closed! It's a tie between: " + String.join(", ", winners) + ". Voters for these options received 10 reward points!"
                : "Poll closed! The winner is: " + winners.get(0) + ". Voters for this option received 10 reward points!";
            
            Map<String, Object> result = new HashMap<>();
            result.put("poll", closedPoll);
            result.put("type", "CLOSE");
            result.put("message", winnerMessage);
            result.put("voterId", userId);
            result.put("rewardPoints", "10");
            
            messagingTemplate.convertAndSend("/topic/polls", result);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Delete poll",
            description = "Deletes a poll and its associated votes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Poll deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient votes"),
        @ApiResponse(responseCode = "403", description = "User not authorized to delete poll"),
        @ApiResponse(responseCode = "404", description = "Poll not found")
    })
    @DeleteMapping("/{pollId}")
    public ResponseEntity<?> deletePoll(
        @Parameter(description = "ID of the poll to delete") @PathVariable Long pollId,
        @Parameter(description = "ID of the user attempting to delete") @RequestParam Long userId
    ) {
        try {
            pollService.deletePoll(pollId, userId);
            messagingTemplate.convertAndSend("/topic/polls", Map.of(
                "type", "DELETE",
                "pollId", pollId
            ));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 