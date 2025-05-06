package me.lojosho.leavingaugie.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.lojosho.leavingaugie.LeavingAugieApplication;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class GoogleMapsRequest {

    private final WebClient client;
    private final String originLocation;
    private final String destinationLocation;

    public GoogleMapsRequest(String originLocation, String destinationLocation ) {
        this.originLocation = originLocation;
        this.destinationLocation = destinationLocation;

        this.client = WebClient
                .builder()
                .baseUrl("https://maps.googleapis.com")
                .build();
    }

    public JsonNode fetchJson() throws IOException {
        String uri = "maps/api/distancematrix/json?origins=" + originLocation + "&destinations=" + destinationLocation + "&key=" + LeavingAugieApplication.getGoogleApiKey();

        Mono<String> response = client
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class);

        String jsonResponse = response.block(); // Blocking call to get the JSON string

        // Convert JSON String to Map
        return new ObjectMapper().readTree(jsonResponse);
    }
}
