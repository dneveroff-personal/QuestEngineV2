import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="space-y-2">
      <h1 className="text-2xl font-semibold">Страница не найдена</h1>
      <Link to="/" className="text-primary text-sm underline underline-offset-4">
        Вернуться на главную
      </Link>
    </div>
  );
}
