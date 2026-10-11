package com.sample.positionbook.repo;

import com.sample.positionbook.entity.EventType;
import com.sample.positionbook.entity.PositionKey;
import com.sample.positionbook.entity.Trade;
import com.sample.positionbook.entity.TradeEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Owns database access and maps rows to domain entities. */
@Repository
public class PositionBookRepository {
    private final JdbcTemplate jdbc;
    public PositionBookRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Called inside a service transaction; lock held through commit.
    public void lockBook() {
        jdbc.queryForObject("SELECT id FROM book_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    public Optional<Trade> findTradeById(long id) {
        List<Trade> trades = jdbc.query("SELECT * FROM trades WHERE event_id = ?",
                (rs, row) -> new Trade(rs.getLong("event_id"), EventType.valueOf(rs.getString("event_type")),
                        rs.getString("trading_account"), rs.getString("security_id"), rs.getLong("quantity"), rs.getBoolean("cancelled")), id);
        return trades.stream().findFirst();
    }

    public void saveTrade(Trade trade) {
        jdbc.update("INSERT INTO trades(event_id, event_type, trading_account, security_id, quantity, cancelled) VALUES (?, ?, ?, ?, ?, ?)",
                trade.getId(), trade.getType().name(), trade.getTradingAccount(), trade.getSecurityId(), trade.getQuantity(), trade.isCancelled());
    }

    public void cancelTrade(long id) {
        jdbc.update("UPDATE trades SET cancelled = TRUE WHERE event_id = ?", id);
    }

    public void saveEvent(TradeEvent event) {
        jdbc.update("INSERT INTO event_history(event_id, event_type, trading_account, security_id, quantity) VALUES (?, ?, ?, ?, ?)",
                event.getId(), event.getType().name(), event.getTradingAccount(), event.getSecurityId(), event.getQuantity());
    }

    public List<TradeEvent> findHistory(String account, String security) {
        return jdbc.query("SELECT * FROM event_history WHERE trading_account = ? AND security_id = ? ORDER BY sequence_id",
                (rs, row) -> new TradeEvent(rs.getLong("event_id"), EventType.valueOf(rs.getString("event_type")),
                        rs.getString("trading_account"), rs.getString("security_id"), rs.getLong("quantity")), account, security);
    }

    public BigDecimal findQuantity(String account, String security) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN event_type = 'BUY' THEN CAST(quantity AS DECIMAL(38,0)) ELSE -CAST(quantity AS DECIMAL(38,0)) END), 0) FROM trades WHERE trading_account = ? AND security_id = ? AND cancelled = FALSE",
                BigDecimal.class, account, security);
    }

    public List<PositionKey> findPositionKeys() {
        return jdbc.query("SELECT DISTINCT trading_account, security_id FROM trades ORDER BY trading_account, security_id",
                (rs, row) -> new PositionKey(rs.getString(1), rs.getString(2)));
    }
}
