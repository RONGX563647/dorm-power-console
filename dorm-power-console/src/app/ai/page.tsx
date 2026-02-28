"use client";

import AppLayout from "@/components/AppLayout";
import { Card, Col, Progress, Row, Tag, Typography } from "antd";

const { Text, Title } = Typography;

const cards = [
  {
    title: "本周行为模式",
    tag: "Pattern",
    content: "夜间用电集中在 23:30-01:00，学习时段负载稳定。",
  },
  {
    title: "设备识别",
    tag: "Detection",
    content: "插孔1：PC + 显示器（置信度 82%），插孔3：Lamp/Adapter。",
  },
  {
    title: "风险预测",
    tag: "Forecast",
    content: "预计明晚 23:40 出现峰值区间，建议提前切换节能策略。",
  },
  {
    title: "节能建议",
    tag: "Advice",
    content: "建议开启睡眠模式：00:30 后低功率待机插孔自动断电。",
  },
  {
    title: "违规负载识别",
    tag: "Safety",
    content: "检测到纯阻性高功率瞬时拉升特征，疑似违规电器。",
  },
  {
    title: "自动化策略命中",
    tag: "Policy",
    content: "本周策略触发 38 次，预计节省 14.6% 待机能耗。",
  },
];

export default function AIPage() {
  return (
    <AppLayout title="AI 分析 AI Insights">
      <Card className="glass-card" styles={{ body: { padding: 18 } }}>
        <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>AI 周报（演示版）</Title>
        <Text type="secondary">基于历史功率曲线、行为模式和异常事件自动生成。</Text>
        <div style={{ marginTop: 12 }}>
          <Text type="secondary">违规负载风险指数</Text>
          <Progress percent={73} status="active" strokeColor="#d64545" />
        </div>
      </Card>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {cards.map((it) => (
          <Col xs={24} md={12} lg={8} key={it.title}>
            <Card className="glass-card" title={it.title} extra={<Tag color="blue">{it.tag}</Tag>} styles={{ body: { minHeight: 120 } }}>
              <Text>{it.content}</Text>
            </Card>
          </Col>
        ))}
      </Row>
    </AppLayout>
  );
}
