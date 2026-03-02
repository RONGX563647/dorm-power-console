import { NextResponse } from "next/server";
import { proxyPost } from "../_utils";

export async function POST(req: Request) {
  try {
    const { status, data } = await proxyPost(req, "/api/auth/login");
    return NextResponse.json(data, { status });
  } catch (error) {
    return NextResponse.json(
      { ok: false, message: error instanceof Error ? error.message : "proxy error" },
      { status: 500 },
    );
  }
}

