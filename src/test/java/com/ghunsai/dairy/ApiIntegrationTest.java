package com.ghunsai.dairy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test against a REAL PostgreSQL (Flyway migration + Hibernate validate + security rules).
 * Runs only when TEST_DB_URL is set, and needs an EMPTY database (use a throwaway one):
 *
 *   TEST_DB_URL=jdbc:postgresql://localhost:5432/dairy_test TEST_DB_USER=postgres TEST_DB_PASSWORD=postgres \
 *   ./mvnw test
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=${TEST_DB_URL}",
        "spring.datasource.username=${TEST_DB_USER:postgres}",
        "spring.datasource.password=${TEST_DB_PASSWORD:postgres}",
        "app.jwt.secret=integration-test-secret-0123456789abcdef",
        "app.bootstrap.owner.username=boss",
        "app.bootstrap.owner.password=Boss@12345"
})
class ApiIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private record Resp(int status, String body) {
    }

    private Resp call(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.method(method, json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(response.statusCode(), response.body());
    }

    private static String find(String body, String regex) {
        Matcher m = Pattern.compile(regex).matcher(body);
        assertTrue(m.find(), "Pattern " + regex + " not found in: " + body);
        return m.group(1);
    }

    private String login(String username, String password) throws Exception {
        Resp r = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertEquals(200, r.status(), r.body());
        return find(r.body(), "\"accessToken\":\"([^\"]+)\"");
    }

    @Test
    void ownerAndWorkerFlow() throws Exception {
        // --- authentication ---
        assertEquals(401, call("GET", "/api/products", null, null).status());
        assertEquals(401, call("POST", "/api/auth/login", null,
                "{\"username\":\"boss\",\"password\":\"wrong-password\"}").status());
        assertEquals(200, call("GET", "/actuator/health", null, null).status());

        String owner = login("boss", "Boss@12345");

        // --- owner creates a worker ---
        Resp created = call("POST", "/api/users", owner,
                "{\"username\":\"ramu\",\"fullName\":\"Ramu\",\"password\":\"Worker@123\",\"role\":\"WORKER\"}");
        assertEquals(201, created.status(), created.body());
        String workerId = find(created.body(), "\"id\":(\\d+)");
        String worker = login("ramu", "Worker@123");

        // --- worker is blocked from every owner-only area ---
        assertEquals(403, call("GET", "/api/users", worker, null).status());
        assertEquals(403, call("GET", "/api/dashboard/daily", worker, null).status());
        assertEquals(403, call("GET", "/api/sales", worker, null).status());
        assertEquals(403, call("GET", "/api/sales/reports/daily", worker, null).status());
        assertEquals(403, call("GET", "/api/stock/movements", worker, null).status());
        assertEquals(403, call("GET", "/api/customers/balances", worker, null).status());
        assertEquals(403, call("POST", "/api/stock/purchase", worker,
                "{\"productId\":1,\"quantity\":5}").status());
        assertEquals(403, call("POST", "/api/products", worker,
                "{\"name\":\"X\",\"category\":\"Y\",\"unit\":\"kg\",\"sellingPrice\":10,\"lowStockThreshold\":1}").status());

        // --- owner sets up product + stock ---
        Resp product = call("POST", "/api/products", owner,
                "{\"name\":\"Milk\",\"category\":\"Dairy\",\"unit\":\"litre\",\"sellingPrice\":50.00,\"lowStockThreshold\":2}");
        assertEquals(201, product.status(), product.body());
        String productId = find(product.body(), "\"id\":(\\d+)");
        assertEquals(201, call("POST", "/api/stock/purchase", owner,
                "{\"productId\":" + productId + ",\"quantity\":10}").status());

        // --- worker can use the billing counter ---
        Resp products = call("GET", "/api/products", worker, null);
        assertEquals(200, products.status());
        assertTrue(products.body().contains("Milk"));

        String bill = "{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}],"
                + "\"cashAmount\":100.00,\"upiAmount\":0,\"udhaarAmount\":0,\"clientRequestId\":\"bill-001\"}";
        Resp sale = call("POST", "/api/sales", worker, bill);
        assertEquals(201, sale.status(), sale.body());
        assertTrue(sale.body().contains("\"totalAmount\":100"), sale.body());
        String saleId = find(sale.body(), "\"id\":(\\d+)");

        // retry with the same clientRequestId must not create a second bill
        Resp retry = call("POST", "/api/sales", worker, bill);
        assertEquals(201, retry.status(), retry.body());
        assertEquals(saleId, find(retry.body(), "\"id\":(\\d+)"));

        Resp stock = call("GET", "/api/stock/" + productId, worker, null);
        assertEquals(200, stock.status());
        assertTrue(stock.body().contains("\"availableStock\":8"), stock.body()); // 10 - 2, not 10 - 4

        // overselling is refused
        Resp oversell = call("POST", "/api/sales", worker,
                "{\"items\":[{\"productId\":" + productId + ",\"quantity\":100}],"
                        + "\"cashAmount\":5000,\"upiAmount\":0,\"udhaarAmount\":0}");
        assertEquals(400, oversell.status(), oversell.body());

        // worker sees own bill + own daily totals, owner sees shop-wide reports
        assertEquals(200, call("GET", "/api/sales/" + saleId, worker, null).status());
        assertEquals(200, call("GET", "/api/sales/reports/my-daily", worker, null).status());
        Resp report = call("GET", "/api/sales/reports/daily", owner, null);
        assertEquals(200, report.status());
        assertTrue(report.body().contains("\"totalBills\":1"), report.body());
        assertEquals(200, call("GET", "/api/dashboard/daily", owner, null).status());

        // --- owner disables the worker: his token stops working immediately ---
        assertEquals(200, call("PUT", "/api/users/" + workerId + "/active", owner, "{\"active\":false}").status());
        assertEquals(401, call("GET", "/api/products", worker, null).status());
    }
}
