package com.rag.ownermanual.service;

import com.rag.ownermanual.config.QueryProperties;
import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.dto.query.Citation;
import com.rag.ownermanual.dto.query.QueryResponse;
import com.rag.ownermanual.repository.VectorStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the read path: search → LLM → answer with citations.
 */
@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private static final String SYSTEM_INSTRUCTION =
            "You are an assistant that answers questions using only the provided manual excerpts. "
                    + "Base your answer strictly on the excerpts; if the excerpts do not contain relevant "
                    + "information, say so. Do not invent details.";

    /** Delimiter between excerpt blocks so the model can distinguish sources. */
    private static final String EXCERPT_DELIMITER = "\n---\n";

    private static final String NO_CHUNKS_ANSWER = "No relevant sections found.";

    private final VectorStoreRepository vectorStoreRepository;
    private final QueryProperties queryProperties;
    private final ChatClient chatClient;

    public QueryService(VectorStoreRepository vectorStoreRepository,
                        QueryProperties queryProperties,
                        ChatClient.Builder chatClientBuilder) {
        this.vectorStoreRepository = Objects.requireNonNull(vectorStoreRepository, "vectorStoreRepository");
        this.queryProperties = Objects.requireNonNull(queryProperties, "queryProperties");
        this.chatClient = Objects.requireNonNull(chatClientBuilder, "chatClientBuilder").build();
    }

    /**
     * Search the vector store for chunks relevant to the query, optionally filtered by vehicle model.
     * @param queryText    User query text (embedded by the repository).
     * @param vehicleModel Optional filter; null or blank means no filter.
     * @return List of chunks ordered by similarity; never null, may be empty.
     */
    public List<Chunk> searchChunks(String queryText, String vehicleModel) {
        // Normalize blank to null so the repository contract is clear: null/blank = no filter.
        String normalizedModel = (vehicleModel != null && !vehicleModel.isBlank()) ? vehicleModel : null;
        int topK = queryProperties.getTopK();
        return vectorStoreRepository.search(queryText, normalizedModel, topK);
    }

    /**
     * Full RAG flow: search chunks → build prompt → call LLM → return answer with citations.
     * @param queryText    User question.
     * @param vehicleModel Optional filter; null or blank means no filter.
     * @return QueryResponse with answer and citations.
     */
    public QueryResponse query(String queryText, String vehicleModel) {
        List<Chunk> chunks = searchChunks(queryText, vehicleModel);

        if (chunks == null || chunks.isEmpty()) {
            log.debug("Query returned no chunks; returning no-relevant-sections message. query={}", maskForLog(queryText));
            return QueryResponse.of(NO_CHUNKS_ANSWER, null);
        }

        log.info("Query returned {} chunk(s), chunkIds={}; building prompt and calling LLM.",
                chunks.size(), chunks.stream().map(Chunk::id).toList());

        // Cap total context to avoid token overrun (context-window limit). We use the *included*
        // chunks for both the prompt and citations so citations match what the model actually saw.
        List<Chunk> included = truncateToContextLimit(chunks, queryProperties.getMaxContextChars());
        String userContent = buildUserMessage(queryText, included);

        String answer = chatClient.prompt()
                .system(s -> s.text(SYSTEM_INSTRUCTION))
                .user(userContent)
                .call()
                .content();

        // Citations = one per chunk we sent to the LLM; enables "which sections supported this answer?"
        List<Citation> citations = buildCitations(included);
        log.info("Citations returned: {} (from {} chunks).", citations.size(), included.size());

        return QueryResponse.of(answer != null && !answer.isBlank() ? answer : "", citations);
    }

    private String buildUserMessage(String queryText, List<Chunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(queryText).append("\n\n");
        sb.append("Manual excerpts (use only these to answer):\n");
        for (Chunk c : chunks) {
            sb.append("[section: ").append(c.section() != null ? c.section() : "").append("; page: ")
                    .append(c.page() != null ? c.page() : "").append("]\n");
            sb.append(c.text()).append(EXCERPT_DELIMITER);
        }
        return sb.toString();
    }

    private List<Chunk> truncateToContextLimit(List<Chunk> chunks, int maxChars) {
        int total = 0;
        List<Chunk> result = new ArrayList<>();
        for (Chunk c : chunks) {
            int len = c.text() != null ? c.text().length() : 0;
            if (total + len > maxChars && !result.isEmpty()) {
                break;
            }
            result.add(c);
            total += len;
        }
        return result;
    }

    private List<Citation> buildCitations(List<Chunk> chunks) {
        List<Citation> out = new ArrayList<>(chunks.size());
        for (Chunk c : chunks) {
            String snippet = truncateSnippet(c.text(), 200);
            out.add(new Citation(c.id(), c.section(), snippet, c.page()));
        }
        return out;
    }

    /** Truncates text for citation snippet; null-safe. */
    private static String truncateSnippet(String text, int maxLen) {
        if (text == null) return null;
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    /**
     * Truncates query for log lines so we don't dump long or sensitive input.
     */
    private static String maskForLog(String query) {
        if (query == null) return null;
        return query.length() > 80 ? query.substring(0, 80) + "..." : query;
    }
}
