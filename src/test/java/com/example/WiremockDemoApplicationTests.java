package com.example;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootVersion;

class WiremockDemoApplicationTests {

  private static final Logger log = LoggerFactory.getLogger("WiremockDemoApplicationTests");
  private static final WireMockServer wireMockServer = new WireMockServer(8089, 8090);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  static void beforeAll() {
    wireMockServer.start();
  }

  @AfterAll
  static void afterAll() {
    if (wireMockServer.isRunning()) {
      wireMockServer.shutdownServer();
    }
  }

  @BeforeEach
  void setUp() {
    WireMock.configureFor("localhost", 8089);
    wireMockServer.addMockServiceRequestListener(this::requestReceived);
  }

  private void requestReceived(Request req, Response res) {
    log.info("Request received: {}", req.getClientIp());
    log.info("Response received: {}", res.getBodyAsString());
  }

  @Test
  void testProducts() throws Exception {
    var json = objectMapper.readTree(new File("src/test/resources/stub/products.json"));

    stubFor(
        WireMock.get("/products")
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withResponseBody(Body.fromJsonBytes(
                        Files.readAllBytes(Path.of("src/test/resources/stub/products.json"))))
                    .withStatus(200)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI("http://localhost:8089/products"))
        .GET()
        .build();
    HttpResponse<String> response = HttpClient.newHttpClient()
        .send(request, BodyHandlers.ofString());
    var responseJson = objectMapper.readTree(response.body());
    assertEquals(json, responseJson);
  }

  @Test
  void contextLoads() {
    assertEquals("2.7.18", SpringBootVersion.getVersion());
  }

}
