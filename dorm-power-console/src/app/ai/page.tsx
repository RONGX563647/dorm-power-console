"use client";

import AppLayout from "@/components/AppLayout";
import { Card, Col, Progress, Row, Tag, Typography } from "antd";
import { 
  RobotOutlined, 
  WarningOutlined, 
  BulbOutlined, 
  SafetyOutlined,
  ThunderboltOutlined,
  LineChartOutlined,
} from "@ant-design/icons";

const { Text, Title } = Typography;

const cards = [
  {
    title: "本周行为模式",
    tag: "Pattern",
    icon: <LineChartOutlined />,
    content: "夜间用电集中在 23:30-01:00，学习时段负载稳定。",
  },
  {
    title: "设备识别",
    tag: "Detection",
    icon: <ThunderboltOutlined />,
    content: "插孔1：PC + 显示器（置信度 82%），插孔3：Lamp/Adapter。",
  },
  {
    title: "风险预测",
    tag: "Forecast",
    icon: <WarningOutlined />,
    content: "预计明晚 23:40 出现峰值区间，建议提前切换节能策略。",
  },
  {
    title: "节能建议",
    tag: "Advice",
    icon: <BulbOutlined />,
    content: "建议开启睡眠模式：00:30 后低功率待机插孔自动断电。",
  },
  {
    title: "违规负载识别",
    tag: "Safety",
    icon: <SafetyOutlined />,
    content: "检测到纯阻性高功率瞬时拉升特征，疑似违规电器。",
  },
  {
    title: "自动化策略命中",
    tag: "Policy",
    icon: <RobotOutlined />,
    content: "本周策略触发 38 次，预计节省 14.6% 待机能耗。",
  },
];

/**
 * AI分析页面组件 - 科技风深蓝配色
 * 
 * 显示AI生成的分析报告和建议。
 */
export default function AIPage() {
  return (
    <AppLayout title="AI 分析 AI Insights">
      <Card 
        className="glass-card" 
        styles={{ body: { padding: 18 } }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 4 }}>
          <RobotOutlined style={{ fontSize: 24, color: "#00d4ff" }} />
          <Title level={5} style={{ margin: 0, color: "#e8f4ff" }}>AI 周报（演示版）</Title>
        </div>
        <Text style={{ color: "#8ba3c7" }}>基于历史功率曲线、行为模式和异常事件自动生成。</Text>
        <div style={{ marginTop: 16 }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <Text style={{ color: "#8ba3c7" }}>违规负载风险指数</Text>
            <Text style={{ color: "#ff4757", fontWeight: 700 }}>73%</Text>
          </div>
          <Progress 
            percent={73} 
            status="active" 
            strokeColor={{ from: "#ff4757", to: "#ffb800" }}
            trailColor="rgba(255, 71, 87, 0.1)"
          />
        </div>
      </Card>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {cards.map((it) => (
          <Col xs={24} md={12} lg={8} key={it.title}>
            <Card 
              className="glass-card" 
              title={
                <span style={{ color: "#e8f4ff", display: "flex", alignItems: "center", gap: 8 }}>
                  <span style={{ color: "#00d4ff" }}>{it.icon}</span>
                  {it.title}
                </span>
              } 
              extra={
                <Tag style={{
                  borderRadius: 999,
                  background: "rgba(0, 212, 255, 0.15)",
                  border: "1px solid rgba(0, 212, 255, 0.3)",
                  color: "#00d4ff",
                }}>
                  {it.tag}
                </Tag>
              } 
              styles={{ body: { minHeight: 120 } }}
            >
              <Text style={{ color: "#8ba3c7" }}>{it.content}</Text>
            </Card>
          </Col>
        ))}
      </Row>
    </AppLayout>
  );
}
