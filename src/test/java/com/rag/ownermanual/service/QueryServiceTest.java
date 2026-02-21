package com.rag.ownermanual.service;

import com.rag.ownermanual.config.QueryProperties;
import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.dto.query.Citation;
import com.rag.ownermanual.dto.query.QueryResponse;
import com.rag.ownermanual.repository.VectorStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private VectorStoreRepository vectorStoreRepository;

    @Mock
    private ChatModel chatModel;

    private QueryProperties queryProperties;
    private QueryService queryService;

    @BeforeEach
    void setUp() {
        queryProperties = new QueryProperties();
        queryProperties.setTopK(5);
        queryProperties.setMaxContextChars(8_000);
        queryService = new QueryService(vectorStoreRepository, queryProperties, ChatClient.builder(chatModel));
    }

    /** Service is a pass-through: returned list must be the same instance the repo returns. */
    @Test
    void searchChunks_returnsListFromRepository() {
        List<Chunk> expected = List.of(
                new Chunk("c1", "Oil change every 5000 miles.", "manual-1", "Model-X", "Maintenance", 10)
        );
        when(vectorStoreRepository.search(eq("How often oil change?"), isNull(), eq(5)))
                .thenReturn(expected);

        List<Chunk> result = queryService.searchChunks("How often oil change?", null);

        assertThat(result).isSameAs(expected);
        verify(vectorStoreRepository).search("How often oil change?", null, 5);
    }

    /** Non-blank vehicleModel must be passed to the repo so Qdrant can filter by metadata. */
    @Test
    void searchChunks_withVehicleModel_passesFilterAndUsesTopK() {
        List<Chunk> expected = List.of(
                new Chunk("c1", "Tire pressure 32 psi.", "manual-1", "Model-Y", "Tires", 5)
        );
        when(vectorStoreRepository.search(eq("tire pressure"), eq("Model-Y"), eq(5)))
                .thenReturn(expected);

        List<Chunk> result = queryService.searchChunks("tire pressure", "Model-Y");

        assertThat(result).isSameAs(expected);
        verify(vectorStoreRepository).search("tire pressure", "Model-Y", 5);
    }

    /** Blank/whitespace vehicleModel → null so repo does not apply a filter (per interface contract). */
    @Test
    void searchChunks_withBlankVehicleModel_passesNullToRepository() {
        when(vectorStoreRepository.search(anyString(), isNull(), anyInt())).thenReturn(List.of());

        queryService.searchChunks("query", "   ");

        verify(vectorStoreRepository).search(eq("query"), isNull(), eq(5));
    }

    @Test
    void searchChunks_usesConfiguredTopK() {
        queryProperties.setTopK(10);
        queryService = new QueryService(vectorStoreRepository, queryProperties, ChatClient.builder(chatModel));
        when(vectorStoreRepository.search(anyString(), isNull(), eq(10))).thenReturn(List.of());

        queryService.searchChunks("q", null);

        verify(vectorStoreRepository).search("q", null, 10);
    }


    /**
     * No chunks → short answer and empty citations; ChatClient must not be used (saves cost and avoids sending empty context).
     */
    @Test
    void query_withNoChunks_returnsNoRelevantSectionsAndEmptyCitations() {
        when(vectorStoreRepository.search(anyString(), isNull(), anyInt())).thenReturn(List.of());

        QueryResponse response = queryService.query("How often oil change?", null);

        assertThat(response.answer()).isEqualTo("No relevant sections found.");
        assertThat(response.citations()).isEmpty();
        verify(chatModel, never()).call(any(Prompt.class));
    }

    /**
     * With chunks: repo returns list → service uses ChatClient (which calls chatModel).
     */
    @Test
    void query_withChunks_returnsAnswerAndCitationsFromMockedChatClient() {
        List<Chunk> chunks = List.of(
                new Chunk("c1", "Oil change every 5000 miles.", "manual-1", "Model-X", "Maintenance", 10),
                new Chunk("c2", "Check tire pressure monthly.", "manual-1", "Model-X", "Tires", 5)
        );
        when(vectorStoreRepository.search(eq("How often oil change?"), isNull(), eq(5))).thenReturn(chunks);
        ChatResponse mockResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("You should change the oil every 5000 miles."))
        ));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        QueryResponse response = queryService.query("How often oil change?", null);

        assertThat(response.answer()).isEqualTo("You should change the oil every 5000 miles.");
        assertThat(response.citations()).hasSize(2);
        assertThat(response.citations().stream().map(Citation::chunkId).toList()).containsExactly("c1", "c2");
        assertThat(response.citations().get(0).section()).isEqualTo("Maintenance");
        assertThat(response.citations().get(0).page()).isEqualTo(10);
        assertThat(response.citations().get(0).snippet()).isEqualTo("Oil change every 5000 miles.");
        assertThat(response.citations().get(1).section()).isEqualTo("Tires");
        assertThat(response.citations().get(1).page()).isEqualTo(5);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        String userContent = promptCaptor.getValue().getUserMessage().getText();
        assertThat(userContent).contains("How often oil change?");
        assertThat(userContent).contains("Oil change every 5000 miles.");
        assertThat(userContent).contains("Check tire pressure monthly.");
    }
}
