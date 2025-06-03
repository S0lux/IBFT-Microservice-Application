import http, { get } from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const paymentSuccessRate = new Rate("payment_success_rate");
const paymentDuration = new Trend("payment_duration");

export const options = {
  stages: [
    { duration: "10s", target: 100 },
    { duration: "30s", target: 1000 },
    { duration: "30s", target: 5000 },
    { duration: "1m", target: 5500 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<2000"],
    http_req_failed: ["rate<0.05"],
    payment_success_rate: ["rate>0.90"],
    payment_duration: ["p(95)<1500"],
  },
};

const senderAccounts = [
  "SENDER_ACCOUNT_1",
  "SENDER_ACCOUNT_2",
  "SENDER_ACCOUNT_3",
  "SENDER_ACCOUNT_4",
  "SENDER_ACCOUNT_5",
  "SENDER_ACCOUNT_6",
  "SENDER_ACCOUNT_7",
  "SENDER_ACCOUNT_8",
  "SENDER_ACCOUNT_9",
  "SENDER_ACCOUNT_10",
];

const recipientAccounts = [
  "RECIPIENT_ACCOUNT_1",
  "RECIPIENT_ACCOUNT_2",
  "RECIPIENT_ACCOUNT_3",
  "RECIPIENT_ACCOUNT_4",
  "RECIPIENT_ACCOUNT_5",
  "RECIPIENT_ACCOUNT_6",
  "RECIPIENT_ACCOUNT_7",
  "RECIPIENT_ACCOUNT_8",
  "RECIPIENT_ACCOUNT_9",
  "RECIPIENT_ACCOUNT_10",
];

function getRandomElement(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function createPaymentRequest() {
  return {
    senderAccountNumber: getRandomElement(senderAccounts),
    recipientAccountNumber: getRandomElement(recipientAccounts),
    description: "Stress test payment",
    amount: 1.0,
  };
}

export default function () {
  const baseUrl = "http://localhost:8083";
  const endpoint = `${baseUrl}/v1/payments`;

  const paymentData = createPaymentRequest();

  // Request headers
  const headers = {
    "Content-Type": "application/json",
    "x-user-id": "ad56129d-7e8e-46f3-b9aa-ec43ede7c567",
    "x-user-status": "ACTIVE",
  };

  // Make the payment request
  const startTime = Date.now();
  const response = http.post(endpoint, JSON.stringify(paymentData), {
    headers,
  });
  const duration = Date.now() - startTime;

  // Record custom metrics
  paymentDuration.add(duration);

  // Additional checks based on response status
  if (response.status === 200 || response.status === 201) {
    // Success case checks
    check(response, {
      "successful payment response structure": (r) => {
        try {
          const body = JSON.parse(r.body);
          return (
            body.hasOwnProperty("id") || body.hasOwnProperty("transactionId")
          );
        } catch (e) {
          return false;
        }
      },
    });
    paymentSuccessRate.add(true);
  } else if (response.status === 400) {
    // Validation error checks
    check(response, {
      "validation error has message": (r) => {
        try {
          const body = JSON.parse(r.body);
          return (
            body.hasOwnProperty("message") || body.hasOwnProperty("errors")
          );
        } catch (e) {
          return false;
        }
      },
    });
    paymentSuccessRate.add(false);
  } else if (response.status >= 500) {
    // Server error
    console.error(`Server error: ${response.status} - ${response.body}`);
    paymentSuccessRate.add(false);
  } else {
    // Other errors
    console.warn(`Unexpected status: ${response.status} - ${response.body}`);
    paymentSuccessRate.add(false);
  }

  if (Math.random() < 0.05) {
    // 5% sampling
    console.log(`Payment request: ${JSON.stringify(paymentData)}`);
    console.log(`Response status: ${response.status}, Duration: ${duration}ms`);
  }

  sleep(Math.random() * 2 + 0.5);
}

export function setup() {
  console.log("Starting payment API load test...");
}

export function teardown(data) {
  console.log("Payment API load test completed.");
}
