export type SocketStatus = {
  id: number;
  on: boolean;
  power_w: number;
  device: string;
  risk?: number;
};

export type Device = {
  id: string;
  name: string;
  room: string;
  online: boolean;
  lastSeen: string;
};

export type StripStatus = {
  ts: number;
  online: boolean;
  total_power_w: number;
  voltage_v: number;
  current_a: number;
  sockets: SocketStatus[];
};

export type TelemetryPoint = {
  ts: number;
  power_w: number;
};
