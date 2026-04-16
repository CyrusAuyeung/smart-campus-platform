package com.unikorn.campus.dashboard;

import java.util.List;

public record DashboardSummary(
    String title,
    String subtitle,
    List<MetricCard> metrics,
    List<String> highlights
) {
}
