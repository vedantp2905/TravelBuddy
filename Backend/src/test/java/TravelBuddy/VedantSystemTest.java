package TravelBuddy;

import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
    "upload.directory=${java.io.tmpdir}/travelbuddy-test-uploads",
    "profilePictures.upload-dir=${java.io.tmpdir}/travelbuddy-test-uploads/profile_pics",
    "travelImages.upload-dir=${java.io.tmpdir}/travelbuddy-test-uploads/travel_images",
    "documents.upload-dir=${java.io.tmpdir}/travelbuddy-test-uploads/documents"
})
public class VedantSystemTest {

    @LocalServerPort
    int port;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        System.setProperty("upload.directory", tempDir.toString());
    }

    @Test
    public void testUserLogin() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "vedant29");
        loginData.put("password", "vedant123");

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginData)
            .when()
            .post("/api/users/login");

        assertEquals(200, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().asString().contains("Login successful"));
    }

    @Test
    public void testGetUserById() {
        Response response = given()
            .when()
            .get("/api/users/28");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("vedant29"));
    }

    @Test
    public void testCheckPassword() {
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("password", "vedant123");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(passwordData)
            .when()
            .post("/api/users/check-password/28");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("true"));
    }

    @Test
    public void testGetNewsletterPreference() {
        Response response = given()
            .when()
            .get("/api/users/newsletter-preference/28");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testUpdateNewsletterPreference() {
        Map<String, Boolean> preferenceData = new HashMap<>();
        preferenceData.put("subscribed", true);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(preferenceData)
            .when()
            .post("/api/users/newsletter-preference/28");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Newsletter preference updated successfully"));
    }

    @Test
    public void testPartialUpdateUser() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("firstName", "Vedant");
        updateData.put("lastName", "Pungliya");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(updateData)
            .when()
            .patch("/api/users/update/28");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("User details updated successfully"));
    }

    @Test
    public void testRequestPasswordReset() {
        Map<String, String> resetData = new HashMap<>();
        resetData.put("email", "vedantpungliya29@gmail.com");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(resetData)
            .when()
            .post("/api/users/reset-password-request");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Verification code has been sent"));
    }

    @Test
    public void testGetUserRewards() {
        Response response = given()
            .when()
            .get("/api/reward/28");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testAddRewardPoints() {
        Map<String, Object> pointsData = new HashMap<>();
        pointsData.put("balance", 100);

        Response response = given()
            .contentType(ContentType.JSON)
            .body(pointsData)
            .when()
            .post("/api/reward/28/update");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Balance updated successfully"));
    }

    @Test
    public void testRedeemRewards() {
        // First add enough points
        Map<String, Object> pointsData = new HashMap<>();
        pointsData.put("balance", 2000);  // More than required 1000 for monthly

        given()
            .contentType(ContentType.JSON)
            .body(pointsData)
            .when()
            .post("/api/reward/28/update");

        // Then try to redeem
        Map<String, String> redeemData = new HashMap<>();
        redeemData.put("plan", "monthly");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(redeemData)
            .when()
            .post("/api/reward/28/use-for-premium");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("Successfully upgraded to premium"));
    }

    @Test
    public void testGetRewardHistory() {
        Response response = given()
            .when()
            .get("/api/reward/28");

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testUpdateTripTask() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("title", "Updated Pack Bags");
        updateData.put("description", "Updated packing list");
        updateData.put("completed", true);
        updateData.put("dueDate", "2024-12-30T10:00:00");
        updateData.put("dayReminderSent", false);
        updateData.put("hourReminderSent", false);
        updateData.put("overdueReminderSent", false);
        updateData.put("user", Map.of("id", 28));

        Response response = given()
            .contentType(ContentType.JSON)
            .body(updateData)
            .when()
            .put("/api/tasks/1");

        assertEquals(200, response.getStatusCode());
    }

}