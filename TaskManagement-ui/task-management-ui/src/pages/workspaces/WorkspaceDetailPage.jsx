import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Typography, Tabs, Tab, Card, CardContent, Grid, Button,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  IconButton, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Select, Menu, MenuItem, FormControl, InputLabel,
  Skeleton, Alert, Snackbar, Tooltip,
} from '@mui/material';
import {
  AddRounded as AddIcon, EditRounded as EditIcon, DeleteRounded as DeleteIcon,
  FolderRounded as FolderIcon, PeopleRounded as PeopleIcon,
  SettingsRounded as SettingsIcon, ArrowBackRounded as BackIcon,
  PersonAddRounded as PersonAddIcon, MoreVertRounded as MoreIcon,
  BarChartRounded as DashboardIcon, CalendarMonthRounded as CalendarIcon,
  SwapHorizRounded as TransferIcon,
  RefreshRounded as ResendIcon,
} from '@mui/icons-material';
import {
  getWorkspaceByIdApi, updateWorkspaceApi, deleteWorkspaceApi,
  transferWorkspaceOwnershipApi,
} from '../../api/workspaceApi';
import {
  getWorkspaceMembersApi,
  updateWorkspaceMemberApi, deleteWorkspaceMemberApi,
} from '../../api/workspaceMemberApi';
import {
  createInvitationApi, getWorkspaceInvitationsApi,
  resendInvitationApi,
} from '../../api/workspaceInvitationApi';
import { getProjectsApi, createProjectApi, updateProjectApi, deleteProjectApi } from '../../api/projectApi';
import { useAuth } from '../../contexts/AuthContext';
import { emit, on } from '../../utils/eventBus';
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
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tabValue, setTabValue] = useState(0);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState({ name: '', description: '' });
  const [editLoading, setEditLoading] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteForm, setInviteForm] = useState({ email: '', role: 'MEMBER' });
  const [inviteLoading, setInviteLoading] = useState(false);
  const [resendingInviteId, setResendingInviteId] = useState(null);
  const [transferOwner, setTransferOwner] = useState({ open: false, member: null });
  const [transferOwnerLoading, setTransferOwnerLoading] = useState(false);
  const [roleChange, setRoleChange] = useState({ open: false, memberId: null, role: 'MEMBER' });
  const [removeMember, setRemoveMember] = useState({ open: false, memberId: null });
  const [deleteProject, setDeleteProject] = useState({ open: false, id: null, name: '' });
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const [createProjectForm, setCreateProjectForm] = useState({ name: '', description: '', endDate: '' });
  const [createProjectLoading, setCreateProjectLoading] = useState(false);
  const [projectMenu, setProjectMenu] = useState({ anchorEl: null, project: null });
  const [editProjectOpen, setEditProjectOpen] = useState(false);
  const [editProjectForm, setEditProjectForm] = useState({ id: null, name: '', description: '', status: 'PLANNING', endDate: '' });
  const [editProjectLoading, setEditProjectLoading] = useState(false);

  const currentUserMember = members.find((m) => m.user?.id === user?.id);
  const isOwner = currentUserMember?.role === 'OWNER';
  const isAdmin = currentUserMember?.role === 'ADMIN' || isOwner;

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
      const currentMembers = Array.isArray(allMembers) ? allMembers.filter((m) => m.workspaceId === Number(id)) : [];
      setMembers(currentMembers);

      // Fetch invitations if current user is admin or owner
      const myMember = currentMembers.find((m) => m.user?.id === user?.id);
      if (myMember?.role === 'OWNER' || myMember?.role === 'ADMIN') {
        try {
          const invRes = await getWorkspaceInvitationsApi(id);
          setInvitations(invRes?.data?.data ?? []);
        } catch {
          setInvitations([]);
        }
      }
    } catch {
      setSnackbar({ open: true, message: 'Failed to load workspace', severity: 'error' });
    } finally { setLoading(false); }
  }, [id, user?.id]);

  // oxlint-disable-next-line react/set-state-in-effect -- tải dữ liệu theo workspace hiện tại
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
      emit('workspace:project-count-changed', { workspaceId: Number(id), delta: -1 });
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
      emit('workspace:project-count-changed', { workspaceId: Number(id), delta: 1 });
      setCreateProjectOpen(false);
      setCreateProjectForm({ name: '', description: '', endDate: '' });
      setSnackbar({ open: true, message: 'Project created', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Create failed', severity: 'error' });
    } finally { setCreateProjectLoading(false); }
  };

  const openEditProject = () => {
    const project = projectMenu.project;
    if (!project) return;
    setEditProjectForm({
      id: project.id,
      name: project.name,
      description: project.description || '',
      status: project.status || 'PLANNING',
      endDate: project.endDate || '',
    });
    setProjectMenu({ anchorEl: null, project: null });
    setEditProjectOpen(true);
  };

  const handleUpdateProject = async () => {
    if (!editProjectForm.name.trim()) return;
    setEditProjectLoading(true);
    try {
      const payload = {
        name: editProjectForm.name.trim(),
        description: editProjectForm.description,
        status: editProjectForm.status,
      };
      if (editProjectForm.endDate) {
        payload.endDate = editProjectForm.endDate;
      } else {
        payload.clearEndDate = true;
      }
      const { data: res } = await updateProjectApi(editProjectForm.id, payload);
      const updatedProject = res?.data ?? res;
      setProjects((prev) => prev.map((project) => project.id === updatedProject.id ? updatedProject : project));
      setEditProjectOpen(false);
      setSnackbar({ open: true, message: 'Project updated', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Update failed', severity: 'error' });
    } finally { setEditProjectLoading(false); }
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
      emit('workspace:deleted', Number(id));
      navigate('/', { replace: true });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Delete failed', severity: 'error' });
      setDeleteLoading(false);
      setDeleteOpen(false);
    }
  };

  const handleSendInvite = async () => {
    if (!inviteForm.email) return;
    setInviteLoading(true);
    try {
      await createInvitationApi(Number(id), { email: inviteForm.email.trim(), role: inviteForm.role });
      setInviteOpen(false);
      setInviteForm({ email: '', role: 'MEMBER' });
      setSnackbar({ open: true, message: 'Member added; email and in-app notifications queued', severity: 'success' });
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to send invitation', severity: 'error' });
    } finally { setInviteLoading(false); }
  };

  const handleResendInvite = async (invId) => {
    setResendingInviteId(invId);
    try {
      await resendInvitationApi(invId);
      setSnackbar({ open: true, message: 'Workspace access notification resent', severity: 'success' });
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to resend invitation', severity: 'error' });
    } finally { setResendingInviteId(null); }
  };

  const handleTransferOwnership = async () => {
    if (!transferOwner.member) return;
    setTransferOwnerLoading(true);
    try {
      const { data: response } = await transferWorkspaceOwnershipApi(Number(id), { newOwnerUserId: transferOwner.member.user?.id });
      emit('workspace:owner-changed', response.data);
      setTransferOwner({ open: false, member: null });
      setSnackbar({ open: true, message: 'Ownership transferred successfully. You are now an Admin.', severity: 'success' });
      fetchData();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Transfer failed', severity: 'error' });
    } finally { setTransferOwnerLoading(false); }
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
        action={<Button startIcon={<BackIcon />} onClick={() => navigate('/')} sx={{ color: '#0F766E' }}>Back to Workspaces</Button>} />
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <IconButton onClick={() => navigate('/')} size="small" sx={{ bgcolor: '#F7FAF8', borderRadius: 2 }}>
            <BackIcon sx={{ fontSize: 20, color: '#66736F' }} />
          </IconButton>
          <Box>
            <Typography variant="h4" sx={{ color: '#17201F' }}>{workspace.name}</Typography>
            <Typography variant="body2" sx={{ color: '#66736F' }}>{workspace.description || 'No description'}</Typography>
          </Box>
        </Box>
        <Box sx={{ display: 'flex', gap: 1.5 }}>
          <Button
            variant="outlined"
            startIcon={<CalendarIcon />}
            onClick={() => navigate(`/workspaces/${id}/calendar`)}
            sx={{ borderRadius: 2, borderColor: '#DCE4E0', color: '#17201F' }}
          >
            Calendar
          </Button>
          <Button
            variant="outlined"
            startIcon={<DashboardIcon />}
            onClick={() => navigate(`/workspaces/${id}/dashboard`)}
            sx={{ borderRadius: 2, borderColor: '#DCE4E0', color: '#17201F' }}
          >
            Dashboard
          </Button>
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
              sx={{ background: '#0F766E', borderRadius: 2 }}>
              Create Project
            </Button>
          )}
        </Box>
        {projects.length === 0 ? (
          <EmptyState icon={<FolderIcon sx={{ fontSize: 36 }} />} title="No projects yet" />
        ) : (
          <Grid container spacing={3}>
            {projects.map((project) => (
              <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={project.id}>
                <Card sx={{ borderRadius: 3, border: '1px solid #DCE4E0', cursor: 'pointer', position: 'relative',
                  '&:hover': { transform: 'translateY(-3px)', boxShadow: '0 8px 24px rgba(15,118,110,0.1)' },
                  transition: 'all 200ms ease'
                }} onClick={() => navigate(`/workspaces/${id}/projects/${project.id}`)}>
                  {isOwner && (
                    <IconButton
                      aria-label={`Project actions for ${project.name}`}
                      size="small"
                      onClick={(event) => {
                        event.stopPropagation();
                        setProjectMenu({ anchorEl: event.currentTarget, project });
                      }}
                      sx={{ position: 'absolute', top: 8, right: 8, color: 'text.secondary', bgcolor: 'background.paper', '&:hover': { bgcolor: 'action.hover' } }}
                    >
                      <MoreIcon fontSize="small" />
                    </IconButton>
                  )}
                  <CardContent sx={{ p: 3, pr: isOwner ? 13 : 3 }}>
                    <Box sx={{ width: 42, height: 42, borderRadius: 2.5, bgcolor: '#EEF3F0',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2 }}>
                      <FolderIcon sx={{ color: '#0F766E', fontSize: 20 }} />
                    </Box>
                    <Typography variant="h6" fontWeight={700} noWrap sx={{ mb: 0.5, color: '#17201F' }}>
                      {project.name}
                    </Typography>
                    <Typography variant="body2" noWrap sx={{ mb: 2, color: '#84918D' }}>
                      {project.description || 'No description'}
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {(() => {
                        const pc = projectStatusColors[project.status] || { bg: '#EEF3F0', text: '#66736F' };
                        return <Chip label={project.status} size="small" sx={{ fontWeight: 600, fontSize: '0.8125rem', bgcolor: pc.bg, color: pc.text }} />;
                      })()}
                      {project.endDate && (
                        <Typography variant="caption" sx={{ color: '#84918D' }}>Due: {project.endDate}</Typography>
                      )}
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        <Menu
          anchorEl={projectMenu.anchorEl}
          open={Boolean(projectMenu.anchorEl)}
          onClose={() => setProjectMenu({ anchorEl: null, project: null })}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        >
          <MenuItem onClick={openEditProject}>
            <EditIcon fontSize="small" sx={{ mr: 2, color: 'primary.main' }} />
            Edit project
          </MenuItem>
          <MenuItem onClick={() => {
            const project = projectMenu.project;
            setProjectMenu({ anchorEl: null, project: null });
            setDeleteProject({ open: true, id: project.id, name: project.name });
          }} sx={{ color: 'error.main' }}>
            <DeleteIcon fontSize="small" sx={{ mr: 2 }} />
            Delete project
          </MenuItem>
        </Menu>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" fontWeight={700} sx={{ color: '#17201F' }}>
            Workspace Members ({members.length})
          </Typography>
          {isAdmin && (
            <Button
              variant="contained"
              startIcon={<PersonAddIcon />}
              onClick={() => setInviteOpen(true)}
              sx={{ borderRadius: 2, background: '#0F766E' }}
            >
              Add Member
            </Button>
          )}
        </Box>
        <TableContainer component={Card} variant="outlined" sx={{ borderRadius: 3, border: '1px solid #DCE4E0', mb: 4 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>User</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Joined At</TableCell>
                {(isOwner || isAdmin) && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {members.length === 0 ? (
                <TableRow><TableCell colSpan={4} align="center" sx={{ py: 4 }}>No members</TableCell></TableRow>
              ) : (
                members.map((member) => (
                  <TableRow key={member.id}>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Box sx={{ width: 34, height: 34, borderRadius: '50%', bgcolor: '#0F766E',
                          display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 700, fontSize: 15 }}>
                          {member.user?.username?.[0]?.toUpperCase() || '?'}
                        </Box>
                        <Box>
                          <Typography variant="body2" fontWeight={600} sx={{ color: '#17201F' }}>
                            {member.user?.username || 'Unknown'}
                          </Typography>
                          <Typography variant="caption" sx={{ color: '#84918D' }}>{member.user?.email || ''}</Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={member.role}
                        size="small"
                        sx={{
                          fontWeight: 600,
                          fontSize: '0.8125rem',
                          bgcolor: member.role === 'OWNER' ? '#FEF3C7' : member.role === 'ADMIN' ? '#E0F2FE' : '#EEF3F0',
                          color: member.role === 'OWNER' ? '#D97706' : member.role === 'ADMIN' ? '#0369A1' : '#66736F',
                        }}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ color: '#84918D' }}>
                        {member.joinedAt ? new Date(member.joinedAt).toLocaleDateString() : '-'}
                      </Typography>
                    </TableCell>
                    {(isOwner || isAdmin) && (
                      <TableCell align="right">
                        {member.role !== 'OWNER' && (
                          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                            {isOwner && (
                              <>
                                <Tooltip title="Transfer Ownership">
                                  <IconButton
                                    size="small"
                                    onClick={() => setTransferOwner({ open: true, member })}
                                    sx={{ color: '#D97706' }}
                                  >
                                    <TransferIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                                <Tooltip title="Change role">
                                  <IconButton
                                    size="small"
                                    onClick={() => setRoleChange({ open: true, memberId: member.id, role: member.role })}
                                  >
                                    <EditIcon fontSize="small" sx={{ color: '#0F766E' }} />
                                  </IconButton>
                                </Tooltip>
                              </>
                            )}
                            {(isOwner || (isAdmin && member.role === 'MEMBER')) && (
                              <Tooltip title="Remove">
                                <IconButton
                                  size="small"
                                  onClick={() => setRemoveMember({ open: true, memberId: member.id })}
                                >
                                  <DeleteIcon fontSize="small" sx={{ color: '#EF4444' }} />
                                </IconButton>
                              </Tooltip>
                            )}
                          </Box>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Membership notification history for Admin / Owner */}
        {isAdmin && (
          <Box sx={{ mt: 3 }}>
            <Typography variant="h6" fontWeight={700} sx={{ mb: 2, color: '#17201F' }}>
              Member Additions ({invitations.length})
            </Typography>
            <TableContainer component={Card} variant="outlined" sx={{ borderRadius: 3, border: '1px solid #DCE4E0' }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Member Email</TableCell>
                    <TableCell>Role</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Added At</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {invitations.length === 0 ? (
                    <TableRow><TableCell colSpan={5} align="center" sx={{ py: 3, color: '#84918D' }}>No members added yet</TableCell></TableRow>
                  ) : (
                    invitations.map((inv) => (
                      <TableRow key={inv.id}>
                        <TableCell><Typography variant="body2" fontWeight={600}>{inv.email}</Typography></TableCell>
                        <TableCell><Chip label={inv.role} size="small" sx={{ fontSize: '0.775rem' }} /></TableCell>
                        <TableCell>
                          <Chip
                            label={inv.status}
                            size="small"
                            sx={{
                              fontSize: '0.775rem',
                              fontWeight: 700,
                              bgcolor: ['PENDING', 'EXPIRED'].includes(inv.status) ? '#FEF3C7' : ['ADDED', 'ACCEPTED'].includes(inv.status) ? '#DCFCE7' : '#FEE2E2',
                              color: ['PENDING', 'EXPIRED'].includes(inv.status) ? '#D97706' : ['ADDED', 'ACCEPTED'].includes(inv.status) ? '#16A34A' : '#DC2626',
                            }}
                          />
                        </TableCell>
                        <TableCell><Typography variant="caption">{inv.createdAt ? new Date(inv.createdAt).toLocaleString() : '-'}</Typography></TableCell>
                        <TableCell align="right">
                          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                            {(inv.status === 'PENDING' || inv.status === 'EXPIRED' || inv.status === 'ADDED' || inv.status === 'ACCEPTED') && (
                              <>
                                <Tooltip title="Resend access notification">
                                  <IconButton size="small" disabled={resendingInviteId === inv.id} onClick={() => handleResendInvite(inv.id)}>
                                    <ResendIcon fontSize="small" sx={{ color: '#0284C7' }} />
                                  </IconButton>
                                </Tooltip>
                              </>
                            )}
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </TabPanel>

      {isOwner && (
        <TabPanel value={tabValue} index={2}>
          <Card variant="outlined" sx={{ maxWidth: 500, borderRadius: 3, border: '1px solid #DCE4E0' }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" fontWeight={700} sx={{ mb: 2.5, color: '#17201F' }}>Workspace Settings</Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <TextField fullWidth label="Name" defaultValue={workspace.name} size="small" />
                <TextField fullWidth label="Description" defaultValue={workspace.description || ''} size="small" multiline rows={2} />
                <Box sx={{ display: 'flex', gap: 1.5 }}>
                   <Button variant="contained" onClick={() => { setEditForm({ name: workspace.name, description: workspace.description || '' }); setEditOpen(true); }}
                    sx={{ borderRadius: 2, background: '#0F766E' }}>
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
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F' }}>Edit Workspace</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth label="Name" value={editForm.name}
            onChange={(e) => setEditForm((p) => ({ ...p, name: e.target.value }))} sx={{ mt: 1, mb: 2 }} />
          <TextField fullWidth label="Description" value={editForm.description}
            onChange={(e) => setEditForm((p) => ({ ...p, description: e.target.value }))} multiline rows={3} />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setEditOpen(false)} disabled={editLoading} sx={{ color: '#66736F' }}>Cancel</Button>
          <Button variant="contained" onClick={handleEdit} disabled={editLoading} sx={{ borderRadius: 2 }}>{editLoading ? 'Saving...' : 'Save'}</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteOpen} title="Delete Workspace" message="This action cannot be undone." confirmText="Delete"
        onConfirm={handleDelete} onCancel={() => setDeleteOpen(false)} loading={deleteLoading} />

      <Dialog open={inviteOpen} onClose={() => setInviteOpen(false)} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F' }}>Add Member to Workspace</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth label="Email address" type="email" value={inviteForm.email}
            helperText="Email must belong to an existing account. Access is added immediately."
            onChange={(e) => setInviteForm((p) => ({ ...p, email: e.target.value }))} sx={{ mt: 1, mb: 2 }} />
          <FormControl fullWidth>
            <InputLabel>Role</InputLabel>
            <Select value={inviteForm.role} label="Role" onChange={(e) => setInviteForm((p) => ({ ...p, role: e.target.value }))}>
              <MenuItem value="MEMBER">Member</MenuItem>
              <MenuItem value="ADMIN">Admin</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setInviteOpen(false)} disabled={inviteLoading} sx={{ color: '#66736F' }}>Cancel</Button>
          <Button variant="contained" onClick={handleSendInvite} disabled={inviteLoading || !inviteForm.email.trim()}
            sx={{ borderRadius: 2, background: '#0F766E' }}>
            {inviteLoading ? 'Adding...' : 'Add Member'}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={transferOwner.open}
        title="Transfer Workspace Ownership"
        message={`Are you sure you want to transfer workspace ownership to "${transferOwner.member?.user?.username || 'this member'}"? You will be demoted to Admin. Each workspace always has exactly one Owner.`}
        confirmText="Transfer Ownership"
        onConfirm={handleTransferOwnership}
        onCancel={() => setTransferOwner({ open: false, member: null })}
        loading={transferOwnerLoading}
      />

      <Dialog open={createProjectOpen} onClose={() => setCreateProjectOpen(false)} maxWidth="sm" fullWidth
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F' }}>Create Project</DialogTitle>
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
          <Button onClick={() => { setCreateProjectOpen(false); setCreateProjectForm({ name: '', description: '', endDate: '' }); }} sx={{ color: '#66736F' }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateProject} disabled={createProjectLoading || !createProjectForm.name.trim()}
            sx={{ borderRadius: 2, background: '#0F766E' }}>
            {createProjectLoading ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={editProjectOpen} onClose={() => setEditProjectOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Project</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 4, mt: 1 }}>
            <TextField autoFocus fullWidth required label="Name" value={editProjectForm.name}
              onChange={(e) => setEditProjectForm((prev) => ({ ...prev, name: e.target.value }))} />
            <TextField fullWidth label="Description" value={editProjectForm.description} multiline rows={3}
              onChange={(e) => setEditProjectForm((prev) => ({ ...prev, description: e.target.value }))} />
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select value={editProjectForm.status} label="Status"
                onChange={(e) => setEditProjectForm((prev) => ({ ...prev, status: e.target.value }))}>
                <MenuItem value="PLANNING">Planning</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="ARCHIVED">Archived</MenuItem>
              </Select>
            </FormControl>
            <TextField fullWidth type="date" label="End date" value={editProjectForm.endDate}
              slotProps={{ inputLabel: { shrink: true } }}
              onChange={(e) => setEditProjectForm((prev) => ({ ...prev, endDate: e.target.value }))} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditProjectOpen(false)} disabled={editProjectLoading} color="inherit">Cancel</Button>
          <Button variant="contained" onClick={handleUpdateProject}
            disabled={editProjectLoading || !editProjectForm.name.trim()}>
            {editProjectLoading ? 'Saving...' : 'Save changes'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={roleChange.open} onClose={() => setRoleChange({ open: false, memberId: null, role: 'MEMBER' })} maxWidth="xs" fullWidth
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F' }}>Change Role</DialogTitle>
        <DialogContent>
          <FormControl fullWidth>
            <InputLabel>Role</InputLabel>
            <Select value={roleChange.role} label="Role" onChange={(e) => setRoleChange((p) => ({ ...p, role: e.target.value }))}>
              <MenuItem value="MEMBER">Member</MenuItem>
              <MenuItem value="ADMIN">Admin</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setRoleChange({ open: false, memberId: null, role: 'MEMBER' })} sx={{ color: '#66736F' }}>Cancel</Button>
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
