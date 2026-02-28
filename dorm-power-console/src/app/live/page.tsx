"use client";

import { useEffect, useMemo, useState } from "react";
import AppLayout from "@/components/AppLayout";
import type { Device, StripStatus, TelemetryPoint } from "@/components/types";
import { fetchJSON } from "@/lib/fetcher";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Flex,
  Row,
  Statistic,
  Tag,
  Timeline,
  message,
} from "antd";
import PowerLineChart, { type CmdMarker } from "@/components/PowerLineChart";
import PageToolbar from "@/components/common/PageToolbar";
import { useCmdDispatcher } from "@/hooks/useCmdDispatcher";
import { useRole } from "@/components/RoleProvider";

type RangeKey = "60s" | "24h" | "7d";
type ModeKey = "learn" | "eco" | "away" | null;
type TimerKey = "10m" | "30m" | "1h" | null;
type PowerState = "on" | "off" | null;
type CmdResult = "success" | "failed" | "timeout";

type CmdSubmitResp = {
  cmdId: string;
};

type CmdStateResp = {
  cmdId: string;
  state: "pending" | "success" | "failed" | "timeout" | "cancelled";
};

export default function LivePage() {
  const { canControl } = useRole();
  const [device, setDevice] = useState("");
  const [devices, setDevices] = useState<Device[]>([]);
  const [range, setRange] = useState<RangeKey>("60s");
  const [status, setStatus] = useState<StripStatus | null>(null);
  const [points, setPoints] = useState<TelemetryPoint[]>([]);
  const [lastUpdated, setLastUpdated] = useState<number>();

  const [powerState, setPowerState] = useState<PowerState>(null);
  const [mode, setMode] = useState<ModeKey>(null);
  const [timerPreset, setTimerPreset] = useState<TimerKey>(null);

  const dispatcher = useCmdDispatcher(4500);

  const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

  useEffect(() => {
    let alive = true;

    async function load() {
      const devs = await fetchJSON<Device[]>("/api/devices");
      const target = devs.some((d) => d.id === device) ? device : (devs[0]?.id ?? "");
      if (!target) {
        if (!alive) return;
        setDevices(devs);
        setStatus(null);
        setPoints([]);
        setLastUpdated(Date.now());
        return;
      }
      const [s, t] = await Promise.all([
        fetchJSON<StripStatus>(`/api/devices/${target}/status`),
        fetchJSON<TelemetryPoint[]>(`/api/telemetry?device=${target}&range=${range}`),
      ]);
      if (!alive) return;
      if (target !== device) setDevice(target);
      setDevices(devs);
      setStatus(s);
      setPoints(t);
      setLastUpdated(Date.now());
    }

    load();
    const timer = setInterval(load, range === "60s" ? 1500 : 5000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [device, range]);

  const executeBackendCommand = async (payload: Record<string, unknown>): Promise<CmdResult> => {
    const submitRes = await fetch(`/api/strips/${encodeURIComponent(device)}/cmd`, {
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

  const runCommand = async (
    action: string,
    payload: Record<string, unknown>,
    onSuccess: () => void,
  ) => {
    const result = await dispatcher.runCommand(action, onSuccess, {
      executor: () => executeBackendCommand(payload),
    });
    if (result === "success") {
      message.success(`命令成功：${action}`);
      reload().catch(console.error);
    } else if (result === "failed") {
      message.error(`命令失败：${action}`);
    } else {
      message.warning(`命令超时：${action}`);
    }
  };

  const togglePower = () => {
    if (!canControl) {
      message.warning("当前角色无控制权限");
      return;
    }
    const next: PowerState = powerState === "on" ? "off" : "on";
    runCommand(next === "on" ? "电源开启" : "电源关闭", { socket: 1, action: next }, () => setPowerState(next)).catch(console.error);
  };

  const toggleMode = (next: Exclude<ModeKey, null>) => {
    if (!canControl) {
      message.warning("当前角色无控制权限");
      return;
    }
    const target: ModeKey = mode === next ? null : next;
    runCommand(
      target ? `模式 ${target}` : "模式取消",
      { action: "mode", mode: target ?? "none" },
      () => setMode(target),
    ).catch(console.error);
  };

  const toggleTimer = (next: Exclude<TimerKey, null>) => {
    if (!canControl) {
      message.warning("当前角色无控制权限");
      return;
    }
    const target: TimerKey = timerPreset === next ? null : next;
    runCommand(
      target ? `定时 ${target}` : "取消定时",
      target ? { action: "timer_off", duration: target } : { action: "timer_off", duration: "cancel" },
      () => setTimerPreset(target),
    ).catch(console.error);
  };

  const powerTag = useMemo(() => {
    if (powerState === "on") return <Tag color="green">已开启</Tag>;
    if (powerState === "off") return <Tag color="red">已关闭</Tag>;
    return <Tag>未设置</Tag>;
  }, [powerState]);

  const modeText =
    mode === "learn" ? "学习模式" :
    mode === "eco" ? "节能模式" :
    mode === "away" ? "离家模式" : "未设置";

  const cmdAlert = dispatcher.last ? (
    <Alert
      type={dispatcher.last.state === "success" ? "success" : dispatcher.last.state === "failed" ? "error" : dispatcher.last.state === "timeout" ? "warning" : "info"}
      showIcon
      title={`最近命令：${dispatcher.last.action}`}
      description={`${new Date(dispatcher.last.at).toLocaleTimeString()} · ${dispatcher.last.state}${dispatcher.last.durationMs ? ` · ${dispatcher.last.durationMs}ms` : ""}`}
    />
  ) : null;

  const cmdMarkers = useMemo<CmdMarker[]>(() => {
    return dispatcher.records
      .filter((r) => r.state === "success" || r.state === "failed" || r.state === "timeout" || r.state === "pending")
      .slice(0, 8)
      .map((r) => ({
        ts: Math.floor(r.at / 1000),
        label: r.action,
        status: r.state === "success" || r.state === "failed" || r.state === "timeout" || r.state === "pending" ? r.state : "pending",
      }));
  }, [dispatcher.records]);

  const reload = async () => {
    const target = device || devices[0]?.id;
    if (!target) {
      setStatus(null);
      setPoints([]);
      setLastUpdated(Date.now());
      return;
    }
    const [s, t] = await Promise.all([
      fetchJSON<StripStatus>(`/api/devices/${target}/status`),
      fetchJSON<TelemetryPoint[]>(`/api/telemetry?device=${target}&range=${range}`),
    ]);
    setStatus(s);
    setPoints(t);
    setLastUpdated(Date.now());
  };

  return (
    <AppLayout title="实时监控 Live">
      <PageToolbar
        device={device}
        onDeviceChange={setDevice}
        deviceOptions={devices.map((d) => ({ value: d.id, label: `${d.room} / ${d.name}` }))}
        timeRange={range}
        onTimeRangeChange={(v) => setRange(v as RangeKey)}
        timeRangeOptions={["60s", "24h", "7d"]}
        lastUpdated={lastUpdated}
        onRefresh={() => reload().catch(console.error)}
      />

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} sm={12} lg={8}>
          <Card className="glass-card"><Statistic title="Power" value={status ? status.total_power_w.toFixed(1) : "--"} suffix="W" /></Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card className="glass-card"><Statistic title="Voltage" value={status ? status.voltage_v.toFixed(1) : "--"} suffix="V" /></Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card className="glass-card"><Statistic title="Current" value={status ? status.current_a.toFixed(2) : "--"} suffix="A" /></Card>
        </Col>
      </Row>

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} lg={16}>
          <PowerLineChart
            title="功率变化"
            points={points}
            threshold={150}
            showStats
            range={range}
            cmdMarkers={cmdMarkers}
            highlightAlerts
          />
        </Col>
        <Col xs={24} lg={8}>
          <Card className="glass-card" title="快捷控制" styles={{ body: { padding: 16 } }}>
            {!canControl ? (
              <Alert
                style={{ marginBottom: 10 }}
                type="warning"
                showIcon
                title="当前账号仅支持管理员能力"
              />
            ) : null}
            {cmdAlert}

            <div style={{ marginTop: cmdAlert ? 12 : 0 }}>
              <Descriptions column={1} size="small" styles={{ label: { width: 90 } }}>
                <Descriptions.Item label="电源状态">{powerTag}</Descriptions.Item>
                <Descriptions.Item label="定时关断">{timerPreset ? <Tag color="gold">{timerPreset}</Tag> : <Tag>未设置</Tag>}</Descriptions.Item>
                <Descriptions.Item label="运行模式"><Tag color="blue">{modeText}</Tag></Descriptions.Item>
              </Descriptions>
            </div>

            <Divider style={{ margin: "12px 0" }}>电源</Divider>
            <Button
              block
              size="large"
              type={powerState === "on" ? "primary" : "default"}
              danger={powerState === "off"}
              loading={dispatcher.isBusy}
              disabled={!canControl}
              onClick={togglePower}
            >
              {powerState === "on" ? "关闭电源" : "开启电源"}
            </Button>

            <Divider style={{ margin: "12px 0" }}>定时关断</Divider>
            <Flex gap={8} wrap>
              {(["10m", "30m", "1h"] as const).map((x) => (
                <Button
                  key={x}
                  type={timerPreset === x ? "primary" : "default"}
                  loading={dispatcher.isBusy}
                  disabled={!canControl}
                  onClick={() => toggleTimer(x)}
                >
                  {x}
                </Button>
              ))}
            </Flex>

            <Divider style={{ margin: "12px 0" }}>模式</Divider>
            <Flex gap={8} wrap>
              <Button type={mode === "learn" ? "primary" : "default"} loading={dispatcher.isBusy} disabled={!canControl} onClick={() => toggleMode("learn")}>学习模式</Button>
              <Button type={mode === "eco" ? "primary" : "default"} loading={dispatcher.isBusy} disabled={!canControl} onClick={() => toggleMode("eco")}>节能模式</Button>
              <Button type={mode === "away" ? "primary" : "default"} loading={dispatcher.isBusy} disabled={!canControl} onClick={() => toggleMode("away")}>离家模式</Button>
            </Flex>

            <div style={{ marginTop: 12 }}>
              <Tag>最近操作</Tag>
              <Timeline
                style={{ marginTop: 8 }}
                items={dispatcher.records.map((log) => ({
                  color: log.state === "success" ? "green" : log.state === "failed" ? "red" : log.state === "timeout" ? "orange" : "blue",
                  content: `${new Date(log.at).toLocaleTimeString()} · ${log.action} · ${log.state}${log.durationMs ? ` (${log.durationMs}ms)` : ""}`,
                }))}
              />
            </div>
          </Card>
        </Col>
      </Row>
    </AppLayout>
  );
}
