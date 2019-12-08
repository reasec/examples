package com.reasec.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.reasec.certificatepinning.CertificatePinningWebClient;
import com.reasec.certificatepinning.model.CertificatePinningSpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ReasecClientTest {

    WireMockServer wireMockServer;
    //WireMockServer wireMockServerSecure;

    @BeforeEach
    public void setup () {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        /*
        wireMockServerSecure = new WireMockRule(wireMockConfig()
                .httpsPort(8443)
                .keystorePath("classpath:certs/certificate.p12")
                .keystorePassword("password")
                .keystoreType("PKCS12"));
        wireMockServerSecure.start();
        */
    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop();
        //wireMockServerSecure.stop();
    }

    private void loadStubs() {

        wireMockServer.stubFor(get(urlEqualTo("/status"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("status.json")));

        //TODO Review configuration for a secure configuration for Wiremock
        //http://wiremock.org/docs/https/
        wireMockServer.stubFor(get(urlEqualTo("/status2"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("status.json")));
    }

    @Test
    public void given_WebClient_when_callNonSecureEndpoint_then_expectedResultsTest() {

        loadStubs();

        Mono<Response> response = WebClient.create()
                .get()
                .uri("http://localhost:8090/status")
                .retrieve()
                .bodyToMono(Response.class);

        StepVerifier
                .create(response)
                .expectNext(new Response(true))
                .expectComplete()
                .verify();
    }

    @Test
    public void given_ReasecClient_when_callSecureEndpoint_then_expectedResultsTest() {

        loadStubs();

        final int port = 8090;
        final String publicKeySha = "B2:6A:A4:80:BC:C8:57:42:16:3B:57:3D:D4:25:C6:88:B7:4D:D4:9F:4A:4E:EE:5E:DF:D4:34:D0:33:98:3A:7F";

        //GIVEN
        final CertificatePinningSpec spec = CertificatePinningSpec.Builder()
                .sha(publicKeySha)
                .build();
        final WebClient webClient = CertificatePinningWebClient.builder(spec)
                .baseUrl("https://localhost:" + port + "/status2")
                .build();
        //WHEN
        StepVerifier.create(webClient.get().exchange())
                //THEN
                .assertNext(clientResponse -> assertThat(clientResponse.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();
    }
}
