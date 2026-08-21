package com.neu.riketiku.portal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/portal-stats")
public class PortalStatsController {
    private final PortalStatsService service;

    public PortalStatsController(PortalStatsService service) {
        this.service = service;
    }

    @GetMapping
    public PortalStats current() {
        return service.current();
    }
}
