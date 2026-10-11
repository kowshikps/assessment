package com.sample.positionbook.service;

import com.sample.positionbook.dto.EventDetail;
import com.sample.positionbook.dto.PositionResponse;
import com.sample.positionbook.dto.TradeEventRequest;
import com.sample.positionbook.entity.EventType;
import com.sample.positionbook.entity.Trade;
import com.sample.positionbook.entity.TradeEvent;
import com.sample.positionbook.exception.BookException;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import com.sample.positionbook.repo.PositionBookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PositionBookService {
    private final PositionBookRepository repository;
    public PositionBookService(PositionBookRepository repository) { this.repository = repository; }

    @Transactional
    public PositionResponse process(TradeEventRequest request) {
        if (request.id == null || request.id <= 0 || request.type == null) {
            throw new BookException(HttpStatus.BAD_REQUEST, "Positive id and type are required");
        }
        repository.lockBook();
        TradeEvent event;
        if (request.type == EventType.CANCEL) {
            Trade original = repository.findTradeById(request.id)
                    .orElseThrow(() -> new BookException(HttpStatus.NOT_FOUND, "Original trade event not found"));
            if (request.tradingAccount != null && !original.getTradingAccount().equals(request.tradingAccount)) {
                throw new BookException(HttpStatus.BAD_REQUEST, "Cancellation account must match original trade");
            }
            if (original.isCancelled()) {
                throw new BookException(HttpStatus.CONFLICT, "Trade has already been cancelled");
            }
            repository.cancelTrade(request.id);
            event = new TradeEvent(request.id, EventType.CANCEL, original.getTradingAccount(), original.getSecurityId(), 0);
        } else {
            validateIdentifier(request.tradingAccount, "tradingAccount");
            validateIdentifier(request.securityId, "securityId");
            if (request.quantity == null || request.quantity <= 0) {
                throw new BookException(HttpStatus.BAD_REQUEST, "quantity must be a positive whole number for BUY/SELL");
            }
            if (repository.findTradeById(request.id).isPresent()) {
                throw new BookException(HttpStatus.CONFLICT, "Trade event id already exists");
            }
            repository.saveTrade(new Trade(request.id, request.type, request.tradingAccount, request.securityId, request.quantity, false));
            event = new TradeEvent(request.id, request.type, request.tradingAccount, request.securityId, request.quantity);
        }
        repository.saveEvent(event);
        return snapshot(event.getTradingAccount(), event.getSecurityId());
    }

    @Transactional
    public PositionResponse get(String account, String security) {
        validateIdentifier(account, "tradingAccount");
        validateIdentifier(security, "securityId");
        repository.lockBook();
        return snapshot(account, security);
    }

    @Transactional
    public List<PositionResponse> list() {
        repository.lockBook();
        return repository.findPositionKeys().stream()
                .map(key -> snapshot(key.getTradingAccount(), key.getSecurityId())).collect(java.util.stream.Collectors.toList());
    }

    private PositionResponse snapshot(String account, String security) {
        List<TradeEvent> history = repository.findHistory(account, security);
        if (history.isEmpty()) throw new BookException(HttpStatus.NOT_FOUND, "Position not found");
        BigDecimal total = repository.findQuantity(account, security);
        try {
            return new PositionResponse(account, security, total.longValueExact(), history.stream().map(event -> new EventDetail(event.getId(), event.getType(),
                    event.getTradingAccount(), event.getSecurityId(), event.getQuantity())).collect(java.util.stream.Collectors.toList()));
        } catch (ArithmeticException ex) {
            throw new BookException(HttpStatus.BAD_REQUEST, "Resulting position quantity exceeds signed 64-bit range");
        }
    }

    private void validateIdentifier(String value, String name) {
        if (value == null || value.isBlank() || value.length() > 100 || !value.equals(value.trim())) {
            throw new BookException(HttpStatus.BAD_REQUEST, name + " must contain 1-100 characters with no surrounding whitespace");
        }
    }
}
