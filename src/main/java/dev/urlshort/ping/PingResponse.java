package dev.urlshort.ping;

/**
 * The ping body: exactly {@code status} and {@code time}, the latter an ISO-8601 UTC instant.
 *
 * @param status always {@code ok} when the service answers
 * @param time the server's current instant, ISO-8601 UTC with the {@code Z} designator
 */
public record PingResponse(String status, String time) {
}
