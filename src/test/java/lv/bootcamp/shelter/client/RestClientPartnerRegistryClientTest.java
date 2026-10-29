package lv.bootcamp.shelter.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RestClientPartnerRegistryClientTest {
    private MockWebServer server;
    private RestClientPartnerRegistryClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        client = new RestClientPartnerRegistryClient(RestClient.builder(), server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void lookup_shouldParseSuccessResponse() {
        server.enqueue(new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {"registered": true, "partnerShelterName": "The best partner"}
                                """));

        PartnerRegistryResult result = client.lookup("MC123");

        assertThat(result.registered()).isTrue();
        assertThat(result.partnerShelterName()).isEqualTo("The best partner");
    }

    @Test
    void lookup_shouldParseFailedResponse() {
        server.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                                {"message": "Service unavailable"}
                                """));

        assertThatThrownBy(() -> client.lookup("MC123"))
                .isInstanceOf(HttpServerErrorException.class)
                        .hasMessageContaining("503 Service Unavailable");
    }
}
