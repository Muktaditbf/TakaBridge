package com.takabridge.service;

/**
 * Thrown when what the visitor typed cannot be converted.
 *
 * The message it carries is always written for a human being, so a servlet
 * can put it straight on the page without exposing anything technical.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
