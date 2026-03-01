"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
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

// 拆分组件：状态显示组件
const StatusDisplay = React.memo(({
  powerState,
  timerPreset,
  modeText,
}: {
  powerState: PowerState;
  timerPreset: TimerKey;
  modeText: string;
}) => {
  const powerTag = useMemo(() => {
    if (powerState === "on") return (
      <Tag 
        style={{
          borderRadius: 999,
          background: "rgba(0, 230, 118, 0.15)",
          border: "1px solid rgba(0, 230, 118, 0.4)",
          color: "#00e676",
        }}
      >
        已开启
      </Tag>
    );
    if (powerState === "off") return (
      <Tag 
        style={{
          borderRadius: 999,
          background: "rgba(255, 71, 87, 0.15)",
          border: "1px solid rgba(255, 71, 87, 0.4)",
          color: "#ff4757",
        }}
      >
        已关闭
      </Tag>
    );
    return (
      <Tag 
        style={{
          borderRadius: 999,
          background: "rgba(139, 163, 199, 0.15)",
          border: "1px solid rgba(139, 163, 199, 0.4)",
          color: "#8ba3c7",
        }}
      >
        未设置
      </Tag>
    );
  }, [powerState]);

  return (
    <Descriptions 
      column={1} 
      size="small" 
      styles={{ 
        label: { width: 90, color: "#8ba3c7" },
        content: { color: "#e8f4ff" }
      }}
    >
      <Descriptions.Item label="电源状态">{powerTag}</Descriptions.Item>
      <Descriptions.Item label="定时关断">
        {timerPreset ? (
          <Tag 
            style={{
              borderRadius: 999,
              background: "rgba(255, 184, 0, 0.15)",
              border: "1px solid rgba(255, 184, 0, 0.4)",
              color: "#ffb800",
            }}
          >
            {timerPreset}
          </Tag>
        ) : (
          <Tag 
            style={{
              borderRadius: 999,
              background: "rgba(139, 163, 199, 0.15)",
              border: "1px solid rgba(139, 163, 199, 0.4)",
              color: "#8ba3c7",
            }}
          >
            未设置
          </Tag>
        )}
      </Descriptions.Item>
      <Descriptions.Item label="运行模式">
        <Tag 
          style={{
            borderRadius: 999,
            background: "rgba(0, 212, 255, 0.15)",
            border: "1px solid rgba(0, 212, 255, 0.4)",
            color: "#00d4ff",
          }}
        >
          {modeText}
        </Tag>
      </Descriptions.Item>
    </Descriptions>
  );
});

// 拆分组件：命令历史组件
const CommandHistory = React.memo(({
  records,
}: {
  records: Array<{
    at: number;
    action: string;
    state: string;
    durationMs?: number;
  }>;
}) => {
  return (
    <div style={{ marginTop: 12 }}>
      <Tag 
        style={{
          borderRadius: 999,
          background: "rgba(0, 212, 255, 0.15)",
          border: "1px solid rgba(0, 212, 255, 0.4)",
          color: "#00d4ff",
        }}
      >
        最近操作
      </Tag>
      <Timeline
        style={{ marginTop: 8 }}
        items={records.map((log) => ({
          color: log.state === "success" ? "#00e676" : log.state === "failed" ? "#ff4757" : log.state === "timeout" ? "#ffb800" : "#00d4ff",
          content: <span style={{ color: "#e8f4ff" }}>{new Date(log.at).toLocaleTimeString()} · {log.action} · {log.state}{log.durationMs ? ` (${log.durationMs}ms)` : ""}</span>,
        }))}
      />
    </div>
  );
});

// 拆分组件：控制面板组件
const ControlPanel = React.memo(({
  canControl,
  dispatcher,
  powerState,
  timerPreset,
  mode,
  onTogglePower,
  onToggleTimer,
  onToggleMode,
  cmdAlert,
}: {
  canControl: boolean;
  dispatcher: any;
  powerState: PowerState;
  timerPreset: TimerKey;
  mode: ModeKey;
  onTogglePower: () => void;
  onToggleTimer: (timer: Exclude<TimerKey, null>) => void;
  onToggleMode: (mode: Exclude<ModeKey, null>) => void;
  cmdAlert: React.ReactNode;
}) => {
  return (
    <Card className="glass-card" title={<span style={{ color: "#e8f4ff" }}>快捷控制</span>} styles={{ body: { padding: 16 } }}>
      {!canControl ? (
        <Alert
          style={{ marginBottom: 10, background: "rgba(16, 24, 40, 0.85)", border: "1px solid rgba(0, 212, 255, 0.1)" }}
          type="warning"
          showIcon
          title="当前账号仅支持管理员能力"
        />
      ) : null}
      {cmdAlert}

      <div style={{ marginTop: cmdAlert ? 12 : 0 }}>
        <StatusDisplay 
          powerState={powerState} 
          timerPreset={timerPreset} 
          modeText={mode === "learn" ? "学习模式" : mode === "eco" ? "节能模式" : mode === "away" ? "离家模式" : "未设置"} 
        />
      </div>

      <Divider style={{ margin: "12px 0", borderColor: "rgba(0, 212, 255, 0.1)" }}>
        <span style={{ color: "#8ba3c7" }}>电源</span>
      </Divider>
      <Button
        block
        size="large"
        type={powerState === "on" ? "primary" : "default"}
        danger={powerState === "off"}
        loading={dispatcher.isBusy}
        disabled={!canControl}
        onClick={onTogglePower}
        style={{
          background: powerState === "on" ? "rgba(0, 212, 255, 0.15)" : "rgba(255, 71, 87, 0.15)",
          border: powerState === "on" ? "1px solid rgba(0, 212, 255, 0.4)" : "1px solid rgba(255, 71, 87, 0.4)",
          color: powerState === "on" ? "#00d4ff" : "#ff4757",
        }}
      >
        {powerState === "on" ? "关闭电源" : "开启电源"}
      </Button>

      <Divider style={{ margin: "12px 0", borderColor: "rgba(0, 212, 255, 0.1)" }}>
        <span style={{ color: "#8ba3c7" }}>定时关断</span>
      </Divider>
      <Flex gap={8} wrap>
        {(["10m", "30m", "1h"] as const).map((x) => (
          <Button
            key={x}
            type={timerPreset === x ? "primary" : "default"}
            loading={dispatcher.isBusy}
            disabled={!canControl}
            onClick={() => onToggleTimer(x)}
            style={{
              background: timerPreset === x ? "rgba(255, 184, 0, 0.15)" : "rgba(0, 212, 255, 0.1)",
              border: timerPreset === x ? "1px solid rgba(255, 184, 0, 0.4)" : "1px solid rgba(0, 212, 255, 0.3)",
              color: timerPreset === x ? "#ffb800" : "#00d4ff",
            }}
          >
            {x}
          </Button>
        ))}
      </Flex>

      <Divider style={{ margin: "12px 0", borderColor: "rgba(0, 212, 255, 0.1)" }}>
        <span style={{ color: "#8ba3c7" }}>模式</span>
      </Divider>
      <Flex gap={8} wrap>
        <Button 
          type={mode === "learn" ? "primary" : "default"} 
          loading={dispatcher.isBusy} 
          disabled={!canControl} 
          onClick={() => onToggleMode("learn")}
          style={{
            background: mode === "learn" ? "rgba(0, 212, 255, 0.15)" : "rgba(0, 212, 255, 0.1)",
            border: mode === "learn" ? "1px solid rgba(0, 212, 255, 0.4)" : "1px solid rgba(0, 212, 255, 0.3)",
            color: mode === "learn" ? "#00d4ff" : "#00d4ff",
          }}
        >
          学习模式
        </Button>
        <Button 
          type={mode === "eco" ? "primary" : "default"} 
          loading={dispatcher.isBusy} 
          disabled={!canControl} 
          onClick={() => onToggleMode("eco")}
          style={{
            background: mode === "eco" ? "rgba(0, 230, 118, 0.15)" : "rgba(0, 212, 255, 0.1)",
            border: mode === "eco" ? "1px solid rgba(0, 230, 118, 0.4)" : "1px solid rgba(0, 212, 255, 0.3)",
            color: mode === "eco" ? "#00e676" : "#00d4ff",
          }}
        >
          节能模式
        </Button>
        <Button 
          type={mode === "away" ? "primary" : "default"} 
          loading={dispatcher.isBusy} 
          disabled={!canControl} 
          onClick={() => onToggleMode("away")}
          style={{
            background: mode === "away" ? "rgba(255, 184, 0, 0.15)" : "rgba(0, 212, 255, 0.1)",
            border: mode === "away" ? "1px solid rgba(255, 184, 0, 0.4)" : "1px solid rgba(0, 212, 255, 0.3)",
            color: mode === "away" ? "#ffb800" : "#00d4ff",
          }}
        >
          离家模式
        </Button>
      </Flex>

      <CommandHistory records={dispatcher.records} />
    </Card>
  );
});

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
      const devs = await fetchJSON<Device[]>('/api/devices');
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

  const executeBackendCommand = useCallback(async (payload: Record<string, unknown>): Promise<CmdResult> => {
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
  }, [device]);

  const runCommand = useCallback(async (
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
  }, [dispatcher, executeBackendCommand]);

  const reload = useCallback(async () => {
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
  }, [device, devices, range]);

  const togglePower = useCallback(() => {
    if (!canControl) {
      message.warning("当前角色无控制权限");
      return;
    }
    const next: PowerState = powerState === "on" ? "off" : "on";
    runCommand(next === "on" ? "电源开启" : "电源关闭", { socket: 1, action: next }, () => setPowerState(next)).catch(console.error);
  }, [canControl, powerState, runCommand]);

  const toggleMode = useCallback((next: Exclude<ModeKey, null>) => {
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
  }, [canControl, mode, runCommand]);

  const toggleTimer = useCallback((next: Exclude<TimerKey, null>) => {
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
  }, [canControl, timerPreset, runCommand]);

  const cmdAlert = useMemo(() => {
    if (!dispatcher.last) return null;
    return (
      <Alert
        type={dispatcher.last.state === "success" ? "success" : dispatcher.last.state === "failed" ? "error" : dispatcher.last.state === "timeout" ? "warning" : "info"
        showIcon
        title={`最近命令：${dispatcher.last.action}`}
        description={`${new Date(dispatcher.last.at).toLocaleTimeString()} · ${dispatcher.last.state}${dispatcher.last.durationMs ? ` · ${dispatcher.last.durationMs}ms` : ""}`}
        style={{
          background: "rgba(16, 24, 40, 0.85)",
          border: "1px solid rgba(0, 212, 255, 0.1)",
        }}
      />
    );
  }, [dispatcher.last]);

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

  const deviceOptions = useMemo(() => {
    return devices.map((d) => ({ value: d.id, label: `${d.room} / ${d.name}` }));
  }, [devices]);

  return (
    <AppLayout title="实时监控 Live">
      <PageToolbar
        device={device}
        onDeviceChange={setDevice}
        deviceOptions={deviceOptions}
        timeRange={range}
        onTimeRangeChange={(v) => setRange(v as RangeKey)}
        timeRangeOptions={["60s", "24h", "7d"]}
        lastUpdated={lastUpdated}
        onRefresh={reload}
      />

      <Row gutter={[12, 12]} style={{ marginTop: 12 }}>
        <Col xs={24} sm={12} lg={8}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>Power</span>}
              value={status ? status.total_power_w.toFixed(1) : "--"} 
              suffix="W"
              valueStyle={{ color: "#00d4ff", fontWeight: 700, fontSize: 24 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>Voltage</span>}
              value={status ? status.voltage_v.toFixed(1) : "--"} 
              suffix="V"
              valueStyle={{ color: "#00d4ff", fontWeight: 700, fontSize: 24 }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card className="glass-card">
            <Statistic 
              title={<span style={{ color: "#8ba3c7", fontSize: 12, textTransform: "uppercase", letterSpacing: 1 }}>Current</span>}
              value={status ? status.current_a.toFixed(2) : "--"} 
              suffix="A"
              valueStyle={{ color: "#00d4ff", fontWeight: 700, fontSize: 24 }}
            />
          </Card>
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
          <ControlPanel
            canControl={canControl}
            dispatcher={dispatcher}
            powerState={powerState}
            timerPreset={timerPreset}
            mode={mode}
            onTogglePower={togglePower}
            onToggleTimer={toggleTimer}
            onToggleMode={toggleMode}
            cmdAlert={cmdAlert}
          />
        </Col>
      </Row>
    </AppLayout>
  );
}
