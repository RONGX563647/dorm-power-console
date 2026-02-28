"use client";

import { useEffect, useMemo, useState } from "react";
import AppLayout from "@/components/AppLayout";
import type { StripStatus, TelemetryPoint } from "@/components/types";
import { fetchJSON } from "@/lib/fetcher";
import { Card, Col, Row, Statistic, Tag, message } from "antd";
import PowerLineChart from "@/components/PowerLineChart";
import SocketMatrix from "@/components/SocketMatrix";
import { useParams } from "next/navigation";
import PageToolbar from "@/components/common/PageToolbar";

type RangeKey = "60s" | "24h" | "7d";

type CmdSubmitResp = {
  cmdId: string;
};

type CmdStateResp = {
  cmdId: string;
  state: "pending" | "success" | "failed" | "timeout" | "cancelled";
};

function applySocketState(prev: StripStatus | null, socketId: number, nextOn: boolean): StripStatus | null {
  if (!prev) return prev;
  const sockets = prev.sockets.map((s) => (
    s.id === socketId
      ? { ...s, on: nextOn, power_w: nextOn ? s.power_w : 0 }
      : s
  ));
  const total_power_w = sockets.reduce((sum, s) => sum + s.power_w, 0);
  return { ...prev, sockets, total_power_w };
}

export default function DeviceDetailPage() {
  const params = useParams<{ id: string }>();
  const rawId = params.id;
  const id = decodeURIComponent(rawId);

  const [range, setRange] = useState<RangeKey>("60s");
  const [status, setStatus] = useState<StripStatus | null>(null);
  const [points, setPoints] = useState<TelemetryPoint[]>([]);
  const [lastUpdated, setLastUpdated] = useState<number>();

  useEffect(() => {
    let alive = true;

    async function load() {
      const s = await fetchJSON<StripStatus>(`/api/devices/${id}/status`);
      const t = await fetchJSON<TelemetryPoint[]>(`/api/telemetry?device=${id}&range=${range}`);
      if (!alive) return;
      setStatus(s);
      setPoints(t);
      setLastUpdated(Date.now());
    }

    load();
    const timer = setInterval(load, range === "60s" ? 2000 : 5000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [id, range]);

  const avg = useMemo(() => {
    if (!points.length) return 0;
    return points.reduce((s, p) => s + p.power_w, 0) / points.length;
  }, [points]);

  const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

  const executeBackendCommand = async (payload: Record<string, unknown>): Promise<"success" | "failed" | "timeout"> => {
    const submitRes = await fetch(`/api/strips/${encodeURIComponent(id)}/cmd`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!submitRes.ok) return "failed";

    const submitted = (await submitRes.json().catch(() => ({}))) as CmdSubmitResp;
    if (!submitted.cmdId) return "failed";

    const deadline = Date.now() + 4500;
    while (Date.now() < deadline) {
      await sleep(550);
      const stateRes = await fetch(`/api/cmd/${encodeURIComponent(submitted.cmdId)}`, { cache: "no-store" });
      if (!stateRes.ok) continue;
      const cmd = (await stateRes.json().catch(() => ({}))) as CmdStateResp;
      if (cmd.state === "pending") continue;
      if (cmd.state === "success") return "success";
      if (cmd.state === "timeout") return "timeout";
      return "failed";
    }
    return "timeout";
  };

  const onToggle = async (socketId: number, nextOn: boolean) => {
    try {
      const result = await executeBackendCommand({ socket: socketId, action: nextOn ? "on" : "off" });
      if (result === "failed") {
        message.error(`插孔${socketId} 控制失败`);
        return;
      }
      if (result === "timeout") {
        message.warning(`插孔${socketId} 控制超时，未收到设备确认`);
        return;
      }
      setStatus((prev) => applySocketState(prev, socketId, nextOn));
      message.success(`${id} 插孔${socketId} -> ${nextOn ? "ON" : "OFF"}`);
      reload().catch(console.error);
    } catch {
      message.error("命令下发失败");
    }
  };

  const reload = async () => {
    const [s, t] = await Promise.all([
      fetchJSON<StripStatus>(`/api/devices/${id}/status`),
      fetchJSON<TelemetryPoint[]>(`/api/telemetry?device=${id}&range=${range}`),
    ]);
    setStatus(s);
    setPoints(t);
    setLastUpdated(Date.now());
  };

  return (
    <AppLayout title={`设备详情 ${id}`}>
      <PageToolbar
        timeRange={range}
        onTimeRangeChange={(v) => setRange(v as RangeKey)}
        timeRangeOptions={["60s", "24h", "7d"]}
        lastUpdated={lastUpdated}
        onRefresh={() => reload().catch(console.error)}
      />

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card styles={{ body: { padding: 18 } }}><Statistic title="在线" value={status?.online ? "Online" : "Offline"} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card styles={{ body: { padding: 18 } }}><Statistic title="总功率" value={status ? status.total_power_w.toFixed(1) : "--"} suffix="W" /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card styles={{ body: { padding: 18 } }}><Statistic title="电压" value={status ? status.voltage_v.toFixed(1) : "--"} suffix="V" /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card styles={{ body: { padding: 18 } }}><Statistic title="电流" value={status ? status.current_a.toFixed(2) : "--"} suffix="A" /></Card>
        </Col>
      </Row>

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} lg={16}>
          <PowerLineChart title="最近功率曲线" points={points} threshold={150} showStats range={range} />
        </Col>
        <Col xs={24} lg={8}>
          <Card title="风险提示" styles={{ body: { padding: 16 } }}>
            {(status?.total_power_w ?? 0) > avg * 1.35 && avg > 0 ? (
              <Tag color="orange">高于历史平均 35%</Tag>
            ) : (
              <Tag color="green">当前波动正常</Tag>
            )}
          </Card>
        </Col>
        <Col span={24}>
          <SocketMatrix sockets={status?.sockets ?? []} onToggle={onToggle} />
        </Col>
      </Row>
    </AppLayout>
  );
}
