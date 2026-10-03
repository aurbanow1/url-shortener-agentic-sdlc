/**
 * Click analytics: every redirect records a privacy-safe click without slowing or failing the
 * Visitor, and an Analyst reads a link's total clicks, clicks per day and top referrers (slice
 * 02-analytics, FR-7, FR-8, NFR-P1).
 *
 * <p>Belongs here: the {@code click} table, the reductions (referrer origin, user-agent class, salted
 * client hash), the bounded writer, the statistics endpoint, and the retention purge with its setting
 * (slice 02-click-retention, NFR-P2). Boundary: the only call into this
 * package from {@code link} is the redirect hook, and this package reads {@code link} only to turn a
 * code into its id; it never writes links or audit rows.
 */
@NullMarked
package dev.urlshort.click;

import org.jspecify.annotations.NullMarked;
