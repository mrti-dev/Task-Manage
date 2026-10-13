import axiosClient from './axiosClient';

export const createInvitationApi = (workspaceId, data) =>
  axiosClient.post(`/api/workspaces/${workspaceId}/invitations`, data);

export const getWorkspaceInvitationsApi = (workspaceId) =>
  axiosClient.get(`/api/workspaces/${workspaceId}/invitations`);

export const resendInvitationApi = (id) =>
  axiosClient.post(`/api/workspace-invitations/${id}/resend`);
