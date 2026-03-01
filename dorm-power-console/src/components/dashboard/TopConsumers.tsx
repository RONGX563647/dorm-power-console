"use client";

import { Card, Divider, Progress, Tag, Typography } from "antd";

const { Text } = Typography;

/**
 * Top能耗设备组件 - 科技风深蓝配色
 * 
 * 显示能耗最高的设备列表。
 */
export default function TopConsumers({
  items,
}: {
  items: { name: string; value: number; percent: number }[];
}) {
  return (
    <Card 
      className="glass-card" 
      title={<span style={{ color: "#e8f4ff" }}>Top 能耗设备</span>} 
      styles={{ body: { padding: 14 } }}
    >
      {items.length === 0 ? (
        <Text style={{ color: "#8ba3c7" }}>暂无数据</Text>
      ) : (
        items.map((item, idx) => (
          <div key={`${item.name}-${idx}`} style={{ padding: "8px 0" }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <Tag 
                    style={{
                      marginInlineEnd: 0,
                      borderRadius: 999,
                      background: idx === 0 ? "rgba(255, 184, 0, 0.15)" : idx === 1 ? "rgba(0, 153, 255, 0.15)" : "rgba(139, 163, 199, 0.15)",
                      border: `1px solid ${idx === 0 ? "rgba(255, 184, 0, 0.4)" : idx === 1 ? "rgba(0, 153, 255, 0.4)" : "rgba(139, 163, 199, 0.4)"}`,
                      color: idx === 0 ? "#ffb800" : idx === 1 ? "#0099ff" : "#8ba3c7",
                    }}
                  >
                    #{idx + 1}
                  </Tag>
                  <Text style={{ color: "#e8f4ff" }}>{item.name}</Text>
                </div>
                <Text style={{ color: "#8ba3c7", fontSize: 12 }}>{item.value.toFixed(2)} kWh (估算)</Text>
              </div>
              <div style={{ width: 130 }}>
                <Progress 
                  percent={item.percent} 
                  size="small" 
                  showInfo={false} 
                  strokeColor={{ from: "#00d4ff", to: "#0099ff" }}
                  trailColor="rgba(0, 212, 255, 0.1)"
                />
              </div>
            </div>
            {idx < items.length - 1 ? <Divider style={{ margin: "10px 0 0", borderColor: "rgba(0, 212, 255, 0.1)" }} /> : null}
          </div>
        ))
      )}
    </Card>
  );
}
