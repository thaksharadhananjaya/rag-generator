package com.rag.raggeneratorapplication.rag.service;

import com.rag.raggeneratorapplication.config.props.RetrievalProperties;
import com.rag.raggeneratorapplication.port.llm.LlmMessage;
import com.rag.raggeneratorapplication.port.llm.LlmRequest;
import com.rag.raggeneratorapplication.port.vector.VectorMatch;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

/**
 * Builds a grounded chat prompt from a question and the retrieved chunks.
 * Concrete by design. Retrieved context is capped at
 */
@Service
@RequiredArgsConstructor
public class PromptService {

    private static final String SYSTEM_PROMPT = """
            You are a precise assistant. Answer the question using ONLY the provided context.
            If the context does not contain the answer, say you don't know.
            Cite the context using its bracket number, for example [1].
            """;

    private final RetrievalProperties properties;

    public LlmRequest buildPrompt(
            String question,
            List<VectorMatch> matches) {

        String context = matches.stream()
                .map(VectorMatch::text)
                .limit(properties.topK())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        PromptTemplate promptTemplate = new PromptTemplate(SYSTEM_PROMPT);

        Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", question
        ));

        List<LlmMessage> messages = prompt.getInstructions()
                .stream()
                .map(this::toLlmMessage)
                .toList();

        return LlmRequest.of(
                messages
        );
    }
    private LlmMessage toLlmMessage(Message message) {
        MessageType type = message.getMessageType();

        return switch (type) {
            case SYSTEM -> LlmMessage.system(message.getText());
            case USER -> LlmMessage.user(message.getText());
            default -> throw new IllegalArgumentException(
                    "Unsupported message type: " + type
            );
        };
    }
}
