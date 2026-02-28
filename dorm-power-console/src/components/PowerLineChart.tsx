"use client";

import ReactECharts from "echarts-for-react";
import { Card, Space, Typography } from "antd";
import type { TelemetryPoint } from "@/components/types";

const { Text } = Typography;

/**
 * 图表时间范围类型
 * 定义了支持的遥测数据时间范围
 */
export type ChartRange = "60s" | "24h" | "7d" | "30d";

/**
 * 命令标记类型
 * 定义了在图表上标记的命令信息
 */
export type CmdMarker = {
  ts: number; // 命令时间戳
  label: string; // 命令标签
  status: "success" | "failed" | "timeout" | "pending"; // 命令状态
};

/**
 * 根据遥测数据点推断时间范围
 * 
 * @param points 遥测数据点数组
 * @returns 时间范围
 */
function inferRange(points: TelemetryPoint[]): ChartRange {
  if (points.length < 2) return "60s";
  const spanSec = points[points.length - 1].ts - points[0].ts;
  if (spanSec >= 25 * 24 * 3600) return "30d"; // 超过25天
  if (spanSec >= 6 * 24 * 3600) return "7d"; // 超过6天
  if (spanSec >= 12 * 3600) return "24h"; // 超过12小时
  return "60s"; // 默认60秒
}

function formatXAxis(ts: number, range: ChartRange) {
  const d = new Date(ts * 1000);
  if (range === "60s") return d.toLocaleTimeString();
  if (range === "24h") return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  if (range === "7d") return d.toLocaleString([], { month: "2-digit", day: "2-digit", hour: "2-digit" });
  return d.toLocaleDateString([], { month: "2-digit", day: "2-digit" });
}

function markerColor(status: CmdMarker["status"]) {
  if (status === "success") return "#52c41a";
  if (status === "pending") return "#1677ff";
  if (status === "failed") return "#ff4d4f";
  return "#fa8c16";
}

function buildAlertAreas(values: number[], threshold?: number) {
  if (!threshold || values.length === 0) return undefined;

  const segments: Array<{ start: number; end: number }> = [];
  let start = -1;

  for (let i = 0; i < values.length; i += 1) {
    if (values[i] > threshold && start < 0) start = i;
    if ((values[i] <= threshold || i === values.length - 1) && start >= 0) {
      const end = values[i] <= threshold ? i : i;
      segments.push({ start, end });
      start = -1;
    }
  }

  if (!segments.length) return undefined;

  return {
    silent: true,
    itemStyle: { color: "rgba(255,77,79,0.10)" },
    data: segments.map((s) => [
      { xAxis: s.start },
      { xAxis: s.end },
    ]),
  };
}

function buildCmdMarkPoints(points: TelemetryPoint[], cmdMarkers: CmdMarker[] | undefined) {
  if (!cmdMarkers?.length || !points.length) return undefined;

  const data = cmdMarkers.map((m) => {
    let idx = 0;
    let minDiff = Number.POSITIVE_INFINITY;

    for (let i = 0; i < points.length; i += 1) {
      const diff = Math.abs(points[i].ts - m.ts);
      if (diff < minDiff) {
        minDiff = diff;
        idx = i;
      }
    }

    return {
      name: m.label,
      coord: [idx, points[idx].power_w],
      value: m.label,
      itemStyle: { color: markerColor(m.status) },
      label: { formatter: m.label, color: "#262626", fontSize: 11, backgroundColor: "#fff", padding: [2, 4], borderRadius: 4 },
    };
  });

  return {
    symbol: "pin",
    symbolSize: 18,
    data,
  };
}

export default function PowerLineChart({
  title,
  points,
  threshold,
  showStats = false,
  range,
  cmdMarkers,
  highlightAlerts = true,
}: {
  title: string;
  points: TelemetryPoint[];
  threshold?: number;
  showStats?: boolean;
  range?: ChartRange;
  cmdMarkers?: CmdMarker[];
  highlightAlerts?: boolean;
}) {
  const values = points.map((p) => p.power_w);
  const peak = values.length ? Math.max(...values) : 0;
  const min = values.length ? Math.min(...values) : 0;
  const avg = values.length ? values.reduce((s, v) => s + v, 0) / values.length : 0;
  const displayRange = range ?? inferRange(points);

  const alertAreas = highlightAlerts ? buildAlertAreas(values, threshold) : undefined;
  const markPoint = buildCmdMarkPoints(points, cmdMarkers);

  const option = {
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: points.map((p) => formatXAxis(p.ts, displayRange)),
      axisLabel: { hideOverlap: true },
    },
    yAxis: { type: "value", name: "W" },
    series: [
      {
        name: "Power",
        type: "line",
        data: values,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2, color: "#1677ff" },
        
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(15,127,176,0.35)" },
              { offset: 1, color: "rgba(43,165,143,0.04)" },
            ],
          },
        },
        markLine: threshold
          ? {
              symbol: "none",
              lineStyle: { type: "dashed", color: "#ff4d4f" },
              data: [{ yAxis: threshold, name: "阈值" }],
            }
          : undefined,
        markArea: alertAreas,
        markPoint,
      },
    ],
    dataZoom: [{ type: "inside" }],
    grid: { left: 40, right: 16, top: 24, bottom: 32 },
  };

  return (
    <Card
      className="glass-card"
      title={title}
      styles={{ body: { padding: 20 } }}
      extra={
        showStats ? (
          <Space size={12}>
            <Text type="secondary">峰值 {peak.toFixed(1)}W</Text>
            <Text type="secondary">均值 {avg.toFixed(1)}W</Text>
            <Text type="secondary">最小 {min.toFixed(1)}W</Text>
          </Space>
        ) : null
      }
    >
      <ReactECharts option={option} style={{ height: 320 }} />
    </Card>
  );
}
