package com.paulrule.mathapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Service availability (UC-001), through Actuator's health endpoint, and the end-to-end Calculation smoke test
 * (UC-002).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MathApiApplicationTest {

    private static final String UP = "{\"status\":\"UP\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void answersAvailableWhenStarted() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(UP));
    }

    @Test
    void answersNotAvailableWhileRefusingTraffic() throws Exception {
        try {
            AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("{\"status\":\"OUT_OF_SERVICE\"}"));

            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(UP));
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @Test
    void availabilityAnswerHasOnlyStatus() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void doesNotExposeOtherManagementEndpoints() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
    }

    @Test
    void answersWithoutCredentials() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void repeatedChecksGiveTheSameAnswer() throws Exception {
        for (int check = 0; check < 2; check++) {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(UP));
        }
    }

    @Test
    void performsAnAdditionEndToEnd() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"ADD\",\"a\":\"0.1\",\"b\":\"0.2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("0.3"));
    }
}
