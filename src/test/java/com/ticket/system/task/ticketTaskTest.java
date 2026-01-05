package com.ticket.system.task;

import org.junit.jupiter.api.Test;   // ✅ JUnit5
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TicketTaskTest {

    @Autowired
    private TicketTask ticketTask;

    @Test
    void test() {
        ticketTask.addTicket(); // ✅ 正常
    }
}
