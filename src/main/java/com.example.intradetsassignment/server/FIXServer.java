package com.example.intradetsassignment.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.IncorrectTagValue;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.RefSeqNum;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.Text;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.Reject;

public class FIXServer extends MessageCracker implements Application {

  private static final Logger logger = LoggerFactory.getLogger(FIXServer.class);
  private static final String SERVER_PROPERTIES_FILENAME = "server-config.properties";
  private static final String[] VALID_SYMBOLS = {"AAPL", "GOOG", "TSLA", "AMZN"};

  public static void main(String[] args) {
    try {
      SessionSettings settings = new SessionSettings(SERVER_PROPERTIES_FILENAME);
      Application application = new FIXServer();
      MessageStoreFactory storeFactory = new FileStoreFactory(settings);
      LogFactory logFactory = new FileLogFactory(settings);
      MessageFactory messageFactory = new DefaultMessageFactory();

      SocketAcceptor acceptor = new SocketAcceptor(
          application, storeFactory, settings, logFactory, messageFactory);

      acceptor.start();
      logger.info("FIX Server started.");

      while (true) {
        Thread.sleep(1000);
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  @Override
  public void onCreate(SessionID sessionId) {
  }

  @Override
  public void onLogon(SessionID sessionId) {
    logger.info("Client logged on: {}", sessionId);
  }

  @Override
  public void onLogout(SessionID sessionId) {
    logger.info("Client logged out: {}", sessionId);
  }

  @Override
  public void toAdmin(Message message, SessionID sessionId) {
  }

  @Override
  public void fromAdmin(Message message, SessionID sessionId) {
    logger.info("Received heartbeat from client");
    logger.debug(String.valueOf(message));
  }

  @Override
  public void toApp(Message message, SessionID sessionId) {
  }

  @Override
  public void fromApp(Message message, SessionID sessionId)
      throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
    logger.info("Received message from client");
    logger.debug(String.valueOf(message));

    try {
      crack(message, sessionId);
    } catch (UnsupportedMessageType e) {
      throw new UnsupportedMessageType();
    }
  }

  @Override
  public void onMessage(NewOrderSingle order, SessionID sessionId)
      throws FieldNotFound {

    try {
      validateNewOrderSingle(order);

      ExecutionReport executionReport = new ExecutionReport(
          new OrderID("sample-order-id"),
          new ExecID(generateExecID()),
          new ExecType(ExecType.NEW),
          new OrdStatus(OrdStatus.NEW),
          order.getSide(),
          new LeavesQty(),
          new CumQty(),
          new AvgPx()
      );
      executionReport.set(new Symbol(order.getString(55)));

      logger.info("Sending execution report");
      logger.info(String.valueOf(executionReport));

      Session.sendToTarget(executionReport, sessionId);
    } catch (ValidationException e) {
      logger.error(e.getMessage());
      sendReject(sessionId, e.getMessage());
    } catch (SessionNotFound e) {
      logger.error("Session not found: " + sessionId);
    }
  }

  private void validateNewOrderSingle(NewOrderSingle order) throws ValidationException, FieldNotFound {
    logger.info("-------------- Starting validation --------------");
    if (order.getClOrdID().getValue().isEmpty()) {
      throw new ValidationException("ClOrdID (11) cannot be empty");
    }

    char side = order.getSide().getValue();
    if (side != Side.BUY && side != Side.SELL) {
      throw new ValidationException("Invalid value for Side (54)");
    }

    String symbol = order.getSymbol().getValue();
    boolean validSymbol = false;
    for (String validSym : VALID_SYMBOLS) {
      if (validSym.equals(symbol)) {
        validSymbol = true;
        break;
      }
    }
    if (!validSymbol) {
      throw new ValidationException("Invalid Symbol (55)");
    }

    if (order.getPrice().getValue() <= 0) {
      throw new ValidationException("Price (44) must be positive");
    }

    if (order.getOrderQty().getValue() <= 0) {
      throw new ValidationException("Quantity (32) must be positive");
    }
    logger.info("-------------- Validation completed --------------");
  }

  private void sendReject(SessionID sessionId, String reason) {
    try {
      Reject reject = new Reject();
      reject.set(new Text(reason));
      reject.set(new RefSeqNum(1));

      logger.info("Sending reject");
      logger.debug(String.valueOf(reject));

      Session.sendToTarget(reject, sessionId);
    } catch (SessionNotFound e) {
      logger.error("Session not found: {}", sessionId);
    }
  }

  private String generateExecID() {
    return String.valueOf(System.currentTimeMillis());
  }

  private static class ValidationException extends Exception {

    public ValidationException(String message) {
      super(message);
    }
  }
}
