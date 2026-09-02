import { Link } from "react-router-dom";

import { RegisterForm } from "@/features/auth";

export function RegisterPage() {
  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-8">
      <div className="w-full max-w-sm space-y-6">
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-semibold">Регистрация</h1>
          <p className="text-muted-foreground text-sm">
            Создайте аккаунт, чтобы собрать команду или пройти квест.
          </p>
        </div>

        <RegisterForm />

        <p className="text-muted-foreground text-center text-sm">
          Уже есть аккаунт?{" "}
          <Link to="/login" className="text-primary underline underline-offset-4">
            Войти
          </Link>
        </p>
      </div>
    </div>
  );
}
