"use client";

import { Card, Col, Row, Statistic, Typography } from "antd";
import type { ReactNode } from "react";

const { Text } = Typography;

type KPIItem = {
  title: string;
  value: number | string;
  suffix?: string;
  delta?: string;
  footnote?: string;
  icon?: ReactNode;
  onClick?: () => void;
};

/**
 * KPI网格组件 - 科技风深蓝配色
 * 
 * 显示关键性能指标的网格布局。
 */
export default function KPIGrid({ items }: { items: KPIItem[] }) {
  return (
    <Row gutter={[12, 12]}>
      {items.map((it, idx) => (
        <Col xs={24} sm={12} lg={8} xl={4} key={idx}>
          <Card
            className="glass-card"
            hoverable={Boolean(it.onClick)}
            onClick={it.onClick}
            styles={{ body: { padding: 20 } }}
          >
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>{it.title}</span>} 
              value={it.value} 
              suffix={it.suffix} 
              prefix={<span style={{ color: "#00d4ff", marginRight: 8 }}>{it.icon}</span>}
              valueStyle={{ color: "#e8f4ff", fontWeight: 700, fontSize: 24 }}
            />
            {it.delta ? (
              <Text style={{ 
                display: "block", 
                marginTop: 6,
                color: it.delta.startsWith("-") ? "#ff4757" : "#00e676",
              }}>
                {it.delta}
              </Text>
            ) : null}
            {it.footnote ? (
              <Text style={{ 
                display: "block", 
                marginTop: 4,
                color: "#5a6a7a",
                fontSize: 12,
              }}>
                {it.footnote}
              </Text>
            ) : null}
          </Card>
        </Col>
      ))}
    </Row>
  );
}
