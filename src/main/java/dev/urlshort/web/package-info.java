/**
 * Cross-cutting HTTP concerns that every endpoint shares, so that every response an Operator or
 * client sees has the same shape: the server-issued request id and its log event, the request body
 * cap, RFC 9457 problem details for every error (built from server-owned values only), and the
 * OpenAPI document's fixed metadata.
 *
 * <p>Belongs here: filters, the one error advice, problem factories, API-document configuration.
 * Does not belong here: feature rules or persistence; features throw the problems built by
 * {@link dev.urlshort.web.Problems} and never render errors themselves.
 */
@NullMarked
package dev.urlshort.web;

import org.jspecify.annotations.NullMarked;
