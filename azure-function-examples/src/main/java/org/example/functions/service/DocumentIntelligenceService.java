package org.example.functions.service;

import java.util.ArrayList;
import java.util.List;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentLine;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentPage;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.polling.SyncPoller;
import org.example.functions.model.PageChunk;

/**
 * Service for processing documents using Azure Document Intelligence.
 * Handles document analysis and text extraction.
 */
public class DocumentIntelligenceService {
    private static final String MODEL_ID = "prebuilt-layout";
    private final DocumentAnalysisClient documentAnalysisClient;
    private DocumentIntelligenceClient documentIntelligenceClient ;
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

        this.documentIntelligenceClient=new DocumentIntelligenceClientBuilder()
                .endpoint(System.getenv(endpoint))
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
    }

    /**
     * Analyzes a document and extracts its text content.
     *
     * @param sasUrl The document content as a byte array
     * @return List of extracted text lines from the document
     * @throws DocumentProcessingException if there's an error processing the document
     */
    public void analyzeDocument(String sasUrl) throws DocumentProcessingException {


        logger.info("Starting analysis for SAS URL: " + sasUrl);
        try {
            //
            SyncPoller<OperationResult, AnalyzeResult> poller =
                    documentAnalysisClient.beginAnalyzeDocumentFromUrl("prebuilt-read", sasUrl);

            AnalyzeResult result = poller.getFinalResult();

            List<PageChunk> pageChunks = chunkByPage(result);
            System.out.println(pageChunks);


        } catch (Exception e) {
            String errorMsg = "Failed to process document: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new DocumentProcessingException(errorMsg, e);
        }
    }

    public List<PageChunk> chunkByPage(AnalyzeResult result) {
        List<PageChunk> chunks = new ArrayList<>();

        int pageIndex = 1;
        for (DocumentPage page : result.getPages()) {
            StringBuilder sb = new StringBuilder();

            if (page.getLines() != null) {
                for (DocumentLine line : page.getLines()) {
                    sb.append(line.getContent()).append(" ");
                }
            }

            chunks.add(new PageChunk(pageIndex, sb.toString().trim()));
            pageIndex++;
        }

        return chunks;
    }
}


