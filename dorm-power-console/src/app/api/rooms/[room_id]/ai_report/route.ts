import { NextResponse } from "next/server";

function getBackendBaseUrl() {
  const base = process.env.BACKEND_BASE_URL;
  if (!base) {
    throw new Error("BACKEND_BASE_URL is not configured");
  }
  return base.replace(/\/$/, "");
}

export async function GET(
  req: Request,
  { params }: { params: Promise<{ room_id: string }> },
) {
  try {
    const { room_id } = await params;
    const { searchParams } = new URL(req.url);
    const base = getBackendBaseUrl();
    const qs = searchParams.toString();
    const path = `${base}/api/rooms/${encodeURIComponent(room_id)}/ai_report${qs ? `?${qs}` : ""}`;
    const res = await fetch(path, { cache: "no-store" });
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
