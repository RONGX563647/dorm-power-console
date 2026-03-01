import { NextResponse } from "next/server";
import { authenticateUser } from "@/lib/userDb";

export async function POST(req: Request) {
  try {
    const body = await req.json().catch(() => ({}));
    const { account, password } = body;

    if (!account || typeof account !== "string") {
      return NextResponse.json(
        { ok: false, message: "Account is required" },
        { status: 400 }
      );
    }

    if (!password || typeof password !== "string") {
      return NextResponse.json(
        { ok: false, message: "Password is required" },
        { status: 400 }
      );
    }

    const result = authenticateUser(account, password);

    if (!result.success) {
      return NextResponse.json(
        { ok: false, message: result.error || "Authentication failed" },
        { status: 401 }
      );
    }

    return NextResponse.json({
      ok: true,
      token: result.token,
      user: result.user,
    });
  } catch (error) {
    return NextResponse.json(
      { ok: false, message: error instanceof Error ? error.message : "Server error" },
      { status: 500 }
    );
  }
}
