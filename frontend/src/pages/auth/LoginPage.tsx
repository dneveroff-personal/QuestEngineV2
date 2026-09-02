import { Link } from "react-router-dom";

import { LoginForm } from "@/features/auth";

export function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-semibold">Вход</h1>
          <p className="text-muted-foreground text-sm">
            Войдите, чтобы участвовать в квестах.
          </p>
        </div>

        <LoginForm />

        <p className="text-muted-foreground text-center text-sm">
          Нет аккаунта?{" "}
          <Link to="/register" className="text-primary underline underline-offset-4">
            Зарегистрироваться
          </Link>
        </p>
      </div>
    </div>
  );
}
