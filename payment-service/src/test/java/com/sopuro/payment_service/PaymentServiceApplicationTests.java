package com.sopuro.payment_service;

import com.sopuro.payment_service.dtos.PaymentRequestDTO;
import com.sopuro.payment_service.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@Slf4j
class PaymentServiceApplicationTests {

	@Autowired
	private PaymentService paymentService;

	@Test
	void contextLoads() {
	}

	@Test
	void loadTestFunction() throws Exception {
		int numberOfThreads = 4;
		int requestsPerThread = 5000;
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

		long startTime = System.currentTimeMillis();
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);
		List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

		UUID requestId = UUID.fromString("ad56129d-7e8e-46f3-b9aa-ec43ede7c567");
		PaymentRequestDTO request = PaymentRequestDTO.builder()
				.senderAccountNumber("2LYRK4OL9Z25XW5R")
				.recipientAccountNumber("X7L3EAYH6BI6GUD0")
				.description("Test payment")
				.amount(BigDecimal.ONE)
				.build();

		for (int i = 0; i < numberOfThreads; i++) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				for (int j = 0; j < requestsPerThread; j++) {
					long requestStart = System.nanoTime();
					try {
						paymentService.makePayment(requestId, request);
						successCount.incrementAndGet();
					} catch (Exception e) {
						errorCount.incrementAndGet();
					}
					long requestEnd = System.nanoTime();
					responseTimes.add((requestEnd - requestStart) / 1_000_000);
				}
			}, executor);
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
		long endTime = System.currentTimeMillis();

		// Calculate metrics
		double totalTimeSeconds = (endTime - startTime) / 1000.0;
		int totalRequests = numberOfThreads * requestsPerThread;
		double throughput = totalRequests / totalTimeSeconds;

		responseTimes.sort(Long::compareTo);
		long p50 = responseTimes.get(responseTimes.size() / 2);
		long p95 = responseTimes.get((int) (responseTimes.size() * 0.95));
		long p99 = responseTimes.get((int) (responseTimes.size() * 0.99));

		log.info("=== LOAD TEST RESULTS ===");
		log.info("Total requests: {}", totalRequests);
		log.info("Successful: {}", successCount.get());
		log.info("Errors: {}", errorCount.get());
		log.info("Duration: {}", totalTimeSeconds + "s");
		log.info("Throughput: {}", String.format("%.2f", throughput) + " req/s");
        log.info("Response times (ms) - P50: {}, P95: {}, P99: {}", p50, p95, p99);

		executor.shutdown();
	}
}
