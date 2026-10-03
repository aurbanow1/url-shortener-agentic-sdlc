package dev.urlshort.link;

/**
 * The state of a link as written to an audit row's before and after columns (A-18).
 *
 * @param url the target
 * @param state {@code active} or {@code retired}
 */
record LinkSnapshot(String url, String state) {
}
