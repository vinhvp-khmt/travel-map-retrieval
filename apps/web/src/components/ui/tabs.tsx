import * as React from 'react'
import { cn } from '@/lib/utils'

export function TabsList({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('grid rounded-lg bg-secondary p-1 text-muted-foreground', className)} role="tablist" {...props} />
}

export function TabsTrigger({ className, active, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { active?: boolean }) {
  return (
    <button
      role="tab"
      aria-selected={active}
      className={cn('rounded-md px-3 py-2 text-sm font-semibold transition-colors', active ? 'bg-card text-foreground shadow-sm' : 'hover:text-foreground', className)}
      {...props}
    />
  )
}
