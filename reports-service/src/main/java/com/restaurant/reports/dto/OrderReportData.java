package com.restaurant.reports.dto;

import java.util.List;

public record OrderReportData(
        long totalOrders,
        long completedOrders,
        long cancelledOrders,
        double totalRevenue,
        double avgOrderValue,
        List<TopItemData> topItems
) {}
