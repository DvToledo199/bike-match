package com.bikematch.api;

import com.bikematch.auth.AccountAlreadyExistsException;
import com.bikematch.auth.InvalidCredentialsException;
import com.bikematch.bike.BikeAnalysisLockedException;
import com.bikematch.bike.BikeNotFoundException;
import com.bikematch.bike.InvalidBikePhotoException;
import com.bikematch.media.ImageStorageException;
import com.bikematch.user.CurrentUserNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Send valid JSON");
    }

    /** Bad input the annotations cannot catch. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /** A @Valid check failed. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ProblemDetail handleAccountAlreadyExists(
            AccountAlreadyExistsException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(CurrentUserNotFoundException.class)
    public ProblemDetail handleCurrentUserNotFound(
            CurrentUserNotFoundException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(BikeNotFoundException.class)
    public ProblemDetail handleBikeNotFound(BikeNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BikeAnalysisLockedException.class)
    public ProblemDetail handleBikeAnalysisLocked(BikeAnalysisLockedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidBikePhotoException.class)
    public ProblemDetail handleInvalidBikePhoto(InvalidBikePhotoException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleOversizedPhoto() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Photo must not exceed 10 MB");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingRequestPart(MissingServletRequestPartException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Missing request part: " + exception.getRequestPartName());
    }

    @ExceptionHandler(ImageStorageException.class)
    public ProblemDetail handleImageStorage(ImageStorageException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }
}
