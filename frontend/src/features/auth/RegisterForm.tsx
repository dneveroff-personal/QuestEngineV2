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
      setSession(response.accessToken, response.publicName, response.refreshToken);
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

  const onSubmit = form.handleSubmit((values) => mutation.mutate(values));

  const rootError =
    mutation.error instanceof ApiError && mutation.error.fieldErrors.length === 0
      ? mutation.error.detail || mutation.error.title
      : mutation.error instanceof NetworkError
        ? mutation.error.message
        : mutation.error
          ? "Не удалось зарегистрироваться."
          : null;

  return (
    <Form {...form}>
      <form onSubmit={onSubmit} className="space-y-4">
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Логин</FormLabel>
              <FormControl>
                <Input autoComplete="username" {...field} />
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
              <FormLabel>Отображаемое имя</FormLabel>
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
        {rootError && <p className="text-sm text-destructive">{rootError}</p>}
        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Регистрация…" : "Зарегистрироваться"}
        </Button>
      </form>
    </Form>
  );
}
