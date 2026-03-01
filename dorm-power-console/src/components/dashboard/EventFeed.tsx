"use client";

import { Card, Tag, Timeline, Typography } from "antd";
import {
  CheckCircleOutlined,
  CloudUploadOutlined,
  WarningOutlined,
  ToolOutlined,
} from "@ant-design/icons";

const { Text } = Typography;

type EventItem = {
  id: string;
  type: "REPORT" | "CMD" | "ALERT" | "SYSTEM";
  time: string;
  detail: string;
  status?: "ok" | "warn" | "fail";
};

function colorOf(type: EventItem["type"]) {
  if (type === "ALERT") return "#ff4757";
  if (type === "CMD") return "#00d4ff";
  if (type === "SYSTEM") return "#ffb800";
  return "#00e676";
}

function iconOf(type: EventItem["type"]) {
  if (type === "ALERT") return <WarningOutlined style={{ color: "#ff4757" }} />;
  if (type === "CMD") return <ToolOutlined style={{ color: "#00d4ff" }} />;
  if (type === "SYSTEM") return <CheckCircleOutlined style={{ color: "#ffb800" }} />;
  return <CloudUploadOutlined style={{ color: "#00e676" }} />;
}

function statusColor(status: NonNullable<EventItem["status"]>) {
  if (status === "ok") return "#00e676";
  if (status === "warn") return "#ffb800";
  return "#ff4757";
}

function statusBg(status: NonNullable<EventItem["status"]>) {
  if (status === "ok") return "rgba(0, 230, 118, 0.15)";
  if (status === "warn") return "rgba(255, 184, 0, 0.15)";
  return "rgba(255, 71, 87, 0.15)";
}

function statusBorder(status: NonNullable<EventItem["status"]>) {
  if (status === "ok") return "rgba(0, 230, 118, 0.4)";
  if (status === "warn") return "rgba(255, 184, 0, 0.4)";
  return "rgba(255, 71, 87, 0.4)";
}

/**
 * 事件流组件 - 科技风深蓝配色
 * 
 * 显示系统事件和异常的时间线。
 */
export default function EventFeed({ items }: { items: EventItem[] }) {
  return (
    <Card 
      className="glass-card" 
      title={<span style={{ color: "#e8f4ff" }}>异常与事件流</span>} 
      styles={{ body: { padding: 14 } }}
    >
      {items.length === 0 ? (
        <Text style={{ color: "#8ba3c7" }}>暂无事件</Text>
      ) : (
        <Timeline
          items={items.map((item) => ({
            dot: iconOf(item.type),
            color: colorOf(item.type),
            children: (
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                  <Tag 
                    style={{
                      marginInlineEnd: 0,
                      borderRadius: 999,
                      background: `${colorOf(item.type)}20`,
                      border: `1px solid ${colorOf(item.type)}60`,
                      color: colorOf(item.type),
                    }}
                  >
                    {item.type}
                  </Tag>
                  <Text style={{ color: "#e8f4ff" }}>{item.detail}</Text>
                  {item.status ? (
                    <Tag 
                      style={{
                        borderRadius: 999,
                        background: statusBg(item.status),
                        border: `1px solid ${statusBorder(item.status)}`,
                        color: statusColor(item.status),
                      }}
                    >
                      {item.status}
                    </Tag>
                  ) : null}
                </div>
                <Text style={{ color: "#5a6a7a", fontSize: 12 }}>{item.time}</Text>
              </div>
            ),
          }))}
        />
      )}
    </Card>
  );
}
