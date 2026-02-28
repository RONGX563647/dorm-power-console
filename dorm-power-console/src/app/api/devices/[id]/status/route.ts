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
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const { id } = await params;
    const decodedId = decodeURIComponent(id);
    const base = getBackendBaseUrl();
    const res = await fetch(`${base}/api/devices/${encodeURIComponent(decodedId)}/status`, { cache: "no-store" });
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
