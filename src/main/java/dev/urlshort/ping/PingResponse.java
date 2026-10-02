package dev.urlshort.ping;

/** The ping body: exactly {@code status} and {@code time}, the latter an ISO-8601 UTC instant. */
public record PingResponse(String status, String time) {
}
