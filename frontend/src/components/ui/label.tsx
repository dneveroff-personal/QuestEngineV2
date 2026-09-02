import * as React from "react"

import { cn } from "@/lib/utils"

/**
 * Обычный нативный `<label>`, а не `@base-ui/react`-примитив — в отличие
 * от Button, здесь нет реальной необходимости в headless-поведении
 * (focus management, ARIA), которое давал бы примитив. Простой и
 * предсказуемый вариант.
 */
function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        "flex select-none items-center gap-2 text-sm leading-none font-medium peer-disabled:cursor-not-allowed peer-disabled:opacity-50 group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50",
        className
      )}
      {...props}
    />
  )
}

export { Label }
