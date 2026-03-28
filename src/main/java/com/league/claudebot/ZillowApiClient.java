package com.league.claudebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ZillowApiClient {

    private static final String API_URL = "https://api.hasdata.com/scrape/zillow/listing";
    private static final String API_KEY = "ca3d63ca-c8c3-4ced-8054-571117184bda";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ZillowApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public SearchResult search(String keyword, String type) throws IOException, InterruptedException {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = API_URL + "?keyword=" + encodedKeyword + "&type=" + type;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-api-key", API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status " + response.statusCode() + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    private SearchResult parseResponse(String json) throws IOException {
        JsonNode root = mapper.readTree(json);

        int totalResults = 0;
        JsonNode searchInfo = root.path("searchInformation");
        if (!searchInfo.isMissingNode()) {
            totalResults = searchInfo.path("totalResults").asInt(0);
        }

        List<Property> properties = new ArrayList<>();
        JsonNode propertiesNode = root.path("properties");
        if (propertiesNode.isArray()) {
            for (JsonNode node : propertiesNode) {
                Property p = parseProperty(node);
                if (p != null) {
                    properties.add(p);
                }
            }
        }

        return new SearchResult(totalResults, properties);
    }

    private Property parseProperty(JsonNode node) {
        try {
            String id = node.path("id").asText("");
            String url = node.path("url").asText("");
            String homeType = node.path("homeType").asText("UNKNOWN");
            String status = node.path("status").asText("");
            double price = node.path("price").asDouble(0);
            int beds = node.path("beds").asInt(0);
            double baths = node.path("baths").asDouble(0);
            double area = node.path("area").asDouble(0);
            int daysOnZillow = node.path("daysOnZillow").asInt(0);

            JsonNode address = node.path("address");
            String street = address.path("street").asText("");
            String city = address.path("city").asText("");
            String state = address.path("state").asText("");
            String zipcode = address.path("zipcode").asText("");

            return new Property(id, url, homeType, status, price, beds, baths,
                    area, daysOnZillow, street, city, state, zipcode);
        } catch (Exception e) {
            return null;
        }
    }

    public static class SearchResult {
        private final int totalResults;
        private final List<Property> properties;

        public SearchResult(int totalResults, List<Property> properties) {
            this.totalResults = totalResults;
            this.properties = properties;
        }

        public int getTotalResults() { return totalResults; }
        public List<Property> getProperties() { return properties; }
    }
}
