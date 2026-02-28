import { NextResponse } from "next/server";

function getBackendBaseUrl() {
  const base = process.env.BACKEND_BASE_URL;
  if (!base) {
    throw new Error("BACKEND_BASE_URL is not configured");
  }
  return base.replace(/\/$/, "");
}

export async function POST(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    const { id } = await params;
    const decodedId = decodeURIComponent(id);
    const payload = await req.json().catch(() => ({}));
    const base = getBackendBaseUrl();
    const res = await fetch(`${base}/api/strips/${encodeURIComponent(decodedId)}/cmd`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(payload),
      cache: "no-store",
    });
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
