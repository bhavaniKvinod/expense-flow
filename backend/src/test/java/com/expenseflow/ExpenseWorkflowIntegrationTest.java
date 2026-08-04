package com.expenseflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createReportWithValidItem(String token) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Client trip\",\"purpose\":\"Onsite visit\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long reportId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String lineItem = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("date", LocalDate.now().minusDays(2).toString());
            put("category", "MEALS");
            put("amount", "20.00"); // at/under receipt threshold, no receipt needed
            put("currency", "USD");
            put("merchant", "Corner Cafe");
            put("notes", "Team lunch");
        }});
        mockMvc.perform(post("/api/reports/" + reportId + "/line-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineItem))
                .andExpect(status().isOk());
        return reportId;
    }

    @Test
    void fullHappyPath_submitApproveReimburse() throws Exception {
        String employee = login("employee@expenseflow.test");
        long reportId = createReportWithValidItem(employee);

        mockMvc.perform(post("/api/reports/" + reportId + "/submit")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));

        String manager = login("manager@expenseflow.test");
        mockMvc.perform(post("/api/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        String admin = login("admin@expenseflow.test");
        mockMvc.perform(post("/api/reports/" + reportId + "/reimburse")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentReference\":\"ACH-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REIMBURSED")));

        // Audit trail should contain the full lifecycle.
        mockMvc.perform(get("/api/reports/" + reportId + "/audit")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action", is("CREATED")))
                .andExpect(jsonPath("$[?(@.action=='REIMBURSED')]").isNotEmpty());
    }

    @Test
    void submitBlockedByPolicyViolation() throws Exception {
        String employee = login("employee@expenseflow.test");
        MvcResult created = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad report\",\"purpose\":\"x\"}"))
                .andReturn();
        long reportId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Over receipt threshold, no receipt -> should be blocked.
        String lineItem = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("date", LocalDate.now().minusDays(1).toString());
            put("category", "SOFTWARE");
            put("amount", "500.00");
            put("currency", "USD");
            put("merchant", "SaaS Vendor");
        }});
        mockMvc.perform(post("/api/reports/" + reportId + "/line-items")
                        .header("Authorization", "Bearer " + employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineItem))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations").isNotEmpty());

        // Report must remain a DRAFT.
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + employee))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void cannotApproveADraft() throws Exception {
        String employee = login("employee@expenseflow.test");
        long reportId = createReportWithValidItem(employee);

        String manager = login("manager@expenseflow.test");
        mockMvc.perform(post("/api/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isBadRequest());
    }

    @Test
    void employeeCannotAccessApprovalQueue() throws Exception {
        String employee = login("employee@expenseflow.test");
        mockMvc.perform(get("/api/approvals")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myReportsCanBeFilteredByStatus() throws Exception {
        String employee = login("dev@expenseflow.test");

        // One report we submit (-> SUBMITTED) and one we leave as a DRAFT.
        long submittedId = createReportWithValidItem(employee);
        mockMvc.perform(post("/api/reports/" + submittedId + "/submit")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));

        long draftId = createReportWithValidItem(employee);

        // Filtering by DRAFT returns only drafts, and includes ours (not the submitted one).
        mockMvc.perform(get("/api/reports").param("status", "DRAFT").param("size", "100")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("DRAFT"))))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) draftId)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) submittedId))));

        // Filtering by SUBMITTED returns only submitted reports, including ours.
        mockMvc.perform(get("/api/reports").param("status", "SUBMITTED").param("size", "100")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("SUBMITTED"))))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) submittedId)));
    }
}
