package com.civicshield.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Global exception handling for server-rendered pages. JSON endpoints are handled by
 * {@link ApiExceptionHandler}, which has a higher precedence for {@code @RestController}s.
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return view("error/404", HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView conflict(IllegalStateException ex, HttpServletRequest req) {
        log.warn("Rejected action on {}: {}", req.getRequestURI(), ex.getMessage());
        return view("error/500", HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView any(Exception ex, HttpServletRequest req) {
        // Framework exceptions (404 for missing resources, 405, 400...) carry their own status
        if (ex instanceof ErrorResponse er) {
            HttpStatusCode code = er.getStatusCode();
            String viewName = code.value() == 404 ? "error/404" : code.value() == 403 ? "error/403" : "error/500";
            return view(viewName, code, null, req);
        }
        log.error("Unhandled error on {}", req.getRequestURI(), ex);
        return view("error/500", HttpStatus.INTERNAL_SERVER_ERROR, null, req);
    }

    private ModelAndView view(String name, HttpStatusCode status, String message, HttpServletRequest req) {
        ModelAndView mav = new ModelAndView(name);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("message", message);
        mav.addObject("path", req.getRequestURI());
        return mav;
    }
}
