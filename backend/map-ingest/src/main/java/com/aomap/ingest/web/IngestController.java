package com.aomap.ingest.web;

import com.aomap.common.api.ApiResult;
import com.aomap.ingest.model.JobView;
import com.aomap.ingest.model.LogView;
import com.aomap.ingest.service.IngestService;
import com.aomap.ingest.service.IngestService.RegionStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @GetMapping("/regions")
    public ApiResult<List<RegionStatus>> regions() {
        return ApiResult.ok(ingestService.regions());
    }

    @PostMapping("/jobs")
    public ApiResult<JobView> start(@RequestBody(required = false) StartRequest request) {
        String regionCode = request == null ? null : request.regionCode();
        return ApiResult.ok(ingestService.start(regionCode, "MANUAL"));
    }

    @GetMapping("/jobs")
    public ApiResult<List<JobView>> list(@RequestParam(defaultValue = "10") int limit) {
        return ApiResult.ok(ingestService.list(limit));
    }

    @GetMapping("/jobs/latest")
    public ApiResult<JobView> latest() {
        return ApiResult.ok(ingestService.latest());
    }

    @GetMapping("/jobs/{id}")
    public ApiResult<JobView> get(@PathVariable long id) {
        return ApiResult.ok(ingestService.get(id));
    }

    @GetMapping("/jobs/{id}/logs")
    public ApiResult<List<LogView>> logs(@PathVariable long id, @RequestParam(defaultValue = "0") long afterId) {
        return ApiResult.ok(ingestService.logs(id, afterId));
    }

    public record StartRequest(String regionCode) {
    }
}
