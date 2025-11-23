export enum PermissionRole {
  OWNER = 'OWNER',
  EDITOR = 'EDITOR',
  VIEWER = 'VIEWER'
}

export interface Collaborator {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  role: PermissionRole;
  grantedAt: string;
  isOwner: boolean;
}

export interface Invitation {
  id: string;
  treeId: string;
  treeName: string;
  inviterName: string;
  inviterEmail: string;
  inviteeEmail: string;
  role: PermissionRole;
  token: string;
  expiresAt: string;
  createdAt: string;
  status: string;
  expired: boolean;
}

export interface InviteCollaboratorRequest {
  email: string;
  role: PermissionRole;
  message?: string;
}
