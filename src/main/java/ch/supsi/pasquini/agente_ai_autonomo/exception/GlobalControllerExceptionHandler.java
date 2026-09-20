package ch.supsi.pasquini.agente_ai_autonomo.exception;

import com.google.genai.errors.ClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.InvalidPathException;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidPathException.class)
    public ProblemDetail handleInvalidPath(InvalidPathException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid path format");
    }

    @ExceptionHandler(ClientException.class)
    public ProblemDetail handleClientException(ClientException ex) {
        String detail = "The provided API key is not valid. Make sure you copied it correctly.";
        String errorCode = "GEMINI_GENERIC_ERROR";

        String rawMessage = ex.getMessage();
        if (rawMessage != null) {
            if (rawMessage.contains("API_KEY_INVALID")) {
                detail = "The provided API key is not valid. Make sure you copied it correctly.";
                errorCode = "GEMINI_API_KEY_INVALID";
            } else if (rawMessage.contains("PERMISSION_DENIED")) {
                detail = "The API key does not have the required permissions for this request.";
                errorCode = "GEMINI_PERMISSION_DENIED";
            } else if (rawMessage.contains("RESOURCE_EXHAUSTED")) {
                detail = "You have exceeded the available quota for this API key.";
                errorCode = "GEMINI_QUOTA_EXCEEDED";
            }
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setProperty("errorCode", errorCode);
        return problem;
    }
}
