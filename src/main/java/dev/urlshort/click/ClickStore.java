package dev.urlshort.click;

import java.time.ZoneOffset;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** The {@code click} table (ADR-0013): one insert per recorded click. */
@Component
class ClickStore {

	private final JdbcClient jdbc;

	ClickStore(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	void insert(Click click) {
		jdbc.sql("INSERT INTO click (link_id, clicked_at, clicked_on, referrer, user_agent_class, client_hash)"
				+ " VALUES (:linkId, :clickedAt, :clickedOn, :referrer, :userAgentClass, :clientHash)")
				.param("linkId", click.linkId())
				.param("clickedAt", click.clickedAt().atOffset(ZoneOffset.UTC))
				.param("clickedOn", click.clickedOn())
				.param("referrer", click.referrer())
				.param("userAgentClass", click.userAgentClass())
				.param("clientHash", click.clientHash())
				.update();
	}
}
