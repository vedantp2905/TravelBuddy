package TravelBuddy.service;

import TravelBuddy.model.TravelDocument;
import TravelBuddy.repositories.TravelDocumentsRepository;
import TravelBuddy.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TravelDocumentsService {

    private final TravelDocumentsRepository travelDocumentsRepository;
    private final UserRepository userRepository;
    @Autowired
    public TravelDocumentsService(TravelDocumentsRepository travelDocumentsRepository, UserRepository userRepository) {
        this.travelDocumentsRepository = travelDocumentsRepository;
        this.userRepository = userRepository;
    }

    public boolean doesUserExist(TravelDocument travelDocument) {

        if (!(userRepository.existsById(travelDocument.getUserId()))) {

            return false;
        }
        else {
            return true;
        }
    }

    public TravelDocument createTravelDocument(TravelDocument travelDocument) {
        return travelDocumentsRepository.save(travelDocument);
    }

    public List<TravelDocument> readTravelDocument(String userId) {

        if (!(userRepository.existsById(Long.parseLong(userId)))) {

            return null;
        }

        return travelDocumentsRepository.findByUserId(Long.parseLong(userId));
    }

    public TravelDocument readDocumentById(String id) {

        if (travelDocumentsRepository.existsById(Long.parseLong(id))) {
            return travelDocumentsRepository.findById(Long.parseLong(id)).get();
        }
        return null;
    }

    public boolean deleteTravelDocument(String id) {

        if (!(travelDocumentsRepository).existsById(Long.parseLong(id))) {

            //return false to indicate that the document wasn't found
            return false;
        }

        travelDocumentsRepository.deleteById(Long.parseLong(id));
        //return true to indicate that the document was found and deleted
        return true;
    }

    public boolean updateTravelDocument(String id, TravelDocument updatedDocument) {

        Optional<TravelDocument> existingDocumentOpt = travelDocumentsRepository.findById(Long.parseLong(id));
        if (existingDocumentOpt.isEmpty()) {
            return false;
        }
        TravelDocument existingDocument = existingDocumentOpt.get();

        if (updatedDocument.getDocumentNumber() != null) {
            existingDocument.setDocumentNumber(updatedDocument.getDocumentNumber());
        }
        if (updatedDocument.getUpdatedAt() != null) {
            existingDocument.setUpdatedAt((updatedDocument.getUpdatedAt()));
        }
        if (updatedDocument.getExpiryDate() != null) {
            existingDocument.setExpiryDate((updatedDocument.getExpiryDate()));
        }
        if (updatedDocument.getFilePath() != null) {
            existingDocument.setFilePath((updatedDocument.getFilePath()));
        }

        travelDocumentsRepository.save(existingDocument);
        return true;
    }

}
