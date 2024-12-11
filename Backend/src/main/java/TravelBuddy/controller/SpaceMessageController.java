package TravelBuddy.controller;

import TravelBuddy.model.SpaceMessage;
import TravelBuddy.service.SpaceMessageService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;


@Controller
public class SpaceMessageController {

    @Autowired
    private final SpaceMessageService messageService;

    public SpaceMessageController(SpaceMessageService messageService) {

        this.messageService = messageService;
    }

    @MessageMapping("/travelspace/{id}/sendMessage")
    @SendTo("topic/travelspace/{id}")
    public SpaceMessage sendMessage(@DestinationVariable Long id, @Payload SpaceMessage spaceMessage) {

        return messageService.processMessage(id, spaceMessage);
    }
}
