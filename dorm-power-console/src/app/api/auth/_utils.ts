export function getBackendBaseUrl() {
  const base = process.env.BACKEND_BASE_URL;
  if (!base) {
    throw new Error("BACKEND_BASE_URL is not configured");
  }
  return base.replace(/\/$/, "");
}

export async function proxyPost(req: Request, targetPath: string) {
  const payload = await req.json().catch(() => ({}));
  const res = await fetch(`${getBackendBaseUrl()}${targetPath}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
    cache: "no-store",
  });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, data };
}

