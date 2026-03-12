package com.rag.ownermanual.service;

import com.rag.ownermanual.config.QueryProperties;
import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.dto.query.Citation;
import com.rag.ownermanual.dto.query.QueryResponse;
import com.rag.ownermanual.exception.DownstreamLlmException;
import com.rag.ownermanual.exception.DownstreamVectorStoreException;
import com.rag.ownermanual.repository.VectorStoreRepository;
import com.rag.ownermanual.resilience.ResilienceService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the read path: embed → search → LLM → answer with citations.
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
    private final ResilienceService resilienceService;

    private final MeterRegistry meterRegistry;

    public QueryService(VectorStoreRepository vectorStoreRepository,
                        QueryProperties queryProperties,
                        ChatClient.Builder chatClientBuilder,
                        ResilienceService resilienceService,
                        MeterRegistry meterRegistry) {
        this.vectorStoreRepository = Objects.requireNonNull(vectorStoreRepository, "vectorStoreRepository");
        this.queryProperties = Objects.requireNonNull(queryProperties, "queryProperties");
        this.chatClient = Objects.requireNonNull(chatClientBuilder, "chatClientBuilder").build();
        this.resilienceService = Objects.requireNonNull(resilienceService, "resilienceService");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
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

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Chunk> result = vectorStoreRepository.search(queryText, normalizedModel, topK);
            sample.stop(Timer.builder("query.vector.search.latency")
                    .description("Latency of vector store search calls from QueryService")
                    .tags(Tags.of(
                            "status", "success",
                            "vehicleModel", tagValue(normalizedModel)))
                    .register(meterRegistry));
            return result;
        } catch (RuntimeException ex) {
            sample.stop(Timer.builder("query.vector.search.latency")
                    .description("Latency of vector store search calls from QueryService")
                    .tags(Tags.of(
                            "status", "error",
                            "vehicleModel", tagValue(normalizedModel)))
                    .register(meterRegistry));
            log.error("Vector store search failed. query='{}', vehicleModel='{}'.",
                    maskForLog(queryText), normalizedModel, ex);
            throw new DownstreamVectorStoreException("Vector store search failed", ex);
        }
    }

    /**
     * Full RAG flow: search chunks → build prompt → call LLM → return answer with citations.
     * @param queryText    User question.
     * @param vehicleModel Optional filter; null or blank means no filter.
     * @return QueryResponse with answer and citations.
     */
    public QueryResponse query(String queryText, String vehicleModel) {
        String normalizedModel = (vehicleModel != null && !vehicleModel.isBlank()) ? vehicleModel : null;
        Timer.Sample querySample = Timer.start(meterRegistry);

        log.info("Received query; starting retrieval and LLM call. queryPreview='{}', vehicleModel='{}', topK={}",
                maskForLog(queryText), vehicleModel, queryProperties.getTopK());

        List<Chunk> chunks = searchChunks(queryText, normalizedModel);

        if (chunks == null || chunks.isEmpty()) {
            log.debug("Query returned no chunks; returning no-relevant-sections message. query={}", maskForLog(queryText));
            incrementQueryMetrics("no_chunks", normalizedModel, querySample);
            return QueryResponse.of(NO_CHUNKS_ANSWER, null);
        }

        log.info("Query returned {} chunk(s), chunkIds={}; building prompt and calling LLM.",
                chunks.size(), chunks.stream().map(Chunk::id).toList());

        // Cap total context to avoid token overrun (context-window limit). We use the *included*
        // chunks for both the prompt and citations so citations match what the model actually saw.
        List<Chunk> included = truncateToContextLimit(chunks, queryProperties.getMaxContextChars());
        String userContent = buildUserMessage(queryText, included);

        String answer;
        Timer.Sample llmSample = Timer.start(meterRegistry);
        try {
            answer = resilienceService.executeWithTimeLimit("llm", () -> chatClient.prompt()
                    .system(s -> s.text(SYSTEM_INSTRUCTION))
                    .user(userContent)
                    .call()
                    .content());
            llmSample.stop(Timer.builder("query.llm.latency")
                    .description("Latency of LLM calls from QueryService")
                    .tags(Tags.of(
                            "status", "success",
                            "vehicleModel", tagValue(normalizedModel)))
                    .register(meterRegistry));
        } catch (RuntimeException ex) {
            llmSample.stop(Timer.builder("query.llm.latency")
                    .description("Latency of LLM calls from QueryService")
                    .tags(Tags.of(
                            "status", "error",
                            "vehicleModel", tagValue(normalizedModel)))
                    .register(meterRegistry));
            log.error("LLM answer generation failed. query='{}', includedChunks={}.",
                    maskForLog(queryText), included.size(), ex);
            incrementQueryMetrics("error", normalizedModel, querySample);
            throw new DownstreamLlmException("LLM answer generation failed", ex);
        }

        // Citations = one per chunk we sent to the LLM; enables "which sections supported this answer?"
        List<Citation> citations = buildCitations(included);
        log.info("Citations returned: {} (from {} chunks).", citations.size(), included.size());

        incrementQueryMetrics("success", normalizedModel, querySample);
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

    private void incrementQueryMetrics(String status, String vehicleModel, Timer.Sample sample) {
        Tags tags = Tags.of(
                "status", status,
                "vehicleModel", tagValue(vehicleModel)
        );
        Counter.builder("query.requests")
                .description("Total query requests handled by QueryService")
                .tags(tags)
                .register(meterRegistry)
                .increment();
        sample.stop(Timer.builder("query.latency")
                .description("End-to-end latency for QueryService.query")
                .tags(tags)
                .register(meterRegistry));

        if (!"no_chunks".equals(status)) {
            Counter.builder("query.llm.calls")
                    .description("Total LLM calls initiated by QueryService")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    private static String tagValue(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
    }

    /**
     * Truncates query for log lines so we don't dump long or sensitive input.
     */
    private static String maskForLog(String query) {
        if (query == null) return null;
        return query.length() > 80 ? query.substring(0, 80) + "..." : query;
    }
}
