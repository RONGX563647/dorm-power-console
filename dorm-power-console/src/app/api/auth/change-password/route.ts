import { NextResponse } from "next/server";
import { changePassword, getUserByUsername, getUserByEmail } from "@/lib/userDb";

export async function POST(req: Request) {
  try {
    const body = await req.json().catch(() => ({}));
    const { account, oldPassword, newPassword } = body;

    if (!account || typeof account !== "string") {
      return NextResponse.json(
        { ok: false, message: "Account is required" },
        { status: 400 }
      );
    }

    if (!oldPassword || typeof oldPassword !== "string") {
      return NextResponse.json(
        { ok: false, message: "Old password is required" },
        { status: 400 }
      );
    }

    if (!newPassword || typeof newPassword !== "string") {
      return NextResponse.json(
        { ok: false, message: "New password is required" },
        { status: 400 }
      );
    }

    if (newPassword.length < 6) {
      return NextResponse.json(
        { ok: false, message: "New password must be at least 6 characters" },
        { status: 400 }
      );
    }

    let user = getUserByUsername(account);
    if (!user) {
      user = getUserByEmail(account);
    }

    if (!user) {
      return NextResponse.json(
        { ok: false, message: "User not found" },
        { status: 404 }
      );
    }

    const result = changePassword(user.id, oldPassword, newPassword);

    if (!result.success) {
      return NextResponse.json(
        { ok: false, message: result.error || "Password change failed" },
        { status: 400 }
      );
    }

    return NextResponse.json({
      ok: true,
      message: "Password changed successfully",
    });
  } catch (error) {
    return NextResponse.json(
      { ok: false, message: error instanceof Error ? error.message : "Server error" },
      { status: 500 }
    );
  }
}
