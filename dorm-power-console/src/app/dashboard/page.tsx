"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Button, Card, Col, Progress, Row, Space, Statistic, Switch, Tag, Typography, message } from "antd";
import {
  ThunderboltOutlined,
  DashboardOutlined,
  CloudServerOutlined,
  AlertOutlined,
  FireOutlined,
  ClockCircleOutlined,
  ApartmentOutlined,
  SafetyOutlined,
  WarningFilled,
  CheckCircleFilled,
} from "@ant-design/icons";
import AppLayout from "@/components/AppLayout";
import KPIGrid from "@/components/KPIGrid";
import SocketMatrix from "@/components/SocketMatrix";
import type { Device, StripStatus, TelemetryPoint } from "@/components/types";
import { fetchJSON } from "@/lib/fetcher";
import DashboardFilterBar, { type TimeRange } from "@/components/dashboard/DashboardFilterBar";
import EventFeed from "@/components/dashboard/EventFeed";
import TopConsumers from "@/components/dashboard/TopConsumers";
import { useRole } from "@/components/RoleProvider";

const { Text } = Typography;

type DashboardEvent = {
  id: string;
  type: "REPORT" | "CMD" | "ALERT" | "SYSTEM";
  time: string;
  detail: string;
  status: "ok" | "warn" | "fail";
};

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

export default function DashboardPage() {
  const { canControl, canGlobalPolicy } = useRole();
  const [device, setDevice] = useState("strip01");
  const [timeRange, setTimeRange] = useState<TimeRange>("60s");
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [devices, setDevices] = useState<Device[]>([]);
  const [status, setStatus] = useState<StripStatus | null>(null);
  const [points, setPoints] = useState<TelemetryPoint[]>([]);
  const [statusMap, setStatusMap] = useState<Record<string, StripStatus | null>>({});
  const [lastUpdated, setLastUpdated] = useState<number>();
  const [simEvents, setSimEvents] = useState<DashboardEvent[]>([]);
  const [autoCutoff, setAutoCutoff] = useState(true);
  const [illegalDetect, setIllegalDetect] = useState(true);
  const [rules, setRules] = useState([
    { id: "r1", name: "工作日 23:30 熄灯策略", enabled: true, scope: "A 栋全体宿舍" },
    { id: "r2", name: "待机功率 < 5W 自动断电", enabled: true, scope: "非路由插孔" },
    { id: "r3", name: "违规负载瞬时切断", enabled: false, scope: "高危设备指纹" },
  ]);

  const load = useCallback(async () => {
    const [devs, selectedStatus, selectedTelemetry] = await Promise.all([
      fetchJSON<Device[]>("/api/devices"),
      fetchJSON<StripStatus>(`/api/devices/${device}/status`),
      fetchJSON<TelemetryPoint[]>(`/api/telemetry?device=${device}&range=${timeRange}`),
    ]);

    const allStatuses = await Promise.all(
      devs.map(async (d) => {
        try {
          const s = await fetchJSON<StripStatus>(`/api/devices/${d.id}/status`);
          return [d.id, s] as const;
        } catch {
          return [d.id, null] as const;
        }
      }),
    );

    setDevices(devs);
    setStatus(selectedStatus);
    setPoints(selectedTelemetry);
    setStatusMap(Object.fromEntries(allStatuses));
    setLastUpdated(Date.now());
  }, [device, timeRange]);

  useEffect(() => {
    load().catch(console.error);
  }, [load]);

  useEffect(() => {
    if (!autoRefresh) return;
    const interval = timeRange === "60s" ? 2000 : 5000;
    const timer = setInterval(() => {
      load().catch(console.error);
    }, interval);
    return () => clearInterval(timer);
  }, [autoRefresh, timeRange, load]);

  const selectedDevice = useMemo(() => devices.find((d) => d.id === device) ?? null, [devices, device]);
  const currentRoom = selectedDevice?.room ?? "A-302";
  const roomDevices = useMemo(() => devices.filter((d) => d.room === currentRoom), [devices, currentRoom]);

  const roomPower = useMemo(
    () => roomDevices.reduce((sum, d) => sum + (statusMap[d.id]?.total_power_w ?? 0), 0),
    [roomDevices, statusMap],
  );
  const roomOnline = useMemo(() => roomDevices.filter((d) => d.online).length, [roomDevices]);

  const pointStats = useMemo(() => {
    const values = points.map((p) => p.power_w);
    const avg = values.length ? values.reduce((a, b) => a + b, 0) / values.length : 0;
    return { avg };
  }, [points]);

  const baseEvents = useMemo<DashboardEvent[]>(() => {
    if (!status) return [];
    const highSocket = status.sockets.find((s) => s.power_w >= 100);
    const offline = roomDevices.filter((d) => !d.online);
    return [
      {
        id: "e1",
        type: "REPORT",
        time: new Date(status.ts * 1000).toLocaleTimeString(),
        detail: `${device} 上报功率 ${status.total_power_w.toFixed(1)}W`,
        status: "ok",
      },
      {
        id: "e2",
        type: highSocket ? "ALERT" : "SYSTEM",
        time: new Date().toLocaleTimeString(),
        detail: highSocket ? `插孔 ${highSocket.id} 功率超过阈值` : "系统运行正常",
        status: highSocket ? "warn" : "ok",
      },
      {
        id: "e3",
        type: offline.length ? "SYSTEM" : "CMD",
        time: new Date().toLocaleTimeString(),
        detail: offline.length ? `${currentRoom} 离线设备 ${offline.length} 台` : `${device} 指令通道稳定`,
        status: offline.length ? "fail" : "ok",
      },
    ];
  }, [currentRoom, device, roomDevices, status]);

  const eventItems = useMemo(() => [...simEvents, ...baseEvents].slice(0, 10), [simEvents, baseEvents]);

  const topConsumers = useMemo(() => {
    const list = roomDevices.map((d) => {
      const power = statusMap[d.id]?.total_power_w ?? 0;
      return {
        name: `${d.room} / ${d.name}`,
        power,
      };
    });
    const total = list.reduce((sum, x) => sum + x.power, 0) || 1;
    return list
      .map((x) => ({
        name: x.name,
        value: Number((x.power * 0.021).toFixed(2)),
        percent: Math.round((x.power / total) * 100),
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 5);
  }, [roomDevices, statusMap]);

  const roomLoadDistribution = useMemo(() => {
    const total = roomPower || 1;
    return roomDevices.map((d) => {
      const p = statusMap[d.id]?.total_power_w ?? 0;
      return {
        id: d.id,
        label: `${d.name}`,
        power: p,
        percent: Math.round((p / total) * 100),
      };
    }).sort((a, b) => b.power - a.power);
  }, [roomDevices, roomPower, statusMap]);

  const addSimEvent = (type: DashboardEvent["type"], detail: string, state: DashboardEvent["status"]) => {
    setSimEvents((prev) => [
      {
        id: `sim-${Date.now()}`,
        type,
        detail,
        status: state,
        time: new Date().toLocaleTimeString(),
      },
      ...prev,
    ].slice(0, 6));
  };

  const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

  const executeBackendCommand = async (payload: Record<string, unknown>): Promise<"success" | "failed" | "timeout"> => {
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

  const postPolicyCommand = async (payload: Record<string, unknown>) => {
    return executeBackendCommand({ action: "policy", payload });
  };

  const toggleRule = async (ruleId: string, enabled: boolean) => {
    if (!canGlobalPolicy) {
      message.warning("仅管理员可修改策略规则");
      return;
    }
    const prevRules = rules;
    setRules((prev) => prev.map((r) => (r.id === ruleId ? { ...r, enabled } : r)));
    try {
      const result = await postPolicyCommand({
        kind: "rule",
        ruleId,
        enabled,
      });
      if (result !== "success") {
        setRules(prevRules);
        if (result === "timeout") {
          message.warning("策略执行超时，未收到设备确认");
        } else {
          message.error(`策略${enabled ? "启用" : "停用"}失败`);
        }
        return;
      }
      message.success(`策略已${enabled ? "启用" : "停用"}`);
    } catch {
      setRules(prevRules);
      message.error("策略下发失败");
    }
  };

  const sendSocketCommand = async (socketId: number, nextOn: boolean) => {
    if (!canControl) {
      message.warning("当前角色无控制权限");
      return;
    }
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
      setStatusMap((prev) => ({
        ...prev,
        [device]: applySocketState(prev[device] ?? null, socketId, nextOn),
      }));
      message.success(`插孔${socketId} 已${nextOn ? "开启" : "关闭"}`);
      load().catch(console.error);
    } catch {
      message.error("命令下发失败");
    }
  };

  const alertCount = (status?.sockets.filter((s) => s.power_w >= 100).length ?? 0) + simEvents.filter((e) => e.type === "ALERT").length;
  const floorMap = useMemo(() => {
    const rooms = Array.from(new Set(devices.map((d) => d.room)));
    return rooms.map((room) => {
      const roomDevs = devices.filter((d) => d.room === room);
      const roomOffline = roomDevs.filter((d) => !d.online).length;
      const roomPower = roomDevs.reduce((sum, d) => sum + (statusMap[d.id]?.total_power_w ?? 0), 0);
      const roomRisk = roomDevs.some((d) => (statusMap[d.id]?.total_power_w ?? 0) >= 180);
      return { room, roomOffline, roomPower, roomRisk, total: roomDevs.length };
    });
  }, [devices, statusMap]);

  return (
    <AppLayout title="总览 Dashboard">
      <DashboardFilterBar
        device={device}
        onDeviceChange={setDevice}
        deviceOptions={devices.map((d) => ({ value: d.id, label: `${d.room} / ${d.name}` }))}
        timeRange={timeRange}
        onTimeRangeChange={setTimeRange}
        autoRefresh={autoRefresh}
        onAutoRefreshChange={setAutoRefresh}
        lastUpdated={lastUpdated}
        onRefresh={() => load().catch(console.error)}
      />

      <Row gutter={[16, 16]} style={{ marginTop: 12 }}>
        <Col xs={24} lg={16}>
          <Space direction="vertical" size="middle" style={{ width: "100%" }}>
            <Card className="glass-card" styles={{ body: { padding: 16 } }}>
              <Space direction="vertical" size="small" style={{ width: "100%" }}>
                <Space wrap style={{ justifyContent: "space-between", width: "100%" }}>
                  <Space>
                    <ApartmentOutlined style={{ color: "#00d4ff", fontSize: 18 }} />
                    <Text strong style={{ fontSize: 16, color: "#e8f4ff" }}>{currentRoom} 宿舍聚合视图</Text>
                  </Space>
                  <Tag style={{
                    borderRadius: 999,
                    background: alertCount ? "rgba(255, 71, 87, 0.15)" : "rgba(0, 230, 118, 0.15)",
                    border: `1px solid ${alertCount ? "rgba(255, 71, 87, 0.4)" : "rgba(0, 230, 118, 0.4)"}`,
                    color: alertCount ? "#ff4757" : "#00e676",
                  }}>
                    {alertCount ? `${alertCount} 条异常` : "运行稳定"}
                  </Tag>
                </Space>
                <Alert
                  type={alertCount ? "warning" : "success"}
                  showIcon
                  icon={alertCount ? <WarningFilled style={{ color: "#ffb800" }} /> : <CheckCircleFilled style={{ color: "#00e676" }} />}
                  message={alertCount ? "检测到高功率或离线风险，请优先处理告警" : "暂无高风险异常，系统处于稳定状态"}
                  style={{
                    background: alertCount ? "rgba(255, 184, 0, 0.1)" : "rgba(0, 230, 118, 0.1)",
                    border: `1px solid ${alertCount ? "rgba(255, 184, 0, 0.2)" : "rgba(0, 230, 118, 0.2)"}`,
                  }}
                />
                <Row gutter={12} style={{ marginTop: 8 }}>
                  <Col xs={24} sm={8}>
                    <Statistic 
                      title={<span style={{ color: "#8ba3c7" }}>宿舍总功率</span>} 
                      value={roomPower.toFixed(1)} 
                      suffix="W"
                      valueStyle={{ color: "#00d4ff", fontWeight: 700 }}
                    />
                  </Col>
                  <Col xs={24} sm={8}>
                    <Statistic 
                      title={<span style={{ color: "#8ba3c7" }}>在线设备</span>} 
                      value={`${roomOnline}/${roomDevices.length || 1}`}
                      valueStyle={{ color: "#e8f4ff", fontWeight: 700 }}
                    />
                  </Col>
                  <Col xs={24} sm={8}>
                    <Statistic 
                      title={<span style={{ color: "#8ba3c7" }}>当前设备</span>} 
                      value={device}
                      valueStyle={{ color: "#e8f4ff", fontWeight: 700 }}
                    />
                  </Col>
                </Row>
              </Space>
            </Card>

            <Card 
              className="glass-card" 
              title={<span style={{ color: "#e8f4ff" }}>宿舍负载分布</span>} 
              styles={{ body: { padding: 16 } }}
            >
              <Space direction="vertical" style={{ width: "100%" }}>
                {roomLoadDistribution.length === 0 ? (
                  <Text style={{ color: "#8ba3c7" }}>暂无设备负载数据</Text>
                ) : (
                  roomLoadDistribution.map((item) => (
                    <div key={item.id}>
                      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                        <Text style={{ color: "#e8f4ff" }}>{item.label}</Text>
                        <Text strong style={{ color: "#00d4ff" }}>{item.power.toFixed(1)}W</Text>
                      </div>
                      <Progress 
                        percent={item.percent} 
                        showInfo={false} 
                        strokeColor={{ from: "#00d4ff", to: "#0099ff" }}
                        trailColor="rgba(0, 212, 255, 0.1)"
                      />
                    </div>
                  ))
                )}
              </Space>
            </Card>
          </Space>
        </Col>
        <Col xs={24} lg={8}>
          <Card className="glass-card" title={<Space><SafetyOutlined style={{ color: "#00d4ff" }} /><span style={{ color: "#e8f4ff" }}>安全与策略</span></Space>} styles={{ body: { padding: 12 } }}>
            {!canControl ? (
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 10, background: "rgba(255, 184, 0, 0.1)", border: "1px solid rgba(255, 184, 0, 0.2)" }}
                message="当前账号仅支持管理员能力，安全策略可配置"
              />
            ) : null}
            <Space direction="vertical" style={{ width: "100%" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <Text style={{ color: "#e8f4ff" }}>违规负载检测</Text>
                <Switch
                  checked={illegalDetect}
                  disabled={!canGlobalPolicy}
                  onChange={async (checked) => {
                    if (!canGlobalPolicy) return;
                    const prev = illegalDetect;
                    setIllegalDetect(checked);
                    try {
                      const result = await postPolicyCommand({
                        kind: "illegal_detect",
                        enabled: checked,
                      });
                      if (result !== "success") {
                        setIllegalDetect(prev);
                        if (result === "timeout") {
                          message.warning("违规负载检测策略超时，未收到设备确认");
                        } else {
                          message.error("违规负载检测策略下发失败");
                        }
                        return;
                      }
                      message.success(`违规负载检测已${checked ? "启用" : "停用"}`);
                    } catch {
                      setIllegalDetect(prev);
                      message.error("策略下发失败");
                    }
                  }}
                />
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <Text style={{ color: "#e8f4ff" }}>待机自动断电</Text>
                <Switch
                  checked={autoCutoff}
                  disabled={!canGlobalPolicy}
                  onChange={async (checked) => {
                    if (!canGlobalPolicy) return;
                    const prev = autoCutoff;
                    setAutoCutoff(checked);
                    try {
                      const result = await postPolicyCommand({
                        kind: "auto_cutoff",
                        enabled: checked,
                      });
                      if (result !== "success") {
                        setAutoCutoff(prev);
                        if (result === "timeout") {
                          message.warning("待机自动断电策略超时，未收到设备确认");
                        } else {
                          message.error("待机自动断电策略下发失败");
                        }
                        return;
                      }
                      message.success(`待机自动断电已${checked ? "启用" : "停用"}`);
                    } catch {
                      setAutoCutoff(prev);
                      message.error("策略下发失败");
                    }
                  }}
                />
              </div>
              <div>
                <Text strong style={{ color: "#e8f4ff" }}>策略规则</Text>
                <Space direction="vertical" size="small" style={{ width: "100%", marginTop: 6 }}>
                  {rules.map((r) => (
                    <div key={r.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 8px", borderRadius: 8, border: "1px solid rgba(0, 212, 255, 0.15)", background: "rgba(0, 212, 255, 0.05)" }}>
                      <div>
                        <Text style={{ color: "#e8f4ff" }}>{r.name}</Text>
                        <br />
                        <Text style={{ fontSize: 12, color: "#8ba3c7" }}>{r.scope}</Text>
                      </div>
                      <Switch
                        size="small"
                        checked={r.enabled}
                        disabled={!canGlobalPolicy}
                        onChange={(v) => void toggleRule(r.id, v)}
                      />
                    </div>
                  ))}
                </Space>
              </div>
              <Space wrap>
                <Button 
                  onClick={() => { addSimEvent("ALERT", "模拟告警：插孔2 高功率", "warn"); message.warning("已触发模拟告警"); }}
                  style={{
                    background: "rgba(255, 184, 0, 0.1)",
                    border: "1px solid rgba(255, 184, 0, 0.3)",
                    color: "#ffb800",
                  }}
                >
                  模拟告警
                </Button>
                <Button 
                  onClick={() => { addSimEvent("SYSTEM", "模拟离线：设备临时断连", "fail"); message.error("已触发模拟离线"); }}
                  style={{
                    background: "rgba(255, 71, 87, 0.1)",
                    border: "1px solid rgba(255, 71, 87, 0.3)",
                    color: "#ff4757",
                  }}
                >
                  模拟离线
                </Button>
              </Space>
            </Space>
          </Card>
        </Col>
      </Row>

      <div style={{ marginTop: 12 }}>
        <KPIGrid
          items={[
            {
              title: "当前总功率",
              value: status ? status.total_power_w.toFixed(1) : "--",
              suffix: "W",
              delta: pointStats.avg ? `${(((status?.total_power_w ?? 0) - pointStats.avg) / pointStats.avg * 100).toFixed(1)}%` : "--",
              footnote: `对比近 ${timeRange} 均值`,
              icon: <ThunderboltOutlined />,
            },
            {
              title: "今日电量",
              value: points.length ? (points.reduce((a, p) => a + p.power_w, 0) / 3600).toFixed(2) : "--",
              suffix: "kWh",
              footnote: "演示估算",
              icon: <DashboardOutlined />,
            },
            {
              title: "在线设备",
              value: `${roomOnline}/${roomDevices.length || 1}`,
              footnote: `${currentRoom} 设备在线率`,
              icon: <CloudServerOutlined />,
            },
            {
              title: "告警数(24h)",
              value: alertCount,
              footnote: "高功率阈值告警",
              icon: <AlertOutlined />,
            },
            {
              title: "高功率插孔",
              value: status ? status.sockets.filter((s) => s.power_w >= 80).length : 0,
              footnote: "阈值 > 80W",
              icon: <FireOutlined />,
            },
            {
              title: "离线时长",
              value: status?.online ? 0 : 12,
              suffix: "min",
              footnote: "当前设备",
              icon: <ClockCircleOutlined />,
            },
          ]}
        />
      </div>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24}>
          <SocketMatrix sockets={status?.sockets ?? []} onToggle={sendSocketCommand} />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24}>
          <Card className="glass-card" title={<span style={{ color: "#e8f4ff" }}>楼层拓扑图（态势）</span>} styles={{ body: { padding: 16 } }}>
            <Row gutter={[12, 12]}>
              {floorMap.map((f) => (
                <Col xs={24} sm={12} md={8} lg={6} key={f.room}>
                  <div
                    style={{
                      borderRadius: 12,
                      padding: 12,
                      border: `1px solid ${f.roomRisk ? "rgba(255, 71, 87, 0.3)" : f.roomOffline > 0 ? "rgba(255, 184, 0, 0.3)" : "rgba(0, 230, 118, 0.3)"}`,
                      background: f.roomRisk ? "rgba(255, 71, 87, 0.1)" : f.roomOffline > 0 ? "rgba(255, 184, 0, 0.1)" : "rgba(0, 230, 118, 0.1)",
                    }}
                  >
                    <Text strong style={{ color: "#e8f4ff" }}>{f.room}</Text>
                    <div><Text style={{ color: "#8ba3c7" }}>总功率：{f.roomPower.toFixed(1)}W</Text></div>
                    <div><Text style={{ color: "#8ba3c7" }}>在线：{f.total - f.roomOffline}/{f.total}</Text></div>
                    <Tag style={{
                      marginTop: 6,
                      borderRadius: 999,
                      background: f.roomRisk ? "rgba(255, 71, 87, 0.15)" : f.roomOffline > 0 ? "rgba(255, 184, 0, 0.15)" : "rgba(0, 230, 118, 0.15)",
                      border: `1px solid ${f.roomRisk ? "rgba(255, 71, 87, 0.4)" : f.roomOffline > 0 ? "rgba(255, 184, 0, 0.4)" : "rgba(0, 230, 118, 0.4)"}`,
                      color: f.roomRisk ? "#ff4757" : f.roomOffline > 0 ? "#ffb800" : "#00e676",
                    }}>
                      {f.roomRisk ? "高危" : f.roomOffline > 0 ? "注意" : "正常"}
                    </Tag>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <EventFeed items={eventItems} />
        </Col>
        <Col xs={24} lg={8}>
          <TopConsumers items={topConsumers} />
        </Col>
      </Row>
    </AppLayout>
  );
}
