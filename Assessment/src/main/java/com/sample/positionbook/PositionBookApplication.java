package com.sample.positionbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Position Book API", version = "1.0.0",
        description = "Process BUY, SELL and CANCEL events; inspect quantities and full event history. Data resets on restart."))
public class PositionBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(PositionBookApplication.class, args);
    }
}
