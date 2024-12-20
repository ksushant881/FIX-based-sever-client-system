package com.example.intradetsassignment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

public class FIXClient implements Application {

  private static final Logger logger = LoggerFactory.getLogger(FIXClient.class);
  private static final String CLIENT_PROPERTIES_FILENAME = "client-config.properties";

  public static void main(String[] args) {
    try {
      SessionSettings settings = new SessionSettings(CLIENT_PROPERTIES_FILENAME);
      Application application = new FIXClient();
      MessageStoreFactory storeFactory = new FileStoreFactory(settings);
      LogFactory logFactory = new FileLogFactory(settings);
      MessageFactory messageFactory = new DefaultMessageFactory();

      SocketInitiator initiator = new SocketInitiator(application, storeFactory, settings, logFactory, messageFactory);
      initiator.start();
      logger.info("FIX Client started.");
      Thread.sleep(5000);

      SessionID sessionId = initiator.getSessions().getFirst();
      sendNewOrderSingle(sessionId);
    } catch (InterruptedException | ConfigError e) {
      logger.error(e.getMessage());
    }
  }

  private static void sendNewOrderSingle(SessionID sessionId) {
    try {
      NewOrderSingle validOrder = new NewOrderSingle(
          new ClOrdID("12345"),
          new Side(Side.BUY),
          new TransactTime(),
          new OrdType(OrdType.LIMIT)
      );
      validOrder.set(new Symbol("AAPL"));
      validOrder.set(new Price(150.25));
      validOrder.set(new OrderQty(100));

      NewOrderSingle invalidOrder = new NewOrderSingle(
          new ClOrdID("12345"),
          new Side(Side.SELL_SHORT),
          new TransactTime(),
          new OrdType(OrdType.LIMIT)
      );
      invalidOrder.set(new Symbol("AAPL"));
      invalidOrder.set(new Price(150.25));
      invalidOrder.set(new OrderQty(100));

      Session.sendToTarget(validOrder, sessionId);
      logger.info("Valid client message sent!");
      logger.debug(String.valueOf(validOrder));

      Thread.sleep(5000);

      Session.sendToTarget(invalidOrder, sessionId);
      logger.info("Invalid client message sent!");
      logger.debug(String.valueOf(invalidOrder));
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  @Override
  public void onCreate(SessionID sessionId) {
    logger.info("Session created: {}", sessionId);
  }

  @Override
  public void onLogon(SessionID sessionId) {
    logger.info("Logged on: {}", sessionId);
  }

  @Override
  public void onLogout(SessionID sessionId) {
    logger.info("Logged out: {}", sessionId);
  }

  @Override
  public void toAdmin(Message message, SessionID sessionId) {
  }

  @Override
  public void fromAdmin(Message message, SessionID sessionId) {
    logger.info("Server response: {}", message);
  }

  @Override
  public void toApp(Message message, SessionID sessionId) {
  }

  @Override
  public void fromApp(Message message, SessionID sessionId) {
    logger.info("Server response: {}", message);
  }
}
