"use client";

import type { ReactNode } from "react";
import { Button, Card, Cascader, Segmented, Select, Space, Switch, Typography } from "antd";

const { Text } = Typography;

type Option = { value: string; label: string };

type CascaderNode = {
  value: string;
  label: string;
  children?: CascaderNode[];
};

function buildCascader(deviceOptions: Option[]): CascaderNode[] {
  const root = new Map<string, Map<string, Option[]>>();
  for (const item of deviceOptions) {
    const chunks = item.label.split("/").map((x) => x.trim()).filter(Boolean);
    const room = chunks[0] ?? "未分组";
    const dev = chunks[1] ?? item.value;
    const building = room.includes("-") ? room.split("-")[0] : "默认楼";
    if (!root.has(building)) root.set(building, new Map());
    const rooms = root.get(building)!;
    if (!rooms.has(room)) rooms.set(room, []);
    rooms.get(room)!.push({ value: item.value, label: dev });
  }

  return Array.from(root.entries()).map(([building, rooms]) => ({
    value: building,
    label: building,
    children: Array.from(rooms.entries()).map(([room, devices]) => ({
      value: room,
      label: room,
      children: devices.map((d) => ({ value: d.value, label: d.label })),
    })),
  }));
}

export default function PageToolbar({
  device,
  onDeviceChange,
  deviceOptions,
  deviceLabel = "设备",
  timeRange,
  onTimeRangeChange,
  timeRangeOptions,
  autoRefresh,
  onAutoRefreshChange,
  lastUpdated,
  onRefresh,
  leftExtra,
  rightExtra,
}: {
  device?: string;
  onDeviceChange?: (device: string) => void;
  deviceOptions?: Option[];
  deviceLabel?: string;
  timeRange?: string;
  onTimeRangeChange?: (range: string) => void;
  timeRangeOptions?: string[];
  autoRefresh?: boolean;
  onAutoRefreshChange?: (enabled: boolean) => void;
  lastUpdated?: number;
  onRefresh?: () => void;
  leftExtra?: ReactNode;
  rightExtra?: ReactNode;
}) {
  const cascaderOptions = deviceOptions ? buildCascader(deviceOptions) : [];

  return (
    <Card className="glass-card" styles={{ body: { padding: 16 } }}>
      <Space wrap size={12} style={{ width: "100%", justifyContent: "space-between" }}>
        <Space wrap size={12}>
          {device && onDeviceChange && deviceOptions ? (
            <>
              <Text type="secondary">{deviceLabel}：</Text>
              <Cascader
                placeholder="选择楼栋/房间/设备"
                options={cascaderOptions}
                onChange={(v) => {
                  const last = v[v.length - 1];
                  if (typeof last === "string") onDeviceChange(last);
                }}
                style={{ width: 240 }}
                displayRender={(labels) => labels.join(" / ")}
              />
              <Select
                value={device}
                onChange={onDeviceChange}
                style={{ width: 160 }}
                options={deviceOptions}
                popupMatchSelectWidth={false}
                styles={{ popup: { root: { minWidth: 260 } } }}
              />
            </>
          ) : null}

          {timeRange && onTimeRangeChange && timeRangeOptions ? (
            <Segmented
              value={timeRange}
              onChange={(v) => onTimeRangeChange(String(v))}
              options={timeRangeOptions}
            />
          ) : null}

          {leftExtra}
        </Space>

        <Space wrap size={12}>
          {typeof autoRefresh === "boolean" && onAutoRefreshChange ? (
            <>
              <Text type="secondary">实时刷新</Text>
              <Switch checked={autoRefresh} onChange={onAutoRefreshChange} />
            </>
          ) : null}

          {typeof lastUpdated === "number" ? (
            <Text type="secondary">最后更新：{new Date(lastUpdated).toLocaleTimeString()}</Text>
          ) : null}

          {onRefresh ? <Button onClick={onRefresh}>手动刷新</Button> : null}
          {rightExtra}
        </Space>
      </Space>
    </Card>
  );
}
