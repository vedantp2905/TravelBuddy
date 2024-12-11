package TravelBuddy.service;

import TravelBuddy.model.Itinerary;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PDFService {
    
    private String cleanHtmlTags(String text) {
        return text.replaceAll("<[^>]*>", "")  // Remove HTML tags
                  .replaceAll("&nbsp;", " ")   // Replace &nbsp; with space
                  .replaceAll("&amp;", "&")    // Replace &amp; with &
                  .replaceAll("&lt;", "<")     // Replace &lt; with <
                  .replaceAll("&gt;", ">")     // Replace &gt; with >
                  .replaceAll("\\s+", " ")     // Replace multiple spaces with single space
                  .trim();                     // Trim leading/trailing spaces
    }
    
    private void addFormattedContent(Document document, String htmlContent, Font contentFont) throws DocumentException {
        // Split content into sections based on headers
        String[] sections = htmlContent.split("(?i)(<h[23]>|<div class=\"day-header\">)");
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;
            
            // Process each section
            if (section.toLowerCase().contains("day") || 
                section.toLowerCase().contains("summary") || 
                section.toLowerCase().contains("flight details")) {
                
                // Extract header text (everything before the closing tag)
                String headerText = section.replaceAll("(?s)</h[23]>.*", "").trim();
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
                Paragraph header = new Paragraph(cleanHtmlTags(headerText), headerFont);
                header.setSpacingBefore(15);
                header.setSpacingAfter(5);
                document.add(header);
                
                // Get content after the header closing tag
                String content = section.replaceAll("(?s).*?</h[23]>", "").trim();
                
                // Process paragraphs
                String[] paragraphs = content.split("(?i)</?p>");
                for (String para : paragraphs) {
                    if (para.trim().isEmpty()) continue;
                    
                    // Handle subsections with <strong> tags
                    if (para.toLowerCase().contains("<strong>")) {
                        // Extract and add subsection header
                        String[] parts = para.split("(?i)</strong>");
                        String subHeader = parts[0].replaceAll("(?i)<strong>", "").trim();
                        Font subHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
                        Paragraph subHeaderPara = new Paragraph(cleanHtmlTags(subHeader), subHeaderFont);
                        subHeaderPara.setSpacingBefore(10);
                        subHeaderPara.setIndentationLeft(20);
                        document.add(subHeaderPara);
                        
                        // Add subsection content if it exists
                        if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                            Paragraph contentPara = new Paragraph(cleanHtmlTags(parts[1]), contentFont);
                            contentPara.setIndentationLeft(30);
                            contentPara.setAlignment(Element.ALIGN_JUSTIFIED);
                            document.add(contentPara);
                        }
                    } else {
                        // Add regular paragraph
                        Paragraph contentPara = new Paragraph(cleanHtmlTags(para), contentFont);
                        contentPara.setIndentationLeft(20);
                        contentPara.setAlignment(Element.ALIGN_JUSTIFIED);
                        document.add(contentPara);
                    }
                }
            }
        }
    }
    
    public byte[] generateItineraryPDF(Itinerary itinerary) throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();
            
            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Travel Itinerary", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);
            
            // Trip Details Section
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, new BaseColor(44, 62, 80));
            Font contentFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            
            Paragraph tripDetails = new Paragraph("Trip Details", sectionFont);
            tripDetails.setSpacingBefore(20);
            tripDetails.setSpacingAfter(10);
            document.add(tripDetails);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            
            // Add trip information
            document.add(new Paragraph("Destination: " + cleanHtmlTags(itinerary.getCountry()), contentFont));
            document.add(new Paragraph("Cities: " + cleanHtmlTags(itinerary.getCities()), contentFont));
            document.add(new Paragraph("Travel Dates: " + 
                itinerary.getStartDate().format(formatter) + " to " + 
                itinerary.getEndDate().format(formatter), contentFont));
            document.add(new Paragraph("Number of Travelers: " + 
                itinerary.getNumberOfAdults() + " adults, " + 
                itinerary.getNumberOfChildren() + " children", contentFont));
            
            // Itinerary Details Section
            Paragraph itineraryDetails = new Paragraph("Daily Itinerary", sectionFont);
            itineraryDetails.setSpacingBefore(30);
            itineraryDetails.setSpacingAfter(10);
            document.add(itineraryDetails);
            
            // Add formatted content - ONLY ONCE
            addFormattedContent(document, itinerary.getGeneratedItinerary(), contentFont);
            
            // Add footer
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
            Paragraph footer = new Paragraph("Generated by TravelBuddy", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);
            
        } finally {
            document.close();
        }
        
        return outputStream.toByteArray();
    }
} 