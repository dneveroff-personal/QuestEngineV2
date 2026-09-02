import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

import { register } from "@/api/auth";
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
import { registerSchema, type RegisterFormValues } from "@/features/auth/schemas";

const FIELD_NAMES = ["username", "password", "email", "publicName"] as const;

export function RegisterForm() {
  const navigate = useNavigate();
  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { username: "", password: "", email: "", publicName: "" },
  });

  const mutation = useMutation({
    mutationFn: register,
    onSuccess: (response) => {
      // Регистрация сразу авторизует (backend возвращает LoginResponse) —
      // отдельного шага "теперь войдите" не требуется.
      setSession({ token: response.token, publicName: response.publicName });
      navigate("/", { replace: true });
    },
    onError: (error) => {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          if ((FIELD_NAMES as readonly string[]).includes(fieldError.field)) {
            form.setError(fieldError.field as (typeof FIELD_NAMES)[number], {
              message: fieldError.message,
            });
          }
        }
      }
    },
  });

  function onSubmit(values: RegisterFormValues) {
    mutation.mutate(values);
  }

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
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl>
                <Input type="email" autoComplete="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="publicName"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Публичное имя (необязательно)</FormLabel>
              <FormControl>
                <Input autoComplete="nickname" {...field} />
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
                <Input type="password" autoComplete="new-password" {...field} />
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
          {mutation.isPending ? "Регистрируем..." : "Зарегистрироваться"}
        </Button>
      </form>
    </Form>
  );
}
