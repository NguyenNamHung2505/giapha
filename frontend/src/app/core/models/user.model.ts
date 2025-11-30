export interface User {
  id: string;
  username: string;
  email?: string;
  name: string;
  admin: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  name: string;
  email?: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  user: User;
}
