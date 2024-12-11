package TravelBuddy.service;

import TravelBuddy.model.Poll;
import TravelBuddy.model.Vote;
import TravelBuddy.repositories.PollRepository;
import TravelBuddy.repositories.VoteRepository;
import TravelBuddy.model.PollResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@Transactional
@EnableScheduling
public class PollService {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(PollService.class);

    public Poll createPoll(Poll poll) {
        poll.setCreatedAt(LocalDateTime.now());
        poll.setExpiresAt(LocalDateTime.now().plusDays(14));
        return pollRepository.save(poll);
    }

    @Transactional(readOnly = true)
    public List<Poll> getActivePolls() {
        List<Poll> polls = pollRepository.findByActiveTrue();
        polls.forEach(poll -> {
            List<Map<String, Object>> voteCounts = voteRepository.countVotesByPollIdGroupByOption(poll.getId());
            Map<String, Integer> votes = new HashMap<>();
            voteCounts.forEach(count -> {
                String option = (String) count.get("option");
                Long voteCount = (Long) count.get("count");
                votes.put(option, voteCount.intValue());
            });
            poll.setVotes(votes);
        });
        return polls;
    }

    @Transactional
    public PollResponse addVote(Long pollId, String option, Long userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));
        
        if (!poll.isActive()) {
            return new PollResponse(getPollWithVotes(pollId), "Poll is closed", false);
        }

        Vote vote = voteRepository.findByPoll_IdAndUserId(pollId, userId);
        
        if (vote != null) {
            if (vote.getSelectedOption().equals(option)) {
                return new PollResponse(getPollWithVotes(pollId), "You have already voted for this option", false);
            }
            vote.setSelectedOption(option);
            vote.setVotedAt(LocalDateTime.now());
        } else {
            vote = new Vote();
            vote.setPollId(pollId);
            vote.setUserId(userId);
            vote.setSelectedOption(option);
            vote.setVotedAt(LocalDateTime.now());
        }
        
        voteRepository.save(vote);
        return new PollResponse(getPollWithVotes(pollId), 
            vote.getId() == null ? "Vote recorded successfully" : "Vote updated successfully", 
            true);
    }

    public Poll closePoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));
        
        Map<String, Integer> finalVotes = getPollWithVotes(pollId).getVotes();
        int totalVotes = finalVotes.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalVotes < 2) {
            throw new RuntimeException("Cannot close poll with less than 2 votes");
        }
        
        // Find the maximum vote count
        int maxVotes = finalVotes.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        
        // Get winning options
        List<String> winners = finalVotes.entrySet().stream()
                .filter(entry -> entry.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();
        
        // Reward users who voted for winning options
        List<Vote> allVotes = voteRepository.findByPoll_Id(pollId);
        for (Vote vote : allVotes) {
            if (winners.contains(vote.getSelectedOption())) {
                try {
                    BigDecimal currentBalance = rewardService.getBalance(vote.getUserId());
                    BigDecimal reward = new BigDecimal("10.00");
                    rewardService.updateBalance(vote.getUserId(), currentBalance.add(reward));
                } catch (Exception e) {
                    // Log error but continue processing other rewards
                    logger.error("Failed to reward user {}: {}", vote.getUserId(), e.getMessage());
                }
            }
        }
                
        poll.setActive(false);
        Poll savedPoll = pollRepository.save(poll);
        savedPoll.setVotes(finalVotes);
        
        return savedPoll;
    }

    @Transactional(readOnly = true)
    private Poll getPollWithVotes(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));
        
        List<Map<String, Object>> voteCounts = voteRepository.countVotesByPollIdGroupByOption(pollId);
        Map<String, Integer> votes = new HashMap<>();
        voteCounts.forEach(count -> {
            String option = (String) count.get("option");
            Long voteCount = (Long) count.get("count");
            votes.put(option, voteCount.intValue());
        });
        poll.setVotes(votes);
        
        return poll;
    }

    @Transactional
    public void deletePoll(Long pollId, Long userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));
                
        if (!poll.getCreatorId().equals(userId)) {
            throw new RuntimeException("Only the creator can delete this poll");
        }
        
        Map<String, Integer> votes = getPollWithVotes(pollId).getVotes();
        int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalVotes < 2) {
            throw new RuntimeException("Cannot delete poll with less than 2 votes");
        }
        
        // The cascade will handle vote deletion automatically
        pollRepository.deleteById(pollId);
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void closeExpiredPolls() {
        List<Poll> expiredPolls = pollRepository.findByActiveTrueAndExpiresAtBefore(LocalDateTime.now());
        for (Poll poll : expiredPolls) {
            try {
                Poll closedPoll = closePoll(poll.getId());
                
                // Announce winners through WebSocket
                Map<String, Object> result = new HashMap<>();
                result.put("type", "EXPIRED");
                result.put("poll", closedPoll);
                result.put("message", "Poll expired after 14 days. Winners have been rewarded!");
                
                messagingTemplate.convertAndSend("/topic/polls", result);
            } catch (Exception e) {
                logger.error("Failed to close expired poll {}: {}", poll.getId(), e.getMessage());
            }
        }
    }
} 