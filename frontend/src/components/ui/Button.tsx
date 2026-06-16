import type { ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';
type Size = 'sm' | 'md';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const base =
  'inline-flex items-center justify-center gap-2 rounded-[10px] font-medium transition-all duration-150 disabled:cursor-not-allowed disabled:opacity-55 active:translate-y-px select-none';

const variants: Record<Variant, string> = {
  primary:
    'bg-forest text-paper-raised hover:bg-forest-deep shadow-[0_1px_0_rgba(255,255,255,0.15)_inset,0_8px_20px_-12px_rgba(20,65,46,0.7)]',
  secondary: 'border border-line-strong bg-paper-raised text-ink hover:border-ink-faint hover:bg-paper',
  ghost: 'text-ink-soft hover:bg-forest-soft hover:text-forest-deep',
  danger: 'border border-danger/30 bg-danger-soft text-danger hover:bg-danger hover:text-paper-raised',
};

const sizes: Record<Size, string> = {
  sm: 'h-8 px-3 text-[13px]',
  md: 'h-10 px-4 text-sm',
};

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      className={cn(base, variants[variant], sizes[size], className)}
      disabled={disabled || loading}
      {...rest}
    >
      {loading && (
        <span className="size-3.5 animate-spin rounded-full border-2 border-current border-r-transparent" />
      )}
      {children}
    </button>
  );
}
