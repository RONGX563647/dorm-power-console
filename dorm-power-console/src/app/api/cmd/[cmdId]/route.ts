import { NextResponse } from "next/server";

function getBackendBaseUrl() {
  const base = process.env.BACKEND_BASE_URL;
  if (!base) {
    throw new Error("BACKEND_BASE_URL is not configured");
  }
  return base.replace(/\/$/, "");
}

export async function GET(
  _req: Request,
  { params }: { params: Promise<{ cmdId: string }> },
) {
  try {
    const { cmdId } = await params;
    const base = getBackendBaseUrl();
    const res = await fetch(`${base}/api/cmd/${encodeURIComponent(cmdId)}`, { cache: "no-store" });
    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  } catch (error) {
    return NextResponse.json(
      {
        ok: false,
        message: error instanceof Error ? error.message : "proxy error",
      },
      { status: 500 },
    );
  }
}
