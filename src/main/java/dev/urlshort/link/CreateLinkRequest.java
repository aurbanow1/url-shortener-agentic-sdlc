package dev.urlshort.link;

import org.jspecify.annotations.Nullable;

/**
 * The create body. Unknown members are ignored (A-14); the rules are applied by
 * {@link LinkValidation}, not by annotations, because the SPEC reports one token in a fixed order.
 *
 * @param url the target to shorten; {@code null} when absent
 */
record CreateLinkRequest(@Nullable String url) {
}
