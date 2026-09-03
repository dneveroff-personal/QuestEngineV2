import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

import { login } from "@/api/auth";
import { ApiError, NetworkError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { setSession } from "@/lib/auth-token";
import { loginSchema, type LoginFormValues } from "@/features/auth/schemas";

export function LoginForm() {
  const navigate = useNavigate();
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (response) => {
      setSession(response.token, response.publicName);
      navigate("/", { replace: true });
    },
    onError: (error) => {
      // Ошибки валидации конкретных полей — привязать к полю формы, а не
      // показывать одним общим текстом (architecture.md §12.1: backend —
      // источник истины, клиентская схема могла с ним разойтись).
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === "username" || fieldError.field === "password") {
            form.setError(fieldError.field, { message: fieldError.message });
          }
        }
      }
    },
  });

  function onSubmit(values: LoginFormValues) {
    mutation.mutate(values);
  }

  // Ошибка, которая не привязана к конкретному полю (неверный логин/пароль,
  // сеть недоступна и т.п.) — показывается одним блоком над кнопкой.
  const generalError =
    mutation.error instanceof NetworkError
      ? mutation.error.message
      : mutation.error instanceof ApiError && mutation.error.fieldErrors.length === 0
        ? mutation.error.message
        : null;

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Имя пользователя</FormLabel>
              <FormControl>
                <Input autoComplete="username" autoFocus {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Пароль</FormLabel>
              <FormControl>
                <Input type="password" autoComplete="current-password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {generalError && (
          <p role="alert" className="text-destructive text-sm">
            {generalError}
          </p>
        )}

        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Входим..." : "Войти"}
        </Button>
      </form>
    </Form>
  );
}
