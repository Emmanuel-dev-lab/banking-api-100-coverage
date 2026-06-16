import { Button } from './Button';

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onPage: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, onPage }: PaginationProps) {
  if (totalElements === 0) return null;
  const human = totalPages === 0 ? 1 : totalPages;
  return (
    <div className="flex items-center justify-between gap-4 border-t border-line px-5 py-3 text-[13px] text-ink-soft">
      <span className="tnum">
        {totalElements} élément{totalElements > 1 ? 's' : ''} · page {page + 1} / {human}
      </span>
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => onPage(page - 1)}>
          ← Précédent
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page + 1 >= human}
          onClick={() => onPage(page + 1)}
        >
          Suivant →
        </Button>
      </div>
    </div>
  );
}
