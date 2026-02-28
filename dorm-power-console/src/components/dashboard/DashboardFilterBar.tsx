"use client";

import PageToolbar from "@/components/common/PageToolbar";

export type TimeRange = "60s" | "24h" | "7d";

export default function DashboardFilterBar({
  device,
  onDeviceChange,
  deviceOptions,
  timeRange,
  onTimeRangeChange,
  autoRefresh,
  onAutoRefreshChange,
  lastUpdated,
  onRefresh,
}: {
  device: string;
  onDeviceChange: (device: string) => void;
  deviceOptions?: Array<{ value: string; label: string }>;
  timeRange: TimeRange;
  onTimeRangeChange: (range: TimeRange) => void;
  autoRefresh: boolean;
  onAutoRefreshChange: (enabled: boolean) => void;
  lastUpdated?: number;
  onRefresh: () => void;
}) {
  return (
    <PageToolbar
      device={device}
      onDeviceChange={onDeviceChange}
      deviceOptions={deviceOptions ?? [
        { value: "strip01", label: "A-302 / strip01" },
        { value: "strip02", label: "A-302 / strip02" },
        { value: "strip03", label: "A-303 / strip03" },
      ]}
      timeRange={timeRange}
      onTimeRangeChange={(v) => onTimeRangeChange(v as TimeRange)}
      timeRangeOptions={["60s", "24h", "7d"]}
      autoRefresh={autoRefresh}
      onAutoRefreshChange={onAutoRefreshChange}
      lastUpdated={lastUpdated}
      onRefresh={onRefresh}
    />
  );
}
