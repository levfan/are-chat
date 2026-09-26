package com.smart.chat.tools;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Component
@Endpoint(id = "h2dump")
public class H2DumpEndpoint {

    @ReadOperation
    public String dump() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:arechat", "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("SCRIPT TO '/tmp/arechat_dump.sql'");
            return "OK - dumped";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}