import { useCallback, useMemo, useState } from "react";

export type CmdState = "idle" | "sending" | "pending" | "success" | "failed" | "timeout" | "cancelled";

export type CmdRecord = {
  id: string;
  at: number;
  action: string;
  state: CmdState;
  durationMs?: number;
};

type RunCommandOptions = {
  simulate?: "success" | "failed" | "timeout";
  executor?: () => Promise<"success" | "failed" | "timeout">;
};

export function useCmdDispatcher(timeoutMs = 4500) {
  const [records, setRecords] = useState<CmdRecord[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);

  const last = records[0] ?? null;

  const runCommand = useCallback(async (
    action: string,
    onSuccess: () => void,
    options?: RunCommandOptions,
  ) => {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2, 7)}`;
    const startedAt = Date.now();
    setActiveId(id);

    const push = (record: CmdRecord) => {
      setRecords((prev) => [record, ...prev.filter((x) => x.id !== id)].slice(0, 12));
    };

    push({ id, at: startedAt, action, state: "sending" });
    await new Promise((r) => setTimeout(r, 180));
    push({ id, at: Date.now(), action, state: "pending" });

    let result: "success" | "failed" | "timeout";
    if (options?.executor) {
      result = await options.executor();
    } else {
      const mode = options?.simulate ?? "success";
      result = await new Promise<"success" | "failed" | "timeout">((resolve) => {
        const timeoutTimer = setTimeout(() => resolve("timeout"), timeoutMs);

        if (mode === "timeout") {
          return;
        }

        const ackDelay = 700;
        setTimeout(() => {
          clearTimeout(timeoutTimer);
          resolve(mode);
        }, ackDelay);
      });
    }

    const endedAt = Date.now();
    if (result === "success") {
      onSuccess();
      push({ id, at: endedAt, action, state: "success", durationMs: endedAt - startedAt });
    } else if (result === "failed") {
      push({ id, at: endedAt, action, state: "failed", durationMs: endedAt - startedAt });
    } else {
      push({ id, at: endedAt, action, state: "timeout", durationMs: endedAt - startedAt });
    }

    setActiveId(null);
    return result;
  }, [timeoutMs]);

  return useMemo(() => ({
    records,
    last,
    activeId,
    isBusy: Boolean(activeId),
    runCommand,
  }), [records, last, activeId, runCommand]);
}
