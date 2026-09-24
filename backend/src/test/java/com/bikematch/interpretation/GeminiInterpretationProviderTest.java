package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** Gemini itself is replaced by a stub model: these tests never leave the machine. */
class GeminiInterpretationProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InterpretationContext context = new InterpretationContext(
            1, 1, "monopivot-reference-v2", "kinematics-rules-1", "en",
            new InterpretationContext.DataQuality(true, null),
            new InterpretationContext.Capabilities(true, true, true, true),
            new InterpretationContext.Conditions("ENDURO", 30.0, 32, 52),
            new InterpretationContext.LeverageShape(
                    "MEDIUM", "PROGRESSIVE", "PROGRESSIVE", "LINEAR", 2.9, 2.8, 2.7, 2.5, 2.35),
            new InterpretationContext.Readings(
                    "MEDIUM", "FIRM", "SQUATS_UNDER_BRAKING", "MEDIUM", "TYPICAL", "NOT_WORTH_MENTIONING"),
            List.of(
                    new InterpretationContext.Evidence("usefulProgressionPercent", 18, "%"),
                    new InterpretationContext.Evidence("maxRearwardMm", 12, "mm")),
            List.of("reference"), List.of("leverage"), List.of("pressure"));

    @Test
    void validAnswerBecomesAnAiInterpretationWithTheCitedEvidence() {
        var provider = providerAnswering("""
                {"summary":" A progressive bike. ","evidenceKeys":["usefulProgressionPercent","maxRearwardMm"]}
                """);

        Interpretation interpretation = provider.generate(context);

        assertThat(interpretation.summary()).isEqualTo("A progressive bike.");
        assertThat(interpretation.source()).isEqualTo(Interpretation.Source.AI);
        assertThat(interpretation.providerVersion()).isEqualTo("gemini-test-model");
        assertThat(interpretation.evidence()).isEqualTo(context.evidence());
    }

    /** The prompt has JSON braces in it: they must reach Gemini as written, not as a template. */
    @Test
    void sendsTheInstructionsAsSystemMessageAndTheContextAsUserMessage() throws Exception {
        var model = new StubChatModel(
                "{\"summary\":\"Text.\",\"evidenceKeys\":[\"usefulProgressionPercent\",\"maxRearwardMm\"]}");
        providerUsing(model).generate(context);

        var messages = model.lastPrompt.getInstructions();
        assertThat(messages).extracting(message -> message.getMessageType())
                .containsExactly(MessageType.SYSTEM, MessageType.USER);
        assertThat(messages.get(0).getText())
                .contains("{\"summary\":\"...\",\"evidenceKeys\":[\"...\"]}");
        assertThat(messages.get(1).getText()).isEqualTo(objectMapper.writeValueAsString(context));
    }

    @Test
    void evidenceKeyThatWasNotSentIsRejected() {
        var provider = providerAnswering(
                "{\"summary\":\"Text.\",\"evidenceKeys\":[\"usefulProgressionPercent\",\"invented\"]}");

        assertThatThrownBy(() -> provider.generate(context))
                .isInstanceOf(InterpretationProviderException.class)
                .hasMessageContaining("unknown evidence key");
    }

    @Test
    void markupInTheSummaryIsRejected() {
        var provider = providerAnswering(
                "{\"summary\":\"<b>Text</b>\",\"evidenceKeys\":[\"usefulProgressionPercent\",\"maxRearwardMm\"]}");

        assertThatThrownBy(() -> provider.generate(context))
                .isInstanceOf(InterpretationProviderException.class)
                .hasMessageContaining("unsafe explanation shape");
    }

    @Test
    void repeatedEvidenceKeyDoesNotCountAsTwo() {
        var provider = providerAnswering(
                "{\"summary\":\"Text.\",\"evidenceKeys\":[\"maxRearwardMm\",\"maxRearwardMm\"]}");

        assertThatThrownBy(() -> provider.generate(context))
                .isInstanceOf(InterpretationProviderException.class)
                .hasMessageContaining("duplicate evidence keys");
    }

    @Test
    void answerThatIsNotJsonIsRejected() {
        var provider = providerAnswering("Here is your explanation: the bike is progressive.");

        assertThatThrownBy(() -> provider.generate(context))
                .isInstanceOf(InterpretationProviderException.class);
    }

    /** InterpretationService only falls back to the rules text for this exception type. */
    @Test
    void failedCallIsReportedAsAProviderFailure() {
        ChatModel failing = prompt -> {
            throw new IllegalStateException("503 Service Unavailable");
        };

        assertThatThrownBy(() -> providerUsing(failing).generate(context))
                .isInstanceOf(InterpretationProviderException.class)
                .hasRootCauseMessage("503 Service Unavailable");
    }

    @Test
    void missingModelStopsTheApplicationAtStartup() {
        assertThatThrownBy(() -> new GeminiInterpretationProvider(
                ChatClient.builder(new StubChatModel("{}")), objectMapper, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_MODEL");
    }

    private GeminiInterpretationProvider providerAnswering(String answer) {
        return providerUsing(new StubChatModel(answer));
    }

    private GeminiInterpretationProvider providerUsing(ChatModel model) {
        return new GeminiInterpretationProvider(ChatClient.builder(model), objectMapper, "test-model");
    }

    private static final class StubChatModel implements ChatModel {

        private final String answer;
        private Prompt lastPrompt;

        private StubChatModel(String answer) {
            this.answer = answer;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            lastPrompt = prompt;
            return new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        }
    }
}
