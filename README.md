# FIX Trading System

A simple FIX-based server-client system implementing order processing functionality.

## Features

- FIX 4.4 protocol implementation
- Order validation and processing
- Heartbeat mechanism
- Execution reporting
- Reject handling

## Running the Application

1. Start the server independently
2. Start the client independently

## Configuration

- Server settings: `src/main/resources/server-config.properties`
- Client settings: `src/main/resources/client-config.properties`
- FIX dictionary: `config/FIX44.xml`
- Logging: `src/main/resources/logback.xml`

## Supported Order Types

The system supports NewOrderSingle (35=D) messages with the following validations:
- ClOrdID (11): Non-empty string
- Side (54): 1 (Buy) or 2 (Sell)
- Symbol (55): AAPL, GOOG, TSLA, or AMZN
- Price (44): Positive decimal
- Quantity (32): Positive integer

## Message Flow

1. Client sends NewOrderSingle
2. Server validates the order
3. Server responds with ExecutionReport (success) or Reject (failure)
4. Heartbeat messages every 30 seconds
