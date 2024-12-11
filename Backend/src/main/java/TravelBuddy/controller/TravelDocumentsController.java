package TravelBuddy.controller;

import TravelBuddy.model.TravelDocument;
import TravelBuddy.service.TravelDocumentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/document/")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE})
public class TravelDocumentsController {

    //@Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private TravelDocumentsService travelDocumentsService;

    @Operation(summary = "Create Travel Document", 
            description = "Creates a new travel document with the provided details and file upload.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid document number provided"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally")
    })
    @PostMapping("/create")
    public ResponseEntity<String> createDocument(
            @RequestParam("userId") Long userId,
            @RequestParam("documentType") String documentType,
            @RequestParam("documentNumber") String documentNumber,
            @RequestParam("expiryDate") String expiryDate,
            @RequestParam("file") MultipartFile file) {
        try {
            if (documentNumber == null || documentNumber.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error: Document number is required.");
            }

            // Generate unique filename
            String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(uploadDir, fileName);

            // Save file
            Files.copy(file.getInputStream(), filePath);

            // Create document
            TravelDocument document = new TravelDocument();
            document.setUserId(userId);
            document.setDocumentType(documentType);
            document.setDocumentNumber(documentNumber);
            
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date parsedDate = formatter.parse(expiryDate);
            document.setExpiryDate(parsedDate);
            
            document.setFilePath(filePath.toString());
            
            LocalDateTime curTime = LocalDateTime.now();
            document.setCreatedAt(curTime);
            document.setUpdatedAt(curTime);

            travelDocumentsService.createTravelDocument(document);
            return ResponseEntity.ok("Successfully created the document.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Getting Travel Documents",
            description = "Retrieves a list of the Travel Documents associated with a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents successfully retrieved"),
            @ApiResponse(responseCode = "404", description = "User or TravelDocuments not found"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally")
    })
    @GetMapping("/read/{userId}")
    public ResponseEntity<?> readDocument(@PathVariable String userId) {

        try {
            List<TravelDocument> documents = travelDocumentsService.readTravelDocument(userId);

            if (documents == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No travel documents found for userId: " + userId);
            }

            return ResponseEntity.ok(documents);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Return Travel Document",
            description = "Returns a single TravelDocument from its id..")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document returned successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally")
    })
    @GetMapping("/read-by-id/{id}")
    public ResponseEntity<?> readByIdDocument(@PathVariable String id) {

        try {
            TravelDocument travelDocument = travelDocumentsService.readDocumentById(id);
            if (travelDocument == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document not found.");
            }
            else {
                return ResponseEntity.ok(travelDocument);
            }
        }
        catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete Travel Document",
            description = "Deletes a travel document based on its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable String id) {

        try {
            boolean successfullyDeleted = travelDocumentsService.deleteTravelDocument(id);
            if (successfullyDeleted) {
                return ResponseEntity.ok("Successfully deleted document.");
            }
            else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The document associated with id " + id + " was not found.");
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Updating TravelDocument",
            description = "Update the details of a TravelDocument.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document updated successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "400", description = "Invalid date format provided"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally"),
    })
    @PatchMapping("update/{id}")
    public ResponseEntity<String> updateDocument(
            @PathVariable String id,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) MultipartFile file) {
        try {
            // Get existing document
            TravelDocument existingDocument = travelDocumentsService.readDocumentById(id);
            if (existingDocument == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("The document associated with id " + id + " was not found.");
            }
            
            // Handle file upload if provided
            if (file != null && !file.isEmpty()) {
                // Delete old file if it exists
                if (existingDocument.getFilePath() != null) {
                    Path oldFilePath = Paths.get(existingDocument.getFilePath());
                    Files.deleteIfExists(oldFilePath);
                }
                
                // Save new file
                String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
                String fileName = UUID.randomUUID().toString() + fileExtension;
                Path filePath = Paths.get(uploadDir, fileName).normalize();
                Files.copy(file.getInputStream(), filePath);
                existingDocument.setFilePath(filePath.toString());
            }

            // Update other fields if provided
            if (documentNumber != null && !documentNumber.isEmpty()) {
                existingDocument.setDocumentNumber(documentNumber);
            }
            
            if (expiryDate != null && !expiryDate.isEmpty()) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date parsedDate = formatter.parse(expiryDate);
                    existingDocument.setExpiryDate(parsedDate);
                } catch (ParseException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid expiry date format. Expected format: yyyy-MM-dd");
                }
            }

            existingDocument.setUpdatedAt(LocalDateTime.now());
            boolean updated = travelDocumentsService.updateTravelDocument(id, existingDocument);
            
            if (updated) {
                return ResponseEntity.ok("Document successfully updated.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to update document");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Download Document File",
            description = "Downloads a document file by its filename.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally")
    })
    @GetMapping("/file/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, fileName);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Delete Document File",
            description = "Deletes a document file by its filename.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally")
    })
    @DeleteMapping("/file/{fileName:.+}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, fileName).normalize();
            File file = filePath.toFile();
            
            if (file.exists()) {
                if (file.delete()) {
                    return ResponseEntity.ok("File deleted successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete file");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }

}
