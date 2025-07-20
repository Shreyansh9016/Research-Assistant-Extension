package com.research.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.assistant.GeminiResponse;
import com.research.assistant.request.ResearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResearchService {
    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest request) {
//------> Build prompt
        String prompt = buildPrompt(request);
//------> Query the AI model API
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );
        System.out.println(prompt);
//------> Making request using web client
        String response = webClient.post()
                .uri(geminiApiUrl + "?key=" + geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
//------> Parse the response
//------> Return response
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String response) {
        try{
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if(geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()){
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
                if(firstCandidate.getContent() != null &&
                        firstCandidate.getContent().getParts() != null &&
                !firstCandidate.getContent().getParts().isEmpty()){
//------> with this entire operation , we are trying to get text from the output json format
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
            return "No Content Found in Response";
        }catch (Exception e){
            return "Error Parsing: " + e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();
        switch(request.getOperation()) {
            case "summarize":
                prompt.append("Provide clear and concise summary of the following text in a few bullet points:\n\n");
                break;
            case "suggest":
                prompt.append("Based on the following content : suggest related topics and further reading. Format the response with clear headings and bullet points:\n\n");
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + request.getOperation());

        }
        prompt.append(request.getContent());
        return prompt.toString();
    }
}
