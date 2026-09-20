package ch.supsi.pasquini.agente_ai_autonomo.ai.gemini;

import com.google.genai.types.FunctionCall;
import com.google.genai.types.Part;

import java.util.List;
import java.util.Queue;

public record FunctionPending(List<Part> collectedPart, FunctionCall functionCall, Queue<FunctionCall> remainingCalls) {

}
