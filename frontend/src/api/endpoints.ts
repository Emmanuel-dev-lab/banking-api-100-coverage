import { request } from './client';
import type {
  AccountResponse,
  AmountRequest,
  ChangePasswordRequest,
  ClientResponse,
  CreateClientRequest,
  CreateLoanRequest,
  JobResultResponse,
  LoanResponse,
  LoginRequest,
  MeCreateLoanRequest,
  OpenAccountRequest,
  PageResponse,
  TokenResponse,
  TransactionResponse,
  TransferRequest,
  UpdateClientRequest,
} from './types';

const page = (p: number, s: number) => `?page=${p}&size=${s}`;

export const api = {
  // --- Auth ---
  login: (body: LoginRequest) =>
    request<TokenResponse>('/api/auth/login', { method: 'POST', body, anonymous: true }),

  // --- Moi (self-service) ---
  me: () => request<ClientResponse>('/api/me'),
  myAccounts: (p: number, s: number) =>
    request<PageResponse<AccountResponse>>(`/api/me/accounts${page(p, s)}`),
  myLoans: (p: number, s: number) =>
    request<PageResponse<LoanResponse>>(`/api/me/loans${page(p, s)}`),
  openMyAccount: (body: OpenAccountRequest) =>
    request<AccountResponse>('/api/me/accounts', { method: 'POST', body }),
  requestMyLoan: (body: MeCreateLoanRequest) =>
    request<LoanResponse>('/api/me/loans', { method: 'POST', body }),
  changePassword: (body: ChangePasswordRequest) =>
    request<void>('/api/me/password', { method: 'POST', body }),

  // --- Clients (ADMIN sauf get/patch owner) ---
  createClient: (body: CreateClientRequest) =>
    request<ClientResponse>('/api/clients', { method: 'POST', body }),
  listClients: (p: number, s: number) =>
    request<PageResponse<ClientResponse>>(`/api/clients${page(p, s)}`),
  getClient: (id: string) => request<ClientResponse>(`/api/clients/${id}`),
  updateClient: (id: string, body: UpdateClientRequest) =>
    request<ClientResponse>(`/api/clients/${id}`, { method: 'PATCH', body }),
  clientAccounts: (id: string, p: number, s: number) =>
    request<PageResponse<AccountResponse>>(`/api/clients/${id}/accounts${page(p, s)}`),
  clientLoans: (id: string, p: number, s: number) =>
    request<PageResponse<LoanResponse>>(`/api/clients/${id}/loans${page(p, s)}`),
  openClientAccount: (id: string, body: OpenAccountRequest) =>
    request<AccountResponse>(`/api/clients/${id}/accounts`, { method: 'POST', body }),

  // --- Comptes ---
  listAccounts: (p: number, s: number) =>
    request<PageResponse<AccountResponse>>(`/api/accounts${page(p, s)}`),
  getAccount: (id: string) => request<AccountResponse>(`/api/accounts/${id}`),
  deposit: (id: string, body: AmountRequest) =>
    request<AccountResponse>(`/api/accounts/${id}/deposit`, { method: 'POST', body }),
  withdraw: (id: string, body: AmountRequest) =>
    request<AccountResponse>(`/api/accounts/${id}/withdraw`, { method: 'POST', body }),
  transactions: (id: string, p: number, s: number) =>
    request<PageResponse<TransactionResponse>>(`/api/accounts/${id}/transactions${page(p, s)}`),
  freezeAccount: (id: string) =>
    request<AccountResponse>(`/api/accounts/${id}/freeze`, { method: 'POST' }),
  closeAccount: (id: string) =>
    request<AccountResponse>(`/api/accounts/${id}/close`, { method: 'POST' }),
  reactivateAccount: (id: string) =>
    request<AccountResponse>(`/api/accounts/${id}/reactivate`, { method: 'POST' }),

  // --- Virements ---
  transfer: (body: TransferRequest) =>
    request<void>('/api/transfers', { method: 'POST', body }),

  // --- Prets ---
  listLoans: (p: number, s: number) =>
    request<PageResponse<LoanResponse>>(`/api/loans${page(p, s)}`),
  getLoan: (id: string) => request<LoanResponse>(`/api/loans/${id}`),
  createLoan: (body: CreateLoanRequest) =>
    request<LoanResponse>('/api/loans', { method: 'POST', body }),
  repayLoan: (id: string, body: AmountRequest) =>
    request<void>(`/api/loans/${id}/repay`, { method: 'POST', body }),

  // --- Jobs (admin) ---
  capitalizeInterest: () =>
    request<JobResultResponse>('/api/admin/jobs/interest', { method: 'POST' }),
  flagOverdue: () => request<JobResultResponse>('/api/admin/jobs/overdue', { method: 'POST' }),
};
