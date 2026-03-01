import { NextResponse } from "next/server";
import { createUser } from "@/lib/userDb";

export async function POST(req: Request) {
  try {
    const body = await req.json().catch(() => ({}));
    const { username, email, password } = body;

    if (!username || typeof username !== "string") {
      return NextResponse.json(
        { ok: false, message: "Username is required" },
        { status: 400 }
      );
    }

    if (!email || typeof email !== "string") {
      return NextResponse.json(
        { ok: false, message: "Email is required" },
        { status: 400 }
      );
    }

    if (!password || typeof password !== "string") {
      return NextResponse.json(
        { ok: false, message: "Password is required" },
        { status: 400 }
      );
    }

    if (username.length < 3 || username.length > 20) {
      return NextResponse.json(
        { ok: false, message: "Username must be between 3 and 20 characters" },
        { status: 400 }
      );
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return NextResponse.json(
        { ok: false, message: "Invalid email format" },
        { status: 400 }
      );
    }

    if (password.length < 6) {
      return NextResponse.json(
        { ok: false, message: "Password must be at least 6 characters" },
        { status: 400 }
      );
    }

    const result = createUser(username, email, password);

    if (!result.success) {
      return NextResponse.json(
        { ok: false, message: result.error || "Registration failed" },
        { status: 400 }
      );
    }

    return NextResponse.json({
      ok: true,
      message: "Registration successful",
      user: result.user,
    });
  } catch (error) {
    return NextResponse.json(
      { ok: false, message: error instanceof Error ? error.message : "Server error" },
      { status: 500 }
    );
  }
}
