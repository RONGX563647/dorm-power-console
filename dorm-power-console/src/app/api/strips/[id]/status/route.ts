import { NextResponse } from "next/server";

export { GET } from "@/app/api/devices/[id]/status/route";

export async function POST() {
  return NextResponse.json({ ok: false, message: "Use /api/strips/{id}/cmd for commands." }, { status: 405 });
}
