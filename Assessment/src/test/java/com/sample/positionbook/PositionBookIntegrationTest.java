package com.sample.positionbook;

import com.sample.positionbook.dto.PositionResponse;
import com.sample.positionbook.dto.TradeEventRequest;
import com.sample.positionbook.entity.EventType;
import com.sample.positionbook.exception.BookException;
import com.sample.positionbook.service.PositionBookService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class PositionBookIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PositionBookService service;

    @BeforeEach void clear() {
        jdbc.update("DELETE FROM event_history");
        jdbc.update("DELETE FROM trades");
    }

    private TradeEventRequest event(long id, EventType type, String account, String security, Long quantity) {
        TradeEventRequest request = new TradeEventRequest();
        request.id = id; request.type = type; request.tradingAccount = account;
        request.securityId = security; request.quantity = quantity;
        return request;
    }

    private void trade(long id, EventType type, String account, String security, long quantity) {
        service.process(event(id, type, account, security, quantity));
    }

    @Test void pdfExampleOneBuyingSecurities() {
        trade(1, EventType.BUY, "ACC1", "SEC1", 100);
        trade(2, EventType.BUY, "ACC1", "SEC1", 50);
        PositionResponse position = service.get("ACC1", "SEC1");
        assertThat(position.quantity).isEqualTo(150);
        assertThat(position.events).extracting(e -> e.id).containsExactly(1L, 2L);
    }

    @Test void pdfExampleTwoDifferentSecuritiesAndAccounts() {
        trade(3, EventType.BUY, "ACC1", "SEC1", 12);
        trade(4, EventType.BUY, "ACC1", "SECXYZ", 50);
        trade(5, EventType.BUY, "ACC2", "SECXYZ", 33);
        trade(6, EventType.BUY, "ACC1", "SEC1", 20);
        assertThat(service.get("ACC1", "SEC1").quantity).isEqualTo(32);
        assertThat(service.get("ACC1", "SECXYZ").quantity).isEqualTo(50);
        assertThat(service.get("ACC2", "SECXYZ").quantity).isEqualTo(33);
        assertThat(service.list()).hasSize(3);
        assertThat(service.get("ACC1", "SEC1").events).extracting(e -> e.id).containsExactly(3L, 6L);
    }

    @Test void pdfExampleThreeBuyingAndSelling() {
        trade(10, EventType.BUY, "ACC1", "SEC1", 100);
        trade(11, EventType.SELL, "ACC1", "SEC1", 50);
        assertThat(service.get("ACC1", "SEC1").quantity).isEqualTo(50);
        assertThat(service.get("ACC1", "SEC1").events).extracting(e -> e.type).containsExactly(EventType.BUY, EventType.SELL);
    }

    @Test void pdfExampleFourCancellationPreservesHistory() {
        trade(21, EventType.BUY, "ACC1", "SEC1", 100);
        trade(21, EventType.CANCEL, "ACC1", "SEC1", 0);
        trade(22, EventType.BUY, "ACC1", "SEC1", 5);
        PositionResponse position = service.get("ACC1", "SEC1");
        assertThat(position.quantity).isEqualTo(5);
        assertThat(position.events).extracting(e -> e.type).containsExactly(EventType.BUY, EventType.CANCEL, EventType.BUY);
        assertThat(position.events).extracting(e -> e.id).containsExactly(21L, 21L, 22L);
    }

    @Test void sellCanCreateNegativePositionAndCancellationReversesIt() {
        trade(1, EventType.SELL, "A", "S", 50);
        assertThat(service.get("A", "S").quantity).isEqualTo(-50);
        PositionResponse result = service.process(event(1, EventType.CANCEL, null, "ignored", -123L));
        assertThat(result.quantity).isZero();
        assertThat(result.events.get(1).securityId).isEqualTo("S");
        assertThat(result.events.get(1).quantity).isZero();
        assertThat(service.list()).hasSize(1);
    }

    @Test void duplicateIdsRejectedGloballyIncludingCancelledTrades() {
        trade(1, EventType.BUY, "A", "S", 10);
        assertThatThrownBy(() -> trade(1, EventType.BUY, "B", "X", 20)).isInstanceOf(BookException.class).hasMessage("Trade event id already exists");
        service.process(event(1, EventType.CANCEL, null, null, null));
        assertThatThrownBy(() -> trade(1, EventType.SELL, "A", "S", 10)).isInstanceOf(BookException.class);
        assertThat(service.get("A", "S").events).hasSize(2);
    }

    @Test void repeatedCancellationReturnsConflictWithoutChangingHistory() throws Exception {
        trade(1, EventType.BUY, "A", "S", 10);
        service.process(event(1, EventType.CANCEL, null, null, null));
        postJson("{\"id\":1,\"type\":\"CANCEL\"}", 409);
        assertThat(service.get("A", "S").events).hasSize(2);
    }

    @Test void unknownCancellationAndPositionReturn404() throws Exception {
        postJson("{\"id\":999,\"type\":\"CANCEL\"}", 404);
        mvc.perform(get("/api/positions/A/S")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Position not found"));
        assertThat(service.list()).isEmpty();
    }

    @Test void cancellationFromWrongAccountIsRejected() throws Exception {
        trade(1, EventType.BUY, "A", "S", 10);
        postJson("{\"id\":1,\"type\":\"CANCEL\",\"tradingAccount\":\"B\"}", 400);
        assertThat(service.get("A", "S").quantity).isEqualTo(10);
        assertThat(service.get("A", "S").events).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "null", "{\"id\":0,\"type\":\"BUY\"}", "{\"id\":-1,\"type\":\"BUY\"}",
        "{\"id\":1,\"type\":\"OTHER\"}", "{\"id\":1,\"type\":\"buy\"}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\"A\",\"securityId\":\"S\",\"quantity\":0}",
        "{\"id\":1,\"type\":\"SELL\",\"tradingAccount\":\"A\",\"securityId\":\"S\",\"quantity\":-2}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\"A\",\"securityId\":\"S\"}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\" \",\"securityId\":\"S\",\"quantity\":1}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\"A \",\"securityId\":\"S\",\"quantity\":1}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\"A\",\"quantity\":1}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\"A\",\"securityId\":\"\",\"quantity\":1}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\"A\",\"securityId\":\"S\",\"quantity\":1.5}",
        "{\"id\":1,\"type\":\"BUY\",\"tradingAccount\":\"A\",\"securityId\":\"S\",\"quantity\":\"10\"}",
        "{\"id\":1,\"type\":\"CANCEL\",\"unexpected\":true}", "{malformed"
    })
    void invalidInputsAreRejectedAndLeaveDatabaseUntouched(String json) throws Exception {
        postJson(json, 400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trades", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_history", Long.class)).isZero();
    }

    @Test void identifierLengthAndGetValidation() throws Exception {
        assertThatThrownBy(() -> trade(1, EventType.BUY, "A".repeat(101), "S", 1)).isInstanceOf(BookException.class);
        assertThatThrownBy(() -> trade(1, EventType.BUY, "A", "S".repeat(101), 1)).isInstanceOf(BookException.class);
        assertThatThrownBy(() -> service.get(" ", "S")).isInstanceOf(BookException.class);
        assertThatThrownBy(() -> service.get("A", null)).isInstanceOf(BookException.class);
        trade(1, EventType.BUY, "A".repeat(100), "S".repeat(100), 1);
        assertThat(service.list()).hasSize(1);
    }

    @Test void overflowRollsBackTradeAndHistory() {
        trade(1, EventType.BUY, "A", "S", Long.MAX_VALUE);
        assertThatThrownBy(() -> trade(2, EventType.BUY, "A", "S", 1)).isInstanceOf(BookException.class).hasMessageContaining("64-bit");
        assertThat(service.get("A", "S").quantity).isEqualTo(Long.MAX_VALUE);
        assertThat(service.get("A", "S").events).hasSize(1);
        trade(2, EventType.SELL, "A", "S", 1);
        assertThat(service.get("A", "S").quantity).isEqualTo(Long.MAX_VALUE - 1);
    }

    @Test void cancellationOverflowRollsBackCancellation() {
        trade(1, EventType.SELL, "A", "S", Long.MAX_VALUE);
        trade(2, EventType.BUY, "A", "S", Long.MAX_VALUE);
        trade(3, EventType.BUY, "A", "S", 1);
        assertThatThrownBy(() -> service.process(event(1, EventType.CANCEL, null, null, null))).isInstanceOf(BookException.class);
        assertThat(service.get("A", "S").quantity).isEqualTo(1);
        assertThat(service.get("A", "S").events).hasSize(3);
        assertThat(jdbc.queryForObject("SELECT cancelled FROM trades WHERE event_id = 1", Boolean.class)).isFalse();
    }

    @Test void concurrentEventsDoNotLoseUpdates() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (long i = 1; i <= 40; i++) {
                final long id = i;
                tasks.add(() -> { trade(id, EventType.BUY, "A", "S", 1); return null; });
            }
            for (Future<Void> future : pool.invokeAll(tasks)) future.get(15, TimeUnit.SECONDS);
            assertThat(service.get("A", "S").quantity).isEqualTo(40);
            assertThat(service.get("A", "S").events).hasSize(40);
        } finally { pool.shutdownNow(); }
    }

    @Test void restFlowReturnsPositionAndHistory() throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":21,\"type\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(100));
        mvc.perform(get("/api/positions/ACC1/SEC1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].id").value(21));
        mvc.perform(get("/api/positions")).andExpect(status().isOk()).andExpect(jsonPath("$[0].tradingAccount").value("ACC1"));
    }

    @Test void openApiAndSwaggerAreAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/events'].post").exists())
                .andExpect(jsonPath("$.paths['/api/positions/{tradingAccount}/{securityId}'].get").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private void postJson(String json, int expectedStatus) throws Exception {
        mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.path").value("/api/events"));
    }
}
