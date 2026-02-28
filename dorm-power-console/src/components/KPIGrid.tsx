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
            <Statistic title={it.title} value={it.value} suffix={it.suffix} prefix={it.icon} />
            {it.delta ? (
              <Text type={it.delta.startsWith("-") ? "danger" : "success"} style={{ display: "block", marginTop: 6 }}>
                {it.delta}
              </Text>
            ) : null}
            {it.footnote ? (
              <Text type="secondary" style={{ display: "block", marginTop: 4 }}>
                {it.footnote}
              </Text>
            ) : null}
          </Card>
        </Col>
      ))}
    </Row>
  );
}
