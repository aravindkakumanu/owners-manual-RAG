package com.rag.ownermanual.service;

import com.rag.ownermanual.domain.ParsedPage;
import com.rag.ownermanual.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fetches a remote document and turns it into structured page content the chunker can consume.
 */
@Service
public class RemoteDocumentParser {

    private static final Logger log = LoggerFactory.getLogger(RemoteDocumentParser.class);

    private final RestClient restClient;

    public RemoteDocumentParser(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetch the document at the given URL and parse it into a list of ParsedPage instances.
     * @param documentUrl URL of the document to ingest
     * @return list of parsed pages, each with page number (when available) and text; never null
     * @throws DocumentProcessingException when the document cannot be fetched or parsed
     */
    public List<ParsedPage> fetchAndParse(String documentUrl) {
        if (documentUrl == null || documentUrl.isBlank()) {
            throw new IllegalArgumentException("documentUrl must be non-blank");
        }

        byte[] pdfBytes = fetchPdfBytes(documentUrl);
        List<Document> documents = readPdfDocuments(pdfBytes);
        List<ParsedPage> pages = toParsedPages(documents);

        log.info("Parsed PDF from URL {} into {} non-empty page(s).", maskUrlForLog(documentUrl), pages.size());
        return pages;
    }

    private byte[] fetchPdfBytes(String documentUrl) {
        try {
            byte[] body = restClient.get()
                    .uri(documentUrl)
                    .retrieve()
                    .body(byte[].class);

            if (body == null || body.length == 0) {
                String message = "Failed to fetch document: empty response body";
                log.warn("{} url={}", message, maskUrlForLog(documentUrl));
                throw new DocumentProcessingException(message);
            }

            return body;
        } catch (RestClientException ex) {
            String message = "Failed to fetch document from URL";
            log.warn("{} url={}", message, maskUrlForLog(documentUrl), ex);
            throw new DocumentProcessingException(message, ex);
        }
    }

    private List<Document> readPdfDocuments(byte[] pdfBytes) {
        Resource resource = new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return "remote.pdf";
            }
        };

        try {
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, config);
            return reader.read();
        } catch (Exception ex) {
            String message = "Failed to parse PDF content";
            log.warn(message, ex);
            throw new DocumentProcessingException(message, ex);
        }
    }

    /**
     * Maps Spring AI Document instances (which carry text + metadata) to our internal ParsedPage domain type.
     */
    private List<ParsedPage> toParsedPages(List<Document> documents) {
        List<ParsedPage> pages = new ArrayList<>();
        if (documents == null) {
            return pages;
        }

        for (Document doc : documents) {
            if (doc == null || !doc.isText()) {
                continue;
            }

            String text = doc.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            Map<String, Object> metadata = doc.getMetadata();
            Integer startPage = toInteger(metadata != null
                    ? metadata.get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER)
                    : null);

            pages.add(new ParsedPage(startPage, text, null));
        }

        return pages;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String maskUrlForLog(String url) {
        if (url == null) {
            return null;
        }
        int queryIndex = url.indexOf('?');
        String base = (queryIndex >= 0) ? url.substring(0, queryIndex) : url;
        if (base.length() > 120) {
            base = base.substring(0, 120) + "...";
        }
        return base;
    }
}
