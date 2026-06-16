import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './endpoints';
import type {
  AmountRequest,
  ChangePasswordRequest,
  CreateClientRequest,
  CreateLoanRequest,
  MeCreateLoanRequest,
  OpenAccountRequest,
  TransferRequest,
  UpdateClientRequest,
} from './types';

// Cles de cache structurees pour des invalidations ciblees.
export const keys = {
  me: ['me'] as const,
  myAccounts: (p: number, s: number) => ['me', 'accounts', p, s] as const,
  myLoans: (p: number, s: number) => ['me', 'loans', p, s] as const,
  clients: (p: number, s: number) => ['clients', p, s] as const,
  client: (id: string) => ['clients', id] as const,
  clientAccounts: (id: string, p: number, s: number) => ['clients', id, 'accounts', p, s] as const,
  clientLoans: (id: string, p: number, s: number) => ['clients', id, 'loans', p, s] as const,
  accounts: (p: number, s: number) => ['accounts', p, s] as const,
  account: (id: string) => ['accounts', id] as const,
  transactions: (id: string, p: number, s: number) => ['accounts', id, 'transactions', p, s] as const,
  loans: (p: number, s: number) => ['loans', p, s] as const,
  loan: (id: string) => ['loans', id] as const,
};

// --- Requetes ---

export const useMe = () => useQuery({ queryKey: keys.me, queryFn: api.me });

export const useMyAccounts = (p: number, s: number) =>
  useQuery({ queryKey: keys.myAccounts(p, s), queryFn: () => api.myAccounts(p, s) });

export const useMyLoans = (p: number, s: number) =>
  useQuery({ queryKey: keys.myLoans(p, s), queryFn: () => api.myLoans(p, s) });

export const useClients = (p: number, s: number) =>
  useQuery({ queryKey: keys.clients(p, s), queryFn: () => api.listClients(p, s) });

export const useClient = (id: string) =>
  useQuery({ queryKey: keys.client(id), queryFn: () => api.getClient(id) });

export const useClientAccounts = (id: string, p: number, s: number) =>
  useQuery({ queryKey: keys.clientAccounts(id, p, s), queryFn: () => api.clientAccounts(id, p, s) });

export const useClientLoans = (id: string, p: number, s: number) =>
  useQuery({ queryKey: keys.clientLoans(id, p, s), queryFn: () => api.clientLoans(id, p, s) });

export const useAccounts = (p: number, s: number) =>
  useQuery({ queryKey: keys.accounts(p, s), queryFn: () => api.listAccounts(p, s) });

export const useAccount = (id: string) =>
  useQuery({ queryKey: keys.account(id), queryFn: () => api.getAccount(id) });

export const useTransactions = (id: string, p: number, s: number) =>
  useQuery({ queryKey: keys.transactions(id, p, s), queryFn: () => api.transactions(id, p, s) });

export const useLoans = (p: number, s: number) =>
  useQuery({ queryKey: keys.loans(p, s), queryFn: () => api.listLoans(p, s) });

export const useLoan = (id: string) =>
  useQuery({ queryKey: keys.loan(id), queryFn: () => api.getLoan(id) });

// --- Mutations ---

export function useDeposit(accountId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AmountRequest) => api.deposit(accountId, body),
    onSuccess: () => invalidateAccount(qc, accountId),
  });
}

export function useWithdraw(accountId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AmountRequest) => api.withdraw(accountId, body),
    onSuccess: () => invalidateAccount(qc, accountId),
  });
}

export function useTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: TransferRequest) => api.transfer(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['accounts'] });
      qc.invalidateQueries({ queryKey: ['me'] });
    },
  });
}

export function useOpenMyAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: OpenAccountRequest) => api.openMyAccount(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['me'] }),
  });
}

export function useRequestMyLoan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: MeCreateLoanRequest) => api.requestMyLoan(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['me'] }),
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (body: ChangePasswordRequest) => api.changePassword(body),
  });
}

export function useCreateClient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateClientRequest) => api.createClient(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients'] }),
  });
}

export function useUpdateClient(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateClientRequest) => api.updateClient(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.client(id) });
      qc.invalidateQueries({ queryKey: ['clients'] });
    },
  });
}

export function useOpenClientAccount(clientId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: OpenAccountRequest) => api.openClientAccount(clientId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clients', clientId] }),
  });
}

export function useCreateLoan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateLoanRequest) => api.createLoan(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['loans'] });
      qc.invalidateQueries({ queryKey: ['accounts'] });
      qc.invalidateQueries({ queryKey: ['clients'] });
    },
  });
}

export function useRepayLoan(loanId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AmountRequest) => api.repayLoan(loanId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.loan(loanId) });
      qc.invalidateQueries({ queryKey: ['loans'] });
      qc.invalidateQueries({ queryKey: ['accounts'] });
    },
  });
}

type AccountAction = 'freeze' | 'close' | 'reactivate';

export function useAccountStatusAction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, action }: { id: string; action: AccountAction }) => {
      if (action === 'freeze') return api.freezeAccount(id);
      if (action === 'close') return api.closeAccount(id);
      return api.reactivateAccount(id);
    },
    onSuccess: (_data, { id }) => invalidateAccount(qc, id),
  });
}

export function useCapitalizeInterest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api.capitalizeInterest(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['accounts'] });
      qc.invalidateQueries({ queryKey: ['me'] });
    },
  });
}

export function useFlagOverdue() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api.flagOverdue(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['loans'] }),
  });
}

function invalidateAccount(qc: ReturnType<typeof useQueryClient>, accountId: string) {
  qc.invalidateQueries({ queryKey: keys.account(accountId) });
  qc.invalidateQueries({ queryKey: ['accounts', accountId, 'transactions'] });
  qc.invalidateQueries({ queryKey: ['accounts'] });
  qc.invalidateQueries({ queryKey: ['me'] });
}
