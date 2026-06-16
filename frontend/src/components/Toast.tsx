import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

type ToastTone = 'success' | 'error' | 'info';
interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

interface ToastContextValue {
  push: (message: string, tone?: ToastTone) => void;
  success: (message: string) => void;
  error: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let counter = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const remove = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const push = useCallback(
    (message: string, tone: ToastTone = 'info') => {
      const id = ++counter;
      setToasts((prev) => [...prev, { id, tone, message }]);
      setTimeout(() => remove(id), 4200);
    },
    [remove],
  );

  const value = useMemo<ToastContextValue>(
    () => ({
      push,
      success: (m) => push(m, 'success'),
      error: (m) => push(m, 'error'),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-6 right-6 z-50 flex w-[min(92vw,360px)] flex-col gap-2.5">
        {toasts.map((t) => (
          <button
            key={t.id}
            onClick={() => remove(t.id)}
            className={`animate-rise flex items-start gap-3 rounded-[12px] border px-4 py-3 text-left text-sm shadow-[0_12px_30px_-12px_rgba(40,35,20,0.4)] backdrop-blur ${toneClasses[t.tone]}`}
          >
            <span className="mt-0.5 text-base leading-none">{toneIcon[t.tone]}</span>
            <span className="flex-1">{t.message}</span>
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

const toneClasses: Record<ToastTone, string> = {
  success: 'border-forest/30 bg-forest-soft text-forest-deep',
  error: 'border-danger/30 bg-danger-soft text-danger',
  info: 'border-line-strong bg-paper-raised text-ink',
};

const toneIcon: Record<ToastTone, string> = {
  success: '✓',
  error: '!',
  info: 'i',
};

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast doit etre utilise dans ToastProvider.');
  return ctx;
}
