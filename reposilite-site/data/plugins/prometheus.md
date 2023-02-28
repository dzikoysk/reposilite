---
id: prometheus
title: Prometheus
description: Expose Reposilite metrics to Prometheus
official: true
repository: dzikoysk/reposilite
authors: [ 'dzikoysk' ]
maven: 'maven.reposilite.com'
groupId: 'com.reposilite.plugin'
artifactId: 'prometheus-plugin'
---

This plugin mounts [Prometheus](https://prometheus.io/) metrics under `/metrics` route to provide monitoring capabilities for Reposilite instance.

Prometheus endpoint is available only for authenticated connections,
so before you can use it, 
you need to configure plugin and your Prometheus instance.

Currently, this plugin can be configured via system properties or environment variables:

| Property | Environment Variable | Description |
| -------- | -------------------- | ------- |
| `reposilite.prometheus.path` | `REPOSILITE_PROMETHEUS_PATH` | Prometheus endpoint path |
| `reposilite.prometheus.user` | `REPOSILITE_PROMETHEUS_USER` | Prometheus user |
| `reposilite.prometheus.password` | `REPOSILITE_PROMETHEUS_PASSWORD` | Prometheus password |

So the minimal working example would be:

```bash
$ java -jar reposilite.jar \
    -Dreposilite.prometheus.user=user \
    -Dreposilite.prometheus.password=password
```

Example `prometheus.yml` configuration with Reposilite metrics endpoint:

```yaml
scrape_configs:
  - job_name: 'reposilite'
    scrape_interval: 5s
    metrics_path: "/metrics"
    static_configs:
      - targets: [ "localhost:8080" ]
        labels:
          group: 'reposilite'
    basic_auth:
      username: 'user'
      password: 'password'
```

## Metrics

Currently, the plugin exposes mostly Jetty related metrics, but it will be extended in the future. 

```yaml
# HELP jetty_requests_total Number of requests
# TYPE jetty_requests_total counter
jetty_requests_total 21.0
# HELP jetty_requests_active Number of requests currently active
# TYPE jetty_requests_active gauge
jetty_requests_active 1.0
# HELP jetty_requests_active_max Maximum number of requests that have been active at once
# TYPE jetty_requests_active_max gauge
jetty_requests_active_max 1.0
# HELP jetty_request_time_max_seconds Maximum time spent handling requests
# TYPE jetty_request_time_max_seconds gauge
jetty_request_time_max_seconds 0.087
# HELP jetty_request_time_seconds_total Total time spent in all request handling
# TYPE jetty_request_time_seconds_total counter
jetty_request_time_seconds_total 0.126
# HELP jetty_dispatched_total Number of dispatches
# TYPE jetty_dispatched_total counter
jetty_dispatched_total 21.0
# HELP jetty_dispatched_active Number of dispatches currently active
# TYPE jetty_dispatched_active gauge
jetty_dispatched_active 1.0
# HELP jetty_dispatched_active_max Maximum number of active dispatches being handled
# TYPE jetty_dispatched_active_max gauge
jetty_dispatched_active_max 1.0
# HELP jetty_dispatched_time_max Maximum time spent in dispatch handling
# TYPE jetty_dispatched_time_max gauge
jetty_dispatched_time_max 87.0
# HELP jetty_dispatched_time_seconds_total Total time spent in dispatch handling
# TYPE jetty_dispatched_time_seconds_total counter
jetty_dispatched_time_seconds_total 0.126
# HELP jetty_async_requests_total Total number of async requests
# TYPE jetty_async_requests_total counter
jetty_async_requests_total 0.0
# HELP jetty_async_requests_waiting Currently waiting async requests
# TYPE jetty_async_requests_waiting gauge
jetty_async_requests_waiting 0.0
# HELP jetty_async_requests_waiting_max Maximum number of waiting async requests
# TYPE jetty_async_requests_waiting_max gauge
jetty_async_requests_waiting_max 0.0
# HELP jetty_async_dispatches_total Number of requested that have been asynchronously dispatched
# TYPE jetty_async_dispatches_total counter
jetty_async_dispatches_total 0.0
# HELP jetty_expires_total Number of async requests requests that have expired
# TYPE jetty_expires_total counter
jetty_expires_total 0.0
# HELP jetty_responses_total Number of requests with response status
# TYPE jetty_responses_total counter
jetty_responses_total{code="1xx",} 0.0
jetty_responses_total{code="2xx",} 18.0
jetty_responses_total{code="3xx",} 0.0
jetty_responses_total{code="4xx",} 2.0
jetty_responses_total{code="5xx",} 0.0
# HELP jetty_stats_seconds Time in seconds stats have been collected for
# TYPE jetty_stats_seconds gauge
jetty_stats_seconds 8.821
# HELP jetty_responses_bytes_total Total number of bytes across all responses
# TYPE jetty_responses_bytes_total counter
jetty_responses_bytes_total 62374.0
# HELP jetty_queued_thread_pool_threads Number of total threads
# TYPE jetty_queued_thread_pool_threads gauge
jetty_queued_thread_pool_threads 16.0
# HELP jetty_queued_thread_pool_utilization Percentage of threads in use
# TYPE jetty_queued_thread_pool_utilization gauge
jetty_queued_thread_pool_utilization 1.0
# HELP jetty_queued_thread_pool_threads_idle Number of idle threads
# TYPE jetty_queued_thread_pool_threads_idle gauge
jetty_queued_thread_pool_threads_idle 8.0
# HELP jetty_queued_thread_pool_jobs Number of total jobs
# TYPE jetty_queued_thread_pool_jobs gauge
jetty_queued_thread_pool_jobs 0.0
```