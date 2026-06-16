import { forwardRef } from 'react';
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

const fieldBase =
  'h-10 w-full rounded-[10px] border border-line-strong bg-paper-raised px-3 text-sm text-ink transition-colors placeholder:text-ink-faint focus:border-forest focus:outline-none';

export function Label({ children, htmlFor }: { children: ReactNode; htmlFor?: string }) {
  return (
    <label
      htmlFor={htmlFor}
      className="mb-1.5 block text-[12px] font-semibold uppercase tracking-[0.06em] text-ink-soft"
    >
      {children}
    </label>
  );
}

interface FieldWrapProps {
  label: string;
  htmlFor?: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}

export function FieldWrap({ label, htmlFor, error, hint, children }: FieldWrapProps) {
  return (
    <div>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {error ? (
        <p className="mt-1 text-[12px] text-danger">{error}</p>
      ) : hint ? (
        <p className="mt-1 text-[12px] text-ink-faint">{hint}</p>
      ) : null}
    </div>
  );
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function Input({ className, ...rest }, ref) {
    return <input ref={ref} className={cn(fieldBase, className)} {...rest} />;
  },
);

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  function Select({ className, children, ...rest }, ref) {
    return (
      <select ref={ref} className={cn(fieldBase, 'cursor-pointer appearance-none pr-9', className)} {...rest}>
        {children}
      </select>
    );
  },
);
