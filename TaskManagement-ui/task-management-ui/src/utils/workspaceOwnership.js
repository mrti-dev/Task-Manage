export function isWorkspaceOwner(workspace, user) {
  return user?.id != null && workspace?.owner?.id != null
    && String(workspace.owner.id) === String(user.id);
}
