/**
 * Short links: a Creator turns an http(s) URL into a short code and can read and retire it, and a
 * Visitor who opens the code is redirected ({@code 302}) or told it is gone ({@code 410}). Slice
 * 01-create-redirect, FR-1 to FR-6 and FR-9.
 *
 * <p>Belongs here: the {@code link} table and its repository, code generation, target validation,
 * the idempotent create, the management API under {@code /api/links} and the redirect route
 * {@code /{code}}. Does not belong here: error rendering and request plumbing ({@code web}), the
 * audit table ({@code audit}), click recording (slice 02-analytics adds one call in
 * {@code RedirectController}).
 */
@NullMarked
package dev.urlshort.link;

import org.jspecify.annotations.NullMarked;
