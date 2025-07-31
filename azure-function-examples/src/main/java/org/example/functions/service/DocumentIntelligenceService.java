package org.example.functions.service;

import java.util.ArrayList;
import java.util.List;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentLine;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentPage;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.polling.SyncPoller;

/**
 * Service for processing documents using Azure Document Intelligence.
 * Handles document analysis and text extraction.
 */
public class DocumentIntelligenceService {
    private static final String MODEL_ID = "prebuilt-layout";
    private final DocumentAnalysisClient documentAnalysisClient;
    private final ClientLogger logger = new ClientLogger(DocumentIntelligenceService.class);

    /**
     * Creates a new instance of DocumentIntelligenceService.
     *
     * @param endpoint The Azure Document Intelligence endpoint
     * @param apiKey   The Azure Document Intelligence API key
     * @throws IllegalArgumentException if endpoint or apiKey is null or empty
     */
    public DocumentIntelligenceService(String endpoint, String apiKey) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Document Intelligence endpoint cannot be null or empty");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Document Intelligence API key cannot be null or empty");
        }

        this.documentAnalysisClient = new DocumentAnalysisClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
    }

    /**
     * Analyzes a document and extracts its text content.
     *
     * @param content The document content as a byte array
     * @return List of extracted text lines from the document
     * @throws DocumentProcessingException if there's an error processing the document
     */
    public List<String> analyzeDocument(byte[] content) throws DocumentProcessingException {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Document content cannot be null or empty");
        }

        try {
            BinaryData documentData = BinaryData.fromBytes(content);
            //
            SyncPoller<OperationResult, AnalyzeResult> poller =
                    documentAnalysisClient.beginAnalyzeDocument("prebuilt-layout", documentData);

            AnalyzeResult result = poller.getFinalResult();

            return extractTextLines(result);

        } catch (Exception e) {
            String errorMsg = "Failed to process document: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new DocumentProcessingException(errorMsg, e);
        }
    }

    /**
     * Extracts text lines from the analysis result.
     */
    private List<String> extractTextLines(AnalyzeResult result) {
        List<String> lines = new ArrayList<>();

        if (result == null || result.getPages() == null) {
            return lines;
        }

        for (DocumentPage page : result.getPages()) {
            if (page.getLines() != null) {
                for (DocumentLine line : page.getLines()) {
                    if (line != null && line.getContent() != null) {
                        lines.add(line.getContent());
                    }
                }
            }
        }
        return lines;
    }
}


