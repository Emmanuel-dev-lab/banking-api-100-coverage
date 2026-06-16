// Types miroir du contrat REST expose par l'API bancaire (XAF, entiers).
// Source unique de verite cote frontend.

export type Role = 'CLIENT' | 'ADMIN';
export type AccountType = 'CURRENT' | 'SAVINGS';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type LoanStatus = 'ACTIVE' | 'PAID_OFF';
export type TransactionType =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'LOAN_DISBURSEMENT'
  | 'LOAN_REPAYMENT'
  | 'INTEREST';

export interface ClientResponse {
  id: string;
  firstName: string;
  lastName: string;
}

export interface AccountResponse {
  id: string;
  clientId: string;
  type: AccountType;
  balance: number;
  status: AccountStatus;
}

export interface TransactionResponse {
  id: string;
  accountId: string;
  type: TransactionType;
  amount: number;
  date: string; // Instant ISO-8601
  relatedAccountId: string | null;
}

export interface InstallmentResponse {
  index: number;
  dueDate: string; // date ISO
  amount: number;
  principalPart: number;
  interestPart: number;
  paid: boolean;
}

export interface LoanResponse {
  id: string;
  clientId: string;
  accountId: string;
  principal: number;
  outstandingPrincipal: number;
  status: LoanStatus;
  late: boolean;
  schedule: InstallmentResponse[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TokenResponse {
  token: string;
}

export interface JobResultResponse {
  processed: number;
}

export interface ErrorResponse {
  code: string;
  message: string;
}

// --- Corps de requete ---

export interface LoginRequest {
  username: string;
  password: string;
}

export interface CreateClientRequest {
  firstName: string;
  lastName: string;
  username: string;
  password: string;
}

export interface UpdateClientRequest {
  firstName: string;
  lastName: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface OpenAccountRequest {
  type: AccountType;
  overdraftLimit: number;
  annualRate: number;
}

export interface AmountRequest {
  amount: number;
}

export interface CreateLoanRequest {
  clientId: string;
  accountId: string;
  principal: number;
  annualRate: number;
  termMonths: number;
}

export interface MeCreateLoanRequest {
  accountId: string;
  principal: number;
  annualRate: number;
  termMonths: number;
}

export interface TransferRequest {
  sourceAccountId: string;
  destAccountId: string;
  amount: number;
}
