import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import { Button } from './Button';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
}

export function Modal({ open, onClose, title, description, children }: ModalProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    const previous = document.activeElement as HTMLElement | null;
    ref.current?.focus();
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
      previous?.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-label={title}
    >
      <div
        className="animate-fade absolute inset-0 bg-[oklch(0.22_0.012_75/0.45)] backdrop-blur-[2px]"
        onClick={onClose}
      />
      <div
        ref={ref}
        tabIndex={-1}
        className="animate-rise relative w-full max-w-md rounded-[16px] border border-line bg-paper-raised p-6 shadow-[0_30px_70px_-25px_rgba(40,35,20,0.5)] focus:outline-none"
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <div>
            <h2 className="text-[20px]">{title}</h2>
            {description && <p className="mt-1 text-sm text-ink-soft">{description}</p>}
          </div>
          <Button variant="ghost" size="sm" onClick={onClose} aria-label="Fermer">
            ✕
          </Button>
        </div>
        {children}
      </div>
    </div>
  );
}
