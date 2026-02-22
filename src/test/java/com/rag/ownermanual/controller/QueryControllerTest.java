package com.rag.ownermanual.controller;

import com.rag.ownermanual.dto.query.Citation;
import com.rag.ownermanual.dto.query.QueryResponse;
import com.rag.ownermanual.service.QueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(QueryController.class)
@Import(GlobalExceptionHandler.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryService queryService;

    @Test
    @DisplayName("Valid request returns 200 with answer and citations")
    void validRequest_returns200WithAnswerAndCitations() throws Exception {
        QueryResponse response = QueryResponse.of(
                "Use the oil type recommended in the manual.",
                List.of(new Citation("chunk-1", "Engine", "Use 5W-30...", 42))
        );
        when(queryService.query(eq("What oil should I use?"), eq(null)))
                .thenReturn(response);

        ResultActions result = mockMvc.perform(post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"What oil should I use?\"}"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Use the oil type recommended in the manual."))
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations.length()").value(1))
                .andExpect(jsonPath("$.citations[0].chunkId").value("chunk-1"))
                .andExpect(jsonPath("$.citations[0].section").value("Engine"));

        verify(queryService).query("What oil should I use?", null);
    }

    @Test
    @DisplayName("Valid request with vehicleModel passes it to service")
    void validRequest_withVehicleModel_passesToService() throws Exception {
        when(queryService.query(eq("Tire pressure?"), eq("Sedan-2024")))
                .thenReturn(QueryResponse.of("Check the door jamb.", List.of()));

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Tire pressure?\", \"vehicleModel\": \"Sedan-2024\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Check the door jamb."));

        verify(queryService).query("Tire pressure?", "Sedan-2024");
    }

    @Test
    @DisplayName("Missing text returns 400 with structured error body (no stack trace)")
    void missingText_returns400StructuredError() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("text")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem(containsString("required"))));
        // No stack trace in response
        String body = result.andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("at com.", "Exception", "Caused by");
    }

    @Test
    @DisplayName("Blank text returns 400 with field error")
    void blankText_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("text")));
    }

    @Test
    @DisplayName("Text exceeding max length returns 400")
    void textTooLong_returns400() throws Exception {
        String longText = "a".repeat(8193);
        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + longText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("text")));
    }
}
