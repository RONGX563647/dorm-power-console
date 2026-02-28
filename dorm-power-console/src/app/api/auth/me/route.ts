import { NextResponse } from "next/server";

function getBackendBaseUrl() {
  const base = process.env.BACKEND_BASE_URL;
  if (!base) {
    throw new Error("BACKEND_BASE_URL is not configured");
  }
  return base.replace(/\/$/, "");
}

export async function GET(req: Request) {
  try {
    const authHeader = req.headers.get("authorization");
    const base = getBackendBaseUrl();
    const res = await fetch(`${base}/api/auth/me`, {
      headers: authHeader ? { authorization: authHeader } : {},
      cache: "no-store",
    });
    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  } catch (error) {
    return NextResponse.json(
      { ok: false, message: error instanceof Error ? error.message : "proxy error" },
      { status: 500 },
    );
  }
}