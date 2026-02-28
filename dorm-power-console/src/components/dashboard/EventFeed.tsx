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
  if (type === "ALERT") return "red";
  if (type === "CMD") return "blue";
  if (type === "SYSTEM") return "gold";
  return "green";
}

function iconOf(type: EventItem["type"]) {
  if (type === "ALERT") return <WarningOutlined />;
  if (type === "CMD") return <ToolOutlined />;
  if (type === "SYSTEM") return <CheckCircleOutlined />;
  return <CloudUploadOutlined />;
}

function statusColor(status: NonNullable<EventItem["status"]>) {
  if (status === "ok") return "green";
  if (status === "warn") return "orange";
  return "red";
}

export default function EventFeed({ items }: { items: EventItem[] }) {
  return (
    <Card className="glass-card" title="异常与事件流" styles={{ body: { padding: 14 } }}>
      {items.length === 0 ? (
        <Text type="secondary">暂无事件</Text>
      ) : (
        <Timeline
          items={items.map((item) => ({
            icon: iconOf(item.type),
            color: colorOf(item.type),
            content: (
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                  <Tag color={colorOf(item.type)} style={{ marginInlineEnd: 0 }}>{item.type}</Tag>
                  <Text>{item.detail}</Text>
                  {item.status ? <Tag color={statusColor(item.status)}>{item.status}</Tag> : null}
                </div>
                <Text type="secondary">{item.time}</Text>
              </div>
            ),
          }))}
        />
      )}
    </Card>
  );
}
