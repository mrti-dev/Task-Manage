import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Typography, Tabs, Tab, Card, CardContent, Grid, Button,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  IconButton, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Select, MenuItem, FormControl, InputLabel,
  Skeleton, Alert, Snackbar, Tooltip,
} from '@mui/material';
import {
  AddRounded as AddIcon, EditRounded as EditIcon, DeleteRounded as DeleteIcon,
  FolderRounded as FolderIcon, PeopleRounded as PeopleIcon,
  SettingsRounded as SettingsIcon, ArrowBackRounded as BackIcon,
  PersonAddRounded as PersonAddIcon,
} from '@mui/icons-material';
import {
  getWorkspaceByIdApi, updateWorkspaceApi, deleteWorkspaceApi,
} from '../../api/workspaceApi';
import {
  getWorkspaceMembersApi, createWorkspaceMemberApi,
  updateWorkspaceMemberApi, deleteWorkspaceMemberApi,
} from '../../api/workspaceMemberApi';
import { getProjectsApi, createProjectApi, deleteProjectApi } from '../../api/projectApi';
import { useAuth } from '../../contexts/AuthContext';
import { on } from '../../utils/eventBus';
import { projectStatusColors } from '../../utils/constants';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/common/ConfirmDialog';

function TabPanel({ children, value, index }) {
  return value === index ? <Box sx={{ pt: 3 }}>{children}</Box> : null;
}

export default function WorkspaceDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [workspace, setWorkspace] = useState(null);
  const [projects, setProjects] = useState([]);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tabValue, setTabValue] = useState(0);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState({ name: '', description: '' });
  const [editLoading, setEditLoading] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [addMemberForm, setAddMemberForm] = useState({ email: '', role: 'MEMBER' });
  const [addMemberLoading, setAddMemberLoading] = useState(false);
  const [roleChange, setRoleChange] = useState({ open: false, memberId: null, role: 'MEMBER' });
  const [removeMember, setRemoveMember] = useState({ open: false, memberId: null });
  const [deleteProject, setDeleteProject] = useState({ open: false, id: null, name: '' });
  const [hoveredProject, setHoveredProject] = useState(null);
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const [createProjectForm, setCreateProjectForm] = useState({ name: '', description: '', endDate: '' });
  const [createProjectLoading, setCreateProjectLoading] = useState(false);

  const currentUserMember = members.find((m) => m.user?.id === user?.id);
  const isOwner = currentUserMember?.role === 'OWNER';

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [wsRes, projectsRes, membersRes] = await Promise.all([
        getWorkspaceByIdApi(id),
        getProjectsApi({ workspaceId: id }),
        getWorkspaceMembersApi(),
      ]);
      setWorkspace(wsRes?.data?.data ?? wsRes?.data);
      setProjects(projectsRes?.data?.data?.content ?? []);
      const allMembers = membersRes?.data?.data ?? membersRes?.data ?? [];
      setMembers(Array.isArray(allMembers) ? allMembers.filter((m) => m.workspaceId === Number(id) || m.id) : []);
    } catch {
      setSnackbar({ open: true, message: 'Failed to load workspace', severity: 'error' });
    } finally { setLoading(false); }
  }, [id]);

  useEffect(() => { fetchData(); }, [fetchData]);

  useEffect(() => {
    const unsub = on('PROJECT_CREATED', () => fetchData());
    return unsub;
  }, [fetchData]);

  const handleDeleteProject = async () => {
    setDeleteLoading(true);
    try {
      await deleteProjectApi(deleteProject.id);
      setProjects((prev) => prev.filter((p) => p.id !== deleteProject.id));
      setDeleteProject({ open: false, id: null, name: '' });
      setSnackbar({ open: true, message: 'Project deleted', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Delete failed', severity: 'error' });
    } finally { setDeleteLoading(false); }
  };

  const handleCreateProject = async () => {
    if (!createProjectForm.name.trim()) return;
    setCreateProjectLoading(true);
    try {
      const { data: res } = await createProjectApi({ workspaceId: Number(id), name: createProjectForm.name, description: createProjectForm.description, endDate: createProjectForm.endDate || null });
      const newProject = res?.data ?? res;
      setProjects((prev) => [...prev, newProject]);
      setCreateProjectOpen(false);
      setCreateProjectForm({ name: '', description: '', endDate: '' });
      setSnackbar({ open: true, message: 'Project created', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Create failed', severity: 'error' });
    } finally { setCreateProjectLoading(false); }
  };

  const handleEdit = async () => {
    setEditLoading(true);
    try {
      const { data: res } = await updateWorkspaceApi(id, editForm);
      setWorkspace(res?.data ?? res);
      setEditOpen(false);
      setSnackbar({ open: true, message: 'Workspace updated', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Update failed', severity: 'error' });
    } finally { setEditLoading(false); }
  };

  const handleDelete = async () => {
    setDeleteLoading(true);
    try {
      await deleteWorkspaceApi(id);
      navigate('/', { replace: true });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Delete failed', severity: 'error' });
      setDeleteLoading(false);
      setDeleteOpen(false);
    }
  };

  const handleAddMember = async () => {
    if (!addMemberForm.email) return;
    setAddMemberLoading(true);
    try {
      await createWorkspaceMemberApi({ workspaceId: Number(id), email: addMemberForm.email, role: addMemberForm.role });
      setAddMemberOpen(false);
      setAddMemberForm({ email: '', role: 'MEMBER' });
      setSnackbar({ open: true, message: 'Member added', severity: 'success' });
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to add member', severity: 'error' });
    } finally { setAddMemberLoading(false); }
  };

  const handleRoleChange = async () => {
    try {
      await updateWorkspaceMemberApi(roleChange.memberId, { role: roleChange.role });
      setRoleChange({ open: false, memberId: null, role: 'MEMBER' });
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to update role', severity: 'error' });
    }
  };

  const handleRemoveMember = async () => {
    try {
      await deleteWorkspaceMemberApi(removeMember.memberId);
      setRemoveMember({ open: false, memberId: null });
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to remove member', severity: 'error' });
    }
  };

  if (loading) {
    return (
      <Box>
        <Skeleton variant="text" width={300} height={40} sx={{ borderRadius: 2 }} />
        <Skeleton variant="text" width={200} height={24} sx={{ mb: 2, borderRadius: 2 }} />
        <Skeleton variant="rounded" height={300} sx={{ borderRadius: 4 }} />
      </Box>
    );
  }

  if (!workspace) {
    return (
      <EmptyState title="Workspace not found" description="The workspace you&apos;re looking for doesn&apos;t exist"
        action={<Button startIcon={<BackIcon />} onClick={() => navigate('/')} sx={{ color: '#3B82F6' }}>Back to Workspaces</Button>} />
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
        <IconButton onClick={() => navigate('/')} size="small" sx={{ bgcolor: '#F8FAFC', borderRadius: 2 }}>
          <BackIcon sx={{ fontSize: 20, color: '#64748B' }} />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h4" sx={{ color: '#0F172A' }}>{workspace.name}</Typography>
          <Typography variant="body2" sx={{ color: '#64748B' }}>{workspace.description || 'No description'}</Typography>
        </Box>
      </Box>

      <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}
        sx={{ mb: 1, '& .MuiTab-root': { minHeight: 48 } }}>
        <Tab icon={<FolderIcon sx={{ fontSize: 18 }} />} label="Projects" iconPosition="start" />
        <Tab icon={<PeopleIcon sx={{ fontSize: 18 }} />} label="Members" iconPosition="start" />
        {isOwner && <Tab icon={<SettingsIcon sx={{ fontSize: 18 }} />} label="Settings" iconPosition="start" />}
      </Tabs>

      <TabPanel value={tabValue} index={0}>
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
          {isOwner && (
            <Button variant="contained" startIcon={<AddIcon />}
              onClick={() => setCreateProjectOpen(true)}
              sx={{ background: 'linear-gradient(135deg, #3B82F6, #60A5FA)', borderRadius: 2 }}>
              Create Project
            </Button>
          )}
        </Box>
        {projects.length === 0 ? (
          <EmptyState icon={<FolderIcon sx={{ fontSize: 36 }} />} title="No projects yet" />
        ) : (
          <Grid container spacing={3}>
            {projects.map((project) => (
              <Grid item xs={12} sm={6} md={4} key={project.id}>
                <Card sx={{ borderRadius: 3, border: '1px solid #E2E8F0', cursor: 'pointer', position: 'relative',
                  '&:hover': { transform: 'translateY(-3px)', boxShadow: '0 8px 24px rgba(59,130,246,0.1)' },
                  transition: 'all 200ms ease'
                }} onClick={() => navigate(`/workspaces/${id}/projects/${project.id}`)}
                  onMouseEnter={() => setHoveredProject(project.id)} onMouseLeave={() => setHoveredProject(null)}>
                  {isOwner && hoveredProject === project.id && (
                    <IconButton size="small" onClick={(e) => { e.stopPropagation(); setDeleteProject({ open: true, id: project.id, name: project.name }); }}
                      sx={{ position: 'absolute', top: 8, right: 8, color: '#EF4444', bgcolor: '#fff',
                        '&:hover': { bgcolor: '#FEE2E2' } }}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  )}
                  <CardContent sx={{ p: 3 }}>
                    <Box sx={{ width: 42, height: 42, borderRadius: 2.5, bgcolor: '#F1F5F9',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2 }}>
                      <FolderIcon sx={{ color: '#3B82F6', fontSize: 20 }} />
                    </Box>
                    <Typography variant="h6" fontWeight={700} noWrap sx={{ mb: 0.5, color: '#0F172A' }}>
                      {project.name}
                    </Typography>
                    <Typography variant="body2" noWrap sx={{ mb: 2, color: '#94A3B8' }}>
                      {project.description || 'No description'}
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {(() => {
                        const pc = projectStatusColors[project.status] || { bg: '#F1F5F9', text: '#64748B' };
                        return <Chip label={project.status} size="small" sx={{ fontWeight: 600, fontSize: '0.6875rem', bgcolor: pc.bg, color: pc.text }} />;
                      })()}
                      {project.endDate && (
                        <Typography variant="caption" sx={{ color: '#94A3B8' }}>Due: {project.endDate}</Typography>
                      )}
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
          {isOwner && (
            <Button variant="contained" startIcon={<PersonAddIcon />} onClick={() => setAddMemberOpen(true)}
            sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #3B82F6, #60A5FA)' }}>
              Add Member
            </Button>
          )}
        </Box>
        <TableContainer component={Card} variant="outlined" sx={{ borderRadius: 3, border: '1px solid #E2E8F0' }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>User</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Joined At</TableCell>
                {isOwner && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {members.length === 0 ? (
                <TableRow><TableCell colSpan={isOwner ? 4 : 3} align="center" sx={{ py: 4 }}>No members</TableCell></TableRow>
              ) : (
                members.map((member) => (
                  <TableRow key={member.id}>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Box sx={{ width: 34, height: 34, borderRadius: '50%', bgcolor: '#3B82F6',
                          display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 700, fontSize: 13 }}>
                          {member.user?.username?.[0]?.toUpperCase() || '?'}
                        </Box>
                        <Box>
                          <Typography variant="body2" fontWeight={600} sx={{ color: '#0F172A' }}>
                            {member.user?.username || 'Unknown'}
                          </Typography>
                          <Typography variant="caption" sx={{ color: '#94A3B8' }}>{member.user?.email || ''}</Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip label={member.role} size="small"
                        sx={{
                          fontWeight: 600, fontSize: '0.6875rem',
                          bgcolor: member.role === 'OWNER' ? '#FEF3C7' : '#F1F5F9',
                          color: member.role === 'OWNER' ? '#D97706' : '#64748B',
                        }} />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ color: '#94A3B8' }}>
                        {member.joinedAt ? new Date(member.joinedAt).toLocaleDateString() : '-'}
                      </Typography>
                    </TableCell>
                    {isOwner && (
                      <TableCell align="right">
                        {member.role !== 'OWNER' && (
                          <>
                            <Tooltip title="Change role"><IconButton size="small" onClick={() => setRoleChange({ open: true, memberId: member.id, role: member.role })}><EditIcon fontSize="small" sx={{ color: '#3B82F6' }} /></IconButton></Tooltip>
                            <Tooltip title="Remove"><IconButton size="small" onClick={() => setRemoveMember({ open: true, memberId: member.id })}><DeleteIcon fontSize="small" sx={{ color: '#EF4444' }} /></IconButton></Tooltip>
                          </>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      {isOwner && (
        <TabPanel value={tabValue} index={2}>
          <Card variant="outlined" sx={{ maxWidth: 500, borderRadius: 3, border: '1px solid #E2E8F0' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" fontWeight={700} sx={{ mb: 2.5, color: '#0F172A' }}>Workspace Settings</Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <TextField fullWidth label="Name" defaultValue={workspace.name} size="small" />
                <TextField fullWidth label="Description" defaultValue={workspace.description || ''} size="small" multiline rows={2} />
                <Box sx={{ display: 'flex', gap: 1.5 }}>
                   <Button variant="contained" onClick={() => { setEditForm({ name: workspace.name, description: workspace.description || '' }); setEditOpen(true); }}
                    sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #3B82F6, #60A5FA)' }}>
                    Edit
                   </Button>
                  <Button variant="outlined" color="error" onClick={() => setDeleteOpen(true)} sx={{ borderRadius: 2 }}>
                    Delete Workspace
                  </Button>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </TabPanel>
      )}

      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#0F172A' }}>Edit Workspace</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth label="Name" value={editForm.name}
            onChange={(e) => setEditForm((p) => ({ ...p, name: e.target.value }))} sx={{ mt: 1, mb: 2 }} />
          <TextField fullWidth label="Description" value={editForm.description}
            onChange={(e) => setEditForm((p) => ({ ...p, description: e.target.value }))} multiline rows={3} />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setEditOpen(false)} disabled={editLoading} sx={{ color: '#64748B' }}>Cancel</Button>
          <Button variant="contained" onClick={handleEdit} disabled={editLoading} sx={{ borderRadius: 2 }}>{editLoading ? 'Saving...' : 'Save'}</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteOpen} title="Delete Workspace" message="This action cannot be undone." confirmText="Delete"
        onConfirm={handleDelete} onCancel={() => setDeleteOpen(false)} loading={deleteLoading} />

      <Dialog open={addMemberOpen} onClose={() => setAddMemberOpen(false)} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#0F172A' }}>Add Member</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth label="Email" type="email" value={addMemberForm.email}
            onChange={(e) => setAddMemberForm((p) => ({ ...p, email: e.target.value }))} sx={{ mt: 1, mb: 2 }} />
          <FormControl fullWidth>
            <InputLabel>Role</InputLabel>
            <Select value={addMemberForm.role} label="Role" onChange={(e) => setAddMemberForm((p) => ({ ...p, role: e.target.value }))}>
              <MenuItem value="MEMBER">Member</MenuItem>
              <MenuItem value="OWNER">Owner</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setAddMemberOpen(false)} disabled={addMemberLoading} sx={{ color: '#64748B' }}>Cancel</Button>
          <Button variant="contained" onClick={handleAddMember} disabled={addMemberLoading}
            sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #3B82F6, #60A5FA)' }}>
            {addMemberLoading ? 'Adding...' : 'Add'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={createProjectOpen} onClose={() => setCreateProjectOpen(false)} maxWidth="sm" fullWidth
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#0F172A' }}>Create Project</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField fullWidth label="Name" required value={createProjectForm.name}
              onChange={(e) => setCreateProjectForm((p) => ({ ...p, name: e.target.value }))} />
            <TextField fullWidth label="Description" value={createProjectForm.description} multiline rows={3}
              onChange={(e) => setCreateProjectForm((p) => ({ ...p, description: e.target.value }))} />
            <TextField fullWidth type="date" value={createProjectForm.endDate}
              onChange={(e) => setCreateProjectForm((p) => ({ ...p, endDate: e.target.value }))} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => { setCreateProjectOpen(false); setCreateProjectForm({ name: '', description: '', endDate: '' }); }} sx={{ color: '#64748B' }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateProject} disabled={createProjectLoading || !createProjectForm.name.trim()}
            sx={{ borderRadius: 2, background: 'linear-gradient(135deg, #3B82F6, #60A5FA)' }}>
            {createProjectLoading ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={roleChange.open} onClose={() => setRoleChange({ open: false, memberId: null, role: 'MEMBER' })} maxWidth="xs" fullWidth
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#0F172A' }}>Change Role</DialogTitle>
        <DialogContent>
          <FormControl fullWidth>
            <InputLabel>Role</InputLabel>
            <Select value={roleChange.role} label="Role" onChange={(e) => setRoleChange((p) => ({ ...p, role: e.target.value }))}>
              <MenuItem value="MEMBER">Member</MenuItem>
              <MenuItem value="OWNER">Owner</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setRoleChange({ open: false, memberId: null, role: 'MEMBER' })} sx={{ color: '#64748B' }}>Cancel</Button>
          <Button variant="contained" onClick={handleRoleChange} sx={{ borderRadius: 2 }}>Save</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={removeMember.open} title="Remove Member" message="Remove this member from the workspace?"
        confirmText="Remove" onConfirm={handleRemoveMember} onCancel={() => setRemoveMember({ open: false, memberId: null })} />

      <ConfirmDialog open={deleteProject.open} title="Delete Project" message={`Delete "${deleteProject.name}" permanently?`}
        confirmText="Delete" onConfirm={handleDeleteProject} onCancel={() => setDeleteProject({ open: false, id: null, name: '' })} loading={deleteLoading} />

      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar((p) => ({ ...p, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setSnackbar((p) => ({ ...p, open: false }))} severity={snackbar.severity}
          variant="filled" sx={{ borderRadius: 3 }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}