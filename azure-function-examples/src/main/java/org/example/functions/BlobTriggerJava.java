package org.example.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import org.example.functions.service.DocumentIntelligenceService;
import org.example.functions.service.DocumentProcessingException;

import java.util.List;
import java.util.logging.Level;

/**
 * Backup implementation of BlobTriggerJava function.
 * This is a simplified version of the main BlobTriggerJava function.
 */
public class BlobTriggerJava {
    private final DocumentIntelligenceService documentService;

    /**
     * Initializes a new instance of the BlobTriggerJava class.
     */
    public BlobTriggerJava() {
        String endpoint = System.getenv("AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT");
        String apiKey = System.getenv("AZURE_DOCUMENT_INTELLIGENCE_KEY");
        
        if (endpoint == null || endpoint.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "Required environment variables AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT and AZURE_DOCUMENT_INTELLIGENCE_KEY must be set"
            );
        }
        
        this.documentService = new DocumentIntelligenceService(endpoint, apiKey);
    }

    /**
     * This function will be invoked when a new or updated blob is detected at the specified path.
     * It only processes PDF files.
     * 
     * @param content The blob content
     * @param name The name of the blob
     * @param context The execution context
     */
    @FunctionName("BlobTriggerJava")
    public void run(
        @BlobTrigger(
            name = "content",
            path = "documents/{name}",  // Different path to avoid conflict with main trigger
            dataType = "binary",
            connection = "AzureWebJobsStorage"
        ) byte[] content,
        @BindingName("name") String name,
        final ExecutionContext context
    ) {
        var logger = context.getLogger();
        
        try {
            if (!name.toLowerCase().endsWith(".pdf")) {
                logger.info("Skipping non-PDF file: " + name);
                return;
            }
            
            logger.info(" Processing PDF document: " + name);
            logger.info(" Blob size: " + content.length + " bytes");
            
            // Process the PDF document
            List<String> extractedLines = documentService.analyzeDocument(content);
            
            // Log the number of lines extracted (without logging the actual content for brevity)
            logger.info(" Successfully extracted " + extractedLines.size() + " lines from document: " + name);
            
        } catch (DocumentProcessingException e) {
            logger.log(Level.SEVERE, " Failed to process document: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, " Unexpected error processing document: " + e.getMessage(), e);
            throw e; // Let the Azure Functions runtime handle the error
        }
    }
}
