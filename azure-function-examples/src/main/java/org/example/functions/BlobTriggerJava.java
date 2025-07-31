package org.example.functions;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import org.example.functions.model.PageChunk;
import org.example.functions.service.DocumentIntelligenceService;
import org.example.functions.service.DocumentProcessingException;

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
     * @param name    The name of the blob
     * @param context The execution context
     */
    @FunctionName("BlobTriggerJava")
    public void run(
            @BlobTrigger(
                    name = "content",
                    path = "documents/{name}",
                    dataType = "binary",
                    connection = "AzureWebJobsStorage"
            ) byte[] content,
            @BindingName("name") String name,
            final ExecutionContext context
    ) throws DocumentProcessingException {
        Logger logger = context.getLogger();

        try {
            if (!name.toLowerCase().endsWith(".pdf")) {
                logger.info("Skipping non-PDF file: " + name);
                return;
            }

            // Generate a SAS URL for the blob
            String connectionString = System.getenv("AzureWebJobsStorage");
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("documents");
            BlobClient blobClient = containerClient.getBlobClient(name);

            // Generate SAS URL valid for 15 minutes
            BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
            BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                    OffsetDateTime.now().plusMinutes(15),
                    permissions
            );

            String sasUrl = blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
            logger.info("Generated SAS URL: " + sasUrl);

            // Pass SAS URL to Document Intelligence
            List<PageChunk> pageChunks = documentService.analyzeDocument(sasUrl);
            pageChunks.forEach(chunk -> logger.info(chunk.getPageNumber() + " " + chunk.getText()));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing blob: " + name + " | " + e.getMessage(), e);
        }
    }
}
