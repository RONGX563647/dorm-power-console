"use client";

import { Card, Divider, Progress, Tag, Typography } from "antd";

const { Text } = Typography;

export default function TopConsumers({
  items,
}: {
  items: { name: string; value: number; percent: number }[];
}) {
  return (
    <Card className="glass-card" title="Top 能耗设备" styles={{ body: { padding: 14 } }}>
      {items.length === 0 ? (
        <Text type="secondary">暂无数据</Text>
      ) : (
        items.map((item, idx) => (
          <div key={`${item.name}-${idx}`} style={{ padding: "8px 0" }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <Tag color={idx === 0 ? "gold" : idx === 1 ? "geekblue" : "default"} style={{ marginInlineEnd: 0 }}>#{idx + 1}</Tag>
                  <Text>{item.name}</Text>
                </div>
                <Text type="secondary">{item.value.toFixed(2)} kWh (估算)</Text>
              </div>
              <div style={{ width: 130 }}>
                <Progress percent={item.percent} size="small" showInfo={false} strokeColor="#0f7fb0" />
              </div>
            </div>
            {idx < items.length - 1 ? <Divider style={{ margin: "10px 0 0" }} /> : null}
          </div>
        ))
      )}
    </Card>
  );
}
