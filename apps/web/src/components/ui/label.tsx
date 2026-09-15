import * as React from 'react'
import { cn } from '@/lib/utils'

export function Label({ className, ...props }: React.LabelHTMLAttributes<HTMLLabelElement>) {
  return <label className={cn('text-xs font-bold uppercase tracking-[0.14em] text-muted-foreground', className)} {...props} />
}
