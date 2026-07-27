package com.expenseflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks in the fixes for issue #5: list endpoints must issue a constant number of queries
 * regardless of how many reports match, and must be paginated. Each test creates its own
 * throwaway employee (rather than reusing a demo account shared with other test classes) so
 * report counts here can't be polluted by data other tests seed for the same user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerformanceTest {

    private static final AtomicInteger USER_SEQ = new AtomicInteger();
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    /** Creates a fresh EMPLOYEE user (no manager) via the admin API and logs in as them. */
    private String newEmployeeToken() throws Exception {
        String admin = login("admin@expenseflow.test");
        String email = "perf-test-" + USER_SEQ.incrementAndGet() + "@expenseflow.test";
        String body = objectMapper.writeValueAsString(new LinkedHashMap<>() {{
            put("name", "Perf Test User");
            put("email", email);
            put("password", PASSWORD);
            put("role", "EMPLOYEE");
        }});
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        return login(email);
    }

    private void createReportWithLineItem(String token, int index) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Report " + index + "\",\"purpose\":\"perf test\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long reportId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String lineItem = objectMapper.writeValueAsString(new LinkedHashMap<>() {{
            put("date", LocalDate.now().minusDays(1).toString());
            put("category", "MEALS");
            put("amount", "10.00");
            put("currency", "USD");
            put("merchant", "Merchant " + index);
            put("notes", "n/a");
        }});
        mockMvc.perform(post("/api/reports/" + reportId + "/line-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineItem))
                .andExpect(status().isOk());
    }

    private Statistics statistics() {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        return stats;
    }

    @Test
    void myReportsListIssuesConstantQueryCountRegardlessOfReportCount() throws Exception {
        String employee = newEmployeeToken();
        int reportCount = 12;
        for (int i = 0; i < reportCount; i++) {
            createReportWithLineItem(employee, i);
        }

        Statistics stats = statistics();
        stats.clear();

        mockMvc.perform(get("/api/reports").param("size", "50")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(reportCount)))
                .andExpect(jsonPath("$.totalElements", is(reportCount)));

        // One query for the page of summaries, one for the pagination count - constant no
        // matter how many reports exist, instead of the old ~2N (employee + lineItems per row).
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void myReportsListIsPaginated() throws Exception {
        String employee = newEmployeeToken();
        for (int i = 0; i < 5; i++) {
            createReportWithLineItem(employee, i);
        }

        mockMvc.perform(get("/api/reports").param("size", "2")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    void pageSizeIsCappedAtDocumentedMaximum() throws Exception {
        String employee = newEmployeeToken();

        mockMvc.perform(get("/api/reports").param("size", "10000")
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }
}
