package com.sample.positionbook.controller;

import com.sample.positionbook.dto.PositionResponse;
import com.sample.positionbook.dto.TradeEventRequest;
import com.sample.positionbook.service.PositionBookService;

import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api")
@Tag(name = "Position Book")
public class PositionBookController {
    private final PositionBookService service;
    public PositionBookController(PositionBookService service) { this.service = service; }

    @PostMapping("/events")
    @Operation(summary = "Process a trade event", description = "BUY adds quantity, SELL subtracts, CANCEL reverses the original trade with the same ID. Returns the updated position and history.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Event accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid input or quantity overflow"),
            @ApiResponse(responseCode = "404", description = "Original trade not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate trade or already cancelled")})
    public PositionResponse process(@Valid @RequestBody TradeEventRequest request) { return service.process(request); }

    @GetMapping("/positions/{tradingAccount}/{securityId}")
    @Operation(summary = "Retrieve a position and its event history")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Position found"),
            @ApiResponse(responseCode = "400", description = "Invalid identifier"),
            @ApiResponse(responseCode = "404", description = "Position not found")})
    public PositionResponse get(@PathVariable("tradingAccount") String tradingAccount, @PathVariable("securityId") String securityId) {
        return service.get(tradingAccount, securityId);
    }

    @GetMapping("/positions")
    @Operation(summary = "List all positions, including zero positions, ordered by account and security")
    public List<PositionResponse> list() { return service.list(); }
}
