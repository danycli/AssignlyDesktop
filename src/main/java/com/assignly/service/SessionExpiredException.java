package com.assignly.service;

import java.io.IOException;

/**
 * Thrown when the portal returns the Login page HTML instead of the expected data,
 * indicating that the user's session cookie has expired.
 */
public class SessionExpiredException extends IOException {
    public SessionExpiredException(String message) {
        super(message);
    }
}
