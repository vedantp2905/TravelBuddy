package TravelBuddy;

import TravelBuddy.model.FriendRequest;
import TravelBuddy.model.Like;
import TravelBuddy.model.User;
import TravelBuddy.repositories.FriendRequestRepository;
import TravelBuddy.repositories.LikeRepository;
import TravelBuddy.repositories.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.test.context.junit4.SpringRunner;

//new ones
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;


//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.test.context.junit4.SpringRunner;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "upload.directory=${java.io.tmpdir}/travelbuddy-test-uploads",
        "profilePictures.upload-dir=${java.io.tmpdir}/travelbuddy-test-uploads/profile_pics",
        "travelImages.upload-dir=${java.io.tmpdir}/travelbuddy-test-uploads/travel_images",
        "documents.upload-dir=${java.io.tmpdir}/travelbuddy-test-uploads/documents"
})
public class VinayakTrigunayatSystemTest {

    @LocalServerPort
    int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testGetFriends() {
        Response response = given()
                .when()
                .get("/api/friend/get/6");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("a"));
    }

    @Test
    public void testTravelFeedGet() {
        Response response = given()
                .when()
                .get("/api/post/get-posts/?newest=true&page=0&size=10");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testLikeCreate() {
        Map<String, Object> likeBody = new HashMap<>();
        likeBody.put("userId", "20");
        likeBody.put("postId", "82");

        Optional<Like> like = likeRepository.findByUserIdAndPostId(20L, 82L);
        if (like.isPresent()) {

            long likeId = like.get().getId();
            likeRepository.deleteById(likeId);
        }

        Response response = given()
                .contentType(ContentType.JSON)
                .body(likeBody)
                .when()
                .post("/api/like/create/");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Like created successfully."));
    }


//    @Test
//    public void testLikeCreate() {
//        Map<String, Object> likeBody = new HashMap<>();
//        likeBody.put("userId", "51");
//        likeBody.put("postId", "72");
//
//        Response response = given()
//                .contentType(ContentType.JSON)
//                .body(likeBody)
//                .when()
//                .post("/api/like/create/");
//
//        assertEquals(200, response.getStatusCode());
//        assertTrue(response.getBody().asString().contains("Like created successfully."));
//    }

    @Test
    public void testCommentCreate() {
        Map<String, Object> commentBody = new HashMap<>();
        commentBody.put("userId", "20");
        commentBody.put("postId", "82");
        commentBody.put("description", "Wow!");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(commentBody)
                .when()
                .post("/api/comment/create/");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Comment created successfully"));
    }

    @Test
    public void testHotelDetailsAPI() {
        Response response = given()
                .when()
                .get("/external-api/fetch-hotel-details?city=Omaha&checkIn=2025-10-10&checkOut=2025-10-15");

        assertEquals(200, response.getStatusCode());
        assertTrue(!response.getBody().asString().isEmpty());
    }

    @Test
    public void testTravelSpaceGet() {
        Response response = given()
                .when()
                .get("/api/travelspace/get/");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testSendRequest_Success() {

        User user59 = userRepository.findById(59L).get();
        User user60 = userRepository.findById(60L).get();
        FriendRequest request = friendRequestRepository.findBySenderAndReceiver(user59, user60);
        String senderId = "59";
        String receiverId = "60";

        if (request != null) {
            friendRequestRepository.delete(request);
        }


        String postString = String.format("/api/friend-request/send-request/?senderId=%s&receiverId=%s", senderId, receiverId);
        Response response = given()
                .queryParam("senderId", senderId)
                .queryParam("receiverId", receiverId)
                .when()
                .post(postString);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Request sent successfully."));
    }

    @Test
    public void testSendRequest_SenderNotFound() {
        String senderId = "99999";
        String receiverId = "52";
        String postString = String.format("/api/friend-request/send-request/?senderId=%s&receiverId=%s", senderId, receiverId);

        Response response = given()
                .queryParam("senderId", senderId)
                .queryParam("receiverId", receiverId)
                .when()
                .post(postString);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Sender not found."));
    }

    @Test
    public void testSearchUsers_Success() {
        long searcherId = 8;  // Ensure this user exists in your test database
        String prompt = "john";

        Response response = given()
                .queryParam("searcherId", searcherId)
                .queryParam("prompt", prompt)
                .when()
                .get("/api/friend/search/");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("john_doe"));
    }



}
