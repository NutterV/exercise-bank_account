package com.exercise.bankaccount.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AuditSubmissionListenerTest {
	@Test
	void shouldPrintFormattedSubmissionWhenTextMessageArrives() throws Exception {
		AuditSubmissionListener listener = new AuditSubmissionListener();
		MessageListener messageListener = onMessage(listener);
		TextMessage textMessage = textMessage(jsonSubmission());
		PrintStream originalOut = System.out;
		ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

		try {
			System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));

			messageListener.onMessage(textMessage);
		} finally {
			System.setOut(originalOut);
		}

		String printed = capturedOut.toString(StandardCharsets.UTF_8);
		assertTrue(printed.contains("\"submission\""));
		assertTrue(printed.contains("\"totalValueOfAllTransactions\" : 75000"));
		assertTrue(printed.contains("\"countOfTransactions\" : 18"));
	}

	@Test
	void shouldRejectUnsupportedMessages() throws Exception {
		AuditSubmissionListener listener = new AuditSubmissionListener();
		Method readSubmission = AuditSubmissionListener.class.getDeclaredMethod("readSubmission", Message.class);
		readSubmission.setAccessible(true);

		Exception exception = (Exception) org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
				() -> readSubmission.invoke(listener, message()));

		assertTrue(exception.getCause() instanceof JMSException);
		assertTrue(exception.getCause().getMessage().contains("Unsupported message type"));
	}

	@Test
	void shouldIgnoreUnsupportedMessagesInListenerCallback() throws Exception {
		AuditSubmissionListener listener = new AuditSubmissionListener();
		MessageListener messageListener = onMessage(listener);

		assertDoesNotThrow(() -> messageListener.onMessage(message()));
	}

	@Test
	void shouldReleaseAwaitShutdownWhenClosed() throws Exception {
		AuditSubmissionListener listener = new AuditSubmissionListener();
		CountDownLatch released = new CountDownLatch(1);
		Thread awaitingThread = new Thread(() -> {
			try {
				listener.awaitShutdown();
				released.countDown();
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(exception);
			}
		});

		awaitingThread.start();
		listener.close();

		assertTrue(released.await(5, TimeUnit.SECONDS));
		awaitingThread.join(TimeUnit.SECONDS.toMillis(5));
	}

	@Test
	void shouldCloseResourcesQuietly() throws Exception {
		AuditSubmissionListener listener = new AuditSubmissionListener();
		CloseTrackingAutoCloseable consumer = new CloseTrackingAutoCloseable();
		CloseTrackingAutoCloseable session = new CloseTrackingAutoCloseable();
		CloseTrackingAutoCloseable connection = new CloseTrackingAutoCloseable();

		setField(listener, "consumer", proxy(jakarta.jms.MessageConsumer.class, consumer));
		setField(listener, "session", proxy(jakarta.jms.Session.class, session));
		setField(listener, "connection", proxy(jakarta.jms.Connection.class, connection));

		listener.close();

		assertEquals(1, consumer.closeCount);
		assertEquals(1, session.closeCount);
		assertEquals(1, connection.closeCount);
	}

	private static String jsonSubmission() throws Exception {
		return """
				{
				  "batches": [
				    {
				      "totalValueOfAllTransactions": 75000,
				      "countOfTransactions": 18
				    }
				  ]
				}
				""";
	}

	private static MessageListener onMessage(AuditSubmissionListener listener) throws Exception {
		Method onMessage = AuditSubmissionListener.class.getDeclaredMethod("onMessage");
		onMessage.setAccessible(true);
		return (MessageListener) onMessage.invoke(listener);
	}

	private static TextMessage textMessage(String text) {
		return proxy(TextMessage.class, (proxy, method, args) -> {
			if (method.getName().equals("getText")) {
				return text;
			}
			return defaultValue(method.getReturnType());
		});
	}

	private static Message message() {
		return proxy(Message.class, (proxy, method, args) -> defaultValue(method.getReturnType()));
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> type, InvocationHandler handler) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Object defaultValue(Class<?> returnType) {
		if (returnType == void.class) {
			return null;
		}
		if (!returnType.isPrimitive()) {
			return null;
		}
		if (returnType == boolean.class) {
			return false;
		}
		if (returnType == byte.class) {
			return (byte) 0;
		}
		if (returnType == short.class) {
			return (short) 0;
		}
		if (returnType == int.class) {
			return 0;
		}
		if (returnType == long.class) {
			return 0L;
		}
		if (returnType == float.class) {
			return 0f;
		}
		if (returnType == double.class) {
			return 0d;
		}
		if (returnType == char.class) {
			return '\0';
		}
		throw new IllegalArgumentException("Unsupported primitive type: " + returnType);
	}

	private static final class CloseTrackingAutoCloseable implements InvocationHandler {
		private int closeCount;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("close")) {
				closeCount++;
			}
			return defaultValue(method.getReturnType());
		}
	}
}
