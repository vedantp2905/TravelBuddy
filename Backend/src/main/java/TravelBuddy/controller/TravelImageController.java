package TravelBuddy.controller;

import TravelBuddy.model.TravelImage;
import TravelBuddy.model.TravelPost;
import TravelBuddy.repositories.TravelImageRepository;
import TravelBuddy.repositories.TravelPostRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travelimage")
@Tag(name="Travel Images", description = "APIs for managing TravelImages")
public class TravelImageController {

    @Value("${travelImages.upload-dir}")
    private String directory;

    @Autowired
    private TravelImageRepository travelImageRepository;

    @Autowired
    private TravelPostRepository travelPostRepository;

    private static final Logger log = LoggerFactory.getLogger(TravelImageController.class);

    @PostConstruct
    public void init() {
        try {
            File dir = new File(directory);
            if (!dir.exists()) {
                log.info("Creating travel images directory at: {}", directory);
                if (!dir.mkdirs()) {
                    log.warn("Could not create directory, but it might already exist: {}", directory);
                }
            }
            
            if (dir.exists()) {
                log.info("Using travel images directory at: {}", dir.getAbsolutePath());
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
            log.error("Security error with travel images directory: {}. Error: {}", directory, e.getMessage());
        }
    }

    @Operation(summary = "Return image",
            description = "Returns the image associated with the provided TravelPost's id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image returned successfully.")
    })
    @GetMapping(value = "/images/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getImageById(@PathVariable Long id) throws IOException {
        if (!travelPostRepository.existsById(id))
            throw new RuntimeException(("Post not found."));
        TravelPost post = travelPostRepository.getReferenceById(id);
        List<TravelImage> images = post.getImages();
        if (images.isEmpty()) {
            throw new RuntimeException(("No images associated with this post."));
        }
        TravelImage firstImage = images.get(0);
        //TravelImage travelImage = travelImageRepository.findById(id)
                //.orElseThrow(() -> new RuntimeException("Image not found"));
        File imageFile = new File(firstImage.getFilePath());
        return Files.readAllBytes(imageFile.toPath());
    }

    @Operation(summary = "Post image",
            description = "Saves a posted image to the database and associates it with a TravelPost.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image posted successfully."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @PostMapping("/{postId}/images")
    public ResponseEntity<?> uploadImageForPost(@PathVariable Long postId,
                                                @RequestParam("image") MultipartFile imageFile) {
        log.info("=== Starting image upload process ===");
        log.info("Post ID: {}", postId);
        log.info("Image name: {}", imageFile.getOriginalFilename());
        log.info("Image size: {} bytes", imageFile.getSize());
        log.info("Content type: {}", imageFile.getContentType());
        
        try {
            TravelPost travelPost = travelPostRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("TravelPost not found"));

            String uniqueFilename = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            String path = directory + File.separator + uniqueFilename;
            
            File destinationFile = new File(path);
            
            log.info("Saving image to: {}", destinationFile.getAbsolutePath());
            imageFile.transferTo(destinationFile);
            
            TravelImage travelImage = new TravelImage();
            travelImage.setFilePath(destinationFile.getAbsolutePath());
            travelImage.setPost(travelPost);
            TravelImage savedImage = travelImageRepository.save(travelImage);
            
            log.info("Image saved successfully with ID: {}", savedImage.getId());
            
            return ResponseEntity.ok()
                .body(Map.of(
                    "message", "Image uploaded successfully",
                    "imageId", savedImage.getId(),
                    "filePath", savedImage.getFilePath()
                ));
        } catch (Exception e) {
            log.error("Error uploading image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }
}


//                File newDirectory = new File(directory);
//                if (!newDirectory.exists()) {
//                    newDirectory.mkdirs(); // Creates all non-existent parent directories
//                }

//                File newDirectory = new File(directory);
//                if (!newDirectory.exists()) {
//                    boolean dirCreated = newDirectory.mkdirs();  // Creates all non-existent parent directories
//                    if (!dirCreated) {
//                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                                .body("Failed to create directory for image storage.");
//                    }
//                }
//File destinationFile = new File(directory + File.separator + imageFile.getOriginalFilename());
//File destinationFile = new File(newDirectory, imageFile.getOriginalFilename());