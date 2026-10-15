import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  Box,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  TextField,
  Select,
  Menu,
  MenuItem,
  FormControl,
  InputLabel,
  Skeleton,
  Snackbar,
  Alert,
  Card,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Autocomplete,
} from '@mui/material';
import {
  AddRounded as AddIcon,
  ArrowBackRounded as BackIcon,
  EditRounded as EditIcon,
  DeleteRounded as DeleteIcon,
  MoreVertRounded as MoreIcon,
} from '@mui/icons-material';
import { getProjectByIdApi } from '../../api/projectApi';
import { getTasksApi, createTaskApi, deleteTaskApi } from '../../api/taskApi';
import { getWorkspaceMembersApi } from '../../api/workspaceMemberApi';
import TaskDetailModal from '../tasks/TaskDetailModal';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { on } from '../../utils/eventBus';
import { taskStatusColors, projectStatusColors } from '../../utils/constants';

const priorityColors = {
  LOW: { bg: '#EEF3F0', text: '#66736F' },
  MEDIUM: { bg: '#D9F3ED', text: '#115E59' },
  HIGH: { bg: '#FEF3C7', text: '#D97706' },
  URGENT: { bg: '#FEE2E2', text: '#DC2626' },
};

export default function ProjectBoardPage() {
  const { id: workspaceId, projectId, taskId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [project, setProject] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [members, setMembers] = useState([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({ title: '', description: '', priority: 'MEDIUM', status: 'TODO', assigneeEmail: '', deadline: '' });
  const [createLoading, setCreateLoading] = useState(false);
  const [selectedTaskId, setSelectedTaskId] = useState(null);
  const [editTaskOnOpen, setEditTaskOnOpen] = useState(false);
  const [deleteTask, setDeleteTask] = useState({ open: false, id: null });
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [taskMenu, setTaskMenu] = useState({ anchorEl: null, task: null });

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [projRes, tasksRes, membersRes] = await Promise.all([
        getProjectByIdApi(projectId),
        getTasksApi({ projectId }),
        getWorkspaceMembersApi(),
      ]);
      setProject(projRes?.data?.data ?? projRes?.data);
      setTasks(tasksRes?.data?.data?.content ?? []);
      const allMembers = membersRes?.data?.data ?? membersRes?.data ?? [];
      setMembers(Array.isArray(allMembers) ? allMembers.filter((m) => m.workspaceId === Number(workspaceId)) : []);
    } catch {
      setSnackbar({ open: true, message: 'Failed to load project', severity: 'error' });
    } finally { setLoading(false); }
  }, [projectId, workspaceId]);

  // oxlint-disable-next-line react/set-state-in-effect -- tải dữ liệu theo route hiện tại
  useEffect(() => { fetchData(); }, [fetchData]);

  useEffect(() => {
    const unsub1 = on('TASK_CREATED', (notif) => {
      if (notif.projectId === Number(projectId)) fetchData();
    });
    const unsub2 = on('TASK_DELETED', (notif) => {
      if (notif.projectId === Number(projectId)) fetchData();
    });
    const unsub3 = on('TASK_UPDATED', (notif) => {
      if (notif.projectId === Number(projectId)) fetchData();
    });
    return () => { unsub1(); unsub2(); unsub3(); };
  }, [projectId, fetchData]);

  useEffect(() => {
    if (taskId && tasks.length > 0) {
      // oxlint-disable-next-line react/set-state-in-effect -- đồng bộ modal với task trên URL
      setEditTaskOnOpen(false);
      setSelectedTaskId(Number(taskId));
    }
  }, [taskId, tasks]);

  const isOwner = members.some((m) => m.user?.id === user?.id && m.role === 'OWNER');

  const handleCreateTask = async () => {
    if (!createForm.title.trim()) return;
    setCreateLoading(true);
    try {
      const { data: res } = await createTaskApi({
        projectId: Number(projectId), title: createForm.title, description: createForm.description,
        priority: createForm.priority, status: createForm.status || 'TODO',
        assigneeEmail: createForm.assigneeEmail || null,
        deadline: createForm.deadline || null,
      });
      setTasks((prev) => [...prev, res?.data ?? res]);
      setCreateOpen(false);
      setCreateForm({ title: '', description: '', priority: 'MEDIUM', status: 'TODO', assigneeEmail: '', deadline: '' });
      setSnackbar({ open: true, message: 'Task created', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to create task', severity: 'error' });
    } finally { setCreateLoading(false); }
  };

  const handleDeleteTask = async () => {
    setDeleteLoading(true);
    try {
      await deleteTaskApi(deleteTask.id);
      setTasks((prev) => prev.filter((t) => t.id !== deleteTask.id));
      setDeleteTask({ open: false, id: null });
      setSnackbar({ open: true, message: 'Task deleted', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to delete task', severity: 'error' });
    } finally { setDeleteLoading(false); }
  };

  if (loading) {
    return (
      <Box>
        <Skeleton variant="text" width={300} height={40} sx={{ borderRadius: 2 }} />
        <Skeleton variant="text" width={200} height={24} sx={{ mb: 2, borderRadius: 2 }} />
        <Skeleton variant="rounded" height={400} sx={{ borderRadius: 4 }} />
      </Box>
    );
  }

  if (!project) {
    return (
      <EmptyState title="Project not found"
        action={<Button startIcon={<BackIcon />} onClick={() => navigate(`/workspaces/${workspaceId}`)} sx={{ color: '#0F766E' }}>Back</Button>}
      />
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: { xs: 'flex-start', sm: 'center' }, gap: 1.5, mb: 6 }}>
        <IconButton onClick={() => navigate(`/workspaces/${workspaceId}`)} size="small" sx={{ bgcolor: '#F7FAF8', borderRadius: 2 }}>
          <BackIcon sx={{ fontSize: 20, color: '#66736F' }} />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h4" sx={{ color: '#17201F' }}>{project.name}</Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 0.5 }}>
            {(() => {
              const pc = projectStatusColors[project.status] || { bg: '#EEF3F0', text: '#66736F' };
              return <Chip label={project.status} size="small" sx={{ fontWeight: 600, fontSize: '1rem', bgcolor: pc.bg, color: pc.text }} />;
            })()}
            {project.startDate && <Typography variant="caption" sx={{ color: '#84918D' }}>Start: {project.startDate}</Typography>}
            {project.endDate && <Typography variant="caption" sx={{ color: '#84918D' }}>End: {project.endDate}</Typography>}
          </Box>
        </Box>
        {isOwner && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            Create Task
          </Button>
        )}
      </Box>

      <TableContainer component={Card} variant="outlined" sx={{ borderRadius: 3, border: '1px solid #DCE4E0' }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Assignee</TableCell>
              <TableCell>Deadline</TableCell>
              {isOwner && <TableCell align="right">Actions</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {tasks.length === 0 ? (
              <TableRow><TableCell colSpan={isOwner ? 6 : 5} align="center" sx={{ py: 5 }}>
                <EmptyState title="No tasks" description="Create your first task" />
              </TableCell></TableRow>
            ) : (
              tasks.map((task) => (
                <TableRow key={task.id} hover sx={{ cursor: 'pointer', '&:hover': { bgcolor: '#EEF3F0' } }}
                  onClick={() => { setEditTaskOnOpen(false); setSelectedTaskId(task.id); }}>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600} sx={{ color: '#17201F' }}>{task.title}</Typography>
                    {task.description && (
                      <Typography variant="caption" noWrap sx={{ maxWidth: 250, display: 'block', color: '#84918D' }}>
                        {task.description}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip label={task.priority} size="small"
                      sx={{ fontWeight: 600, fontSize: '0.8125rem', bgcolor: priorityColors[task.priority]?.bg || '#EEF3F0', color: priorityColors[task.priority]?.text || '#66736F' }} />
                  </TableCell>
                  <TableCell>
                    <Chip label={task.status?.replace('_', ' ')} size="small"
                      sx={{ fontWeight: 600, fontSize: '0.8125rem', bgcolor: taskStatusColors[task.status]?.bg || '#EEF3F0', color: taskStatusColors[task.status]?.text || '#66736F' }} />
                  </TableCell>
                  <TableCell>
                    {task.assignee ? (
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                        <Box sx={{ width: 26, height: 26, borderRadius: '50%', bgcolor: '#0F766E', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 13, fontWeight: 700 }}>
                          {task.assignee.username?.[0]?.toUpperCase() || '?'}
                        </Box>
                        <Typography variant="caption" sx={{ color: '#42504C' }}>{task.assignee.username}</Typography>
                      </Box>
                    ) : <Typography variant="caption" sx={{ color: '#CBD5D1' }}>Unassigned</Typography>}
                  </TableCell>
                  <TableCell><Typography variant="caption" sx={{ color: '#84918D' }}>{task.deadline || '-'}</Typography></TableCell>
                  {isOwner && (
                    <TableCell align="right">
                      <IconButton
                        aria-label={`Task actions for ${task.title}`}
                        size="small"
                        onClick={(event) => {
                          event.stopPropagation();
                          setTaskMenu({ anchorEl: event.currentTarget, task });
                        }}
                        sx={{ color: 'text.secondary' }}
                      >
                        <MoreIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Menu
        anchorEl={taskMenu.anchorEl}
        open={Boolean(taskMenu.anchorEl)}
        onClose={() => setTaskMenu({ anchorEl: null, task: null })}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
      >
        <MenuItem onClick={() => {
          const task = taskMenu.task;
          setTaskMenu({ anchorEl: null, task: null });
          setEditTaskOnOpen(true);
          setSelectedTaskId(task.id);
        }}>
          <EditIcon fontSize="small" sx={{ mr: 2, color: 'primary.main' }} />
          Edit task
        </MenuItem>
        <MenuItem onClick={() => {
          const task = taskMenu.task;
          setTaskMenu({ anchorEl: null, task: null });
          setDeleteTask({ open: true, id: task.id });
        }} sx={{ color: 'error.main' }}>
          <DeleteIcon fontSize="small" sx={{ mr: 2 }} />
          Delete task
        </MenuItem>
      </Menu>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F' }}>Create Task</DialogTitle>
        <DialogContent>
          <TextField autoFocus fullWidth label="Title" value={createForm.title}
            onChange={(e) => setCreateForm((p) => ({ ...p, title: e.target.value }))} sx={{ mt: 1, mb: 2 }} required />
          <TextField fullWidth label="Description" value={createForm.description}
            onChange={(e) => setCreateForm((p) => ({ ...p, description: e.target.value }))} multiline rows={3} sx={{ mb: 2 }} />
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Priority</InputLabel>
            <Select value={createForm.priority} label="Priority" onChange={(e) => setCreateForm((p) => ({ ...p, priority: e.target.value }))}>
              <MenuItem value="LOW">Low</MenuItem>
              <MenuItem value="MEDIUM">Medium</MenuItem>
              <MenuItem value="HIGH">High</MenuItem>
              <MenuItem value="URGENT">Urgent</MenuItem>
            </Select>
          </FormControl>
          <Autocomplete fullWidth sx={{ mb: 2 }} options={members}
            getOptionLabel={(o) => `${o.user?.email} (${o.user?.username})`}
            onChange={(_, v) => setCreateForm((p) => ({ ...p, assigneeEmail: v ? v.user.email : '' }))}
            renderInput={(params) => <TextField {...params} label="Assignee (by email)" />}
            noOptionsText="No members found" />
          <TextField fullWidth type="date" value={createForm.deadline}
            onChange={(e) => setCreateForm((p) => ({ ...p, deadline: e.target.value }))} />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCreateOpen(false)} disabled={createLoading} sx={{ color: '#66736F' }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateTask} disabled={createLoading}
            sx={{ borderRadius: 2, background: '#0F766E' }}>
            {createLoading ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {selectedTaskId && (
        <TaskDetailModal taskId={selectedTaskId} open={Boolean(selectedTaskId)}
          initialEditMode={editTaskOnOpen}
          onClose={() => { setSelectedTaskId(null); setEditTaskOnOpen(false); if (taskId) navigate(`/workspaces/${workspaceId}/projects/${projectId}`); }}
          onUpdate={(updatedTask) => setTasks((prev) => prev.map((t) => (t.id === updatedTask.id ? updatedTask : t)))}
          onDelete={(deletedTaskId) => { setTasks((prev) => prev.filter((t) => t.id !== deletedTaskId)); setSelectedTaskId(null); setEditTaskOnOpen(false); if (taskId) navigate(`/workspaces/${workspaceId}/projects/${projectId}`); }}
          isOwner={isOwner} currentUserId={user?.id} />
      )}

      <ConfirmDialog open={deleteTask.open} title="Delete Task" message="Are you sure?" confirmText="Delete"
        onConfirm={handleDeleteTask} onCancel={() => setDeleteTask({ open: false, id: null })} loading={deleteLoading} />

      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar((p) => ({ ...p, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setSnackbar((p) => ({ ...p, open: false }))} severity={snackbar.severity}
          variant="filled" sx={{ borderRadius: 3 }}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
