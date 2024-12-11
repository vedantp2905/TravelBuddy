package TravelBuddy.controller;

import TravelBuddy.model.ProfilePicture;
import TravelBuddy.model.User;
import TravelBuddy.repositories.ProfilePictureRepository;
import TravelBuddy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;  
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/profile-picture")
@Tag(name="Profile Picture", description = "APIs for profile pictures")
public class ProfilePictureController {

    private static final Logger log = LoggerFactory.getLogger(ProfilePictureController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ProfilePictureRepository profilePictureRepository;

    @Value("${profilePictures.upload-dir}")
    private String directory;

    @PostConstruct
    public void init() {
        try {
            File dir = new File(directory);
            if (!dir.exists()) {
                log.info("Creating profile pictures directory at: {}", directory);
                if (!dir.mkdirs()) {
                    log.warn("Could not create directory, but it might already exist: {}", directory);
                }
            }
            
            if (dir.exists()) {
                log.info("Using profile pictures directory at: {}", dir.getAbsolutePath());
                // Test write permissions
                File testFile = new File(dir, ".test");
                try {
                    if (testFile.createNewFile()) {
                        testFile.delete();
                        log.info("Directory is writable");
                    }
                } catch (IOException e) {
                    log.warn("Directory exists but may not be writable: {}. Error: {}", directory, e.getMessage());
                }
            }
        } catch (SecurityException e) {
            log.error("Security error with profile pictures directory: {}. Error: {}", directory, e.getMessage());
        }
    }

    @Operation(summary = "Get Profile Picture",
            description = "Returns the profile picture of the user.")
    @GetMapping(value = "/get/{userId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> getImageById(@PathVariable Long userId) throws IOException {
        try {
            if (!userService.userExists(userId)) {
                throw new RuntimeException("User not found.");
            }

            User user = userService.findById(userId);
            ProfilePicture picture = profilePictureRepository.findByUser(user);
            
            if (picture == null) {
                // Return default profile picture
                File defaultImage = new File(directory + "/default_profile.jpg");
                return ResponseEntity.ok(Files.readAllBytes(defaultImage.toPath()));
            }

            File imageFile = new File(picture.getFilePath());
            return ResponseEntity.ok(Files.readAllBytes(imageFile.toPath()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching profile picture: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload Profile Picture",
            description = "Uploads the profile picture to the server.")
    @PostMapping("/upload/{userId}")
    public ResponseEntity<?> handleFileUpload(@RequestParam("image") MultipartFile imageFile, @PathVariable long userId) {
        try {
            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Profile picture is required");
            }
            
            // Create directories if they don't exist
            File uploadDir = new File(directory);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            if (!userService.userExists(userId)) {
                return ResponseEntity.badRequest().body("User not found.");
            }
            User user = userService.findById(userId);

            if (profilePictureRepository.existsByUser(user)) {
                ProfilePicture oldPicture = profilePictureRepository.findByUser(user);
                // Delete old file
                File oldFile = new File(oldPicture.getFilePath());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
                profilePictureRepository.delete(oldPicture);
            }

            String uniqueFilename = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            String path = directory + "/" + uniqueFilename;
            File destinationFile = new File(path);
            imageFile.transferTo(destinationFile);

            ProfilePicture picture = new ProfilePicture();
            picture.setFilePath(destinationFile.getAbsolutePath());
            picture.setUser(user);
            profilePictureRepository.save(picture);

            return ResponseEntity.ok("File uploaded successfully: " + destinationFile.getAbsolutePath());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }



}


