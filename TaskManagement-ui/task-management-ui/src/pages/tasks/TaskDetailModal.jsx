import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Skeleton,
  Snackbar,
  Alert,
  Checkbox,
  LinearProgress,
} from '@mui/material';
import {
  CloseRounded as CloseIcon,
  DeleteRounded as DeleteIcon,
  SendRounded as SendIcon,
  EditRounded as EditIcon,
  HowToRegRounded as ClaimIcon,
  LocalOfferRounded as TagIcon,
  PlaylistAddCheckRounded as ChecklistIcon,
  AccountTreeRounded as DependencyIcon,
  SubdirectoryArrowRightRounded as SubtaskIcon,
} from '@mui/icons-material';
import { getTaskByIdApi, updateTaskApi, deleteTaskApi, getSubtasksApi, createTaskApi, getTasksApi } from '../../api/taskApi';
import { getCommentsByTaskApi, createCommentApi } from '../../api/commentApi';
import { getChecklistApi, addChecklistItemApi, updateChecklistItemApi, deleteChecklistItemApi } from '../../api/checklistApi';
import { getWorkspaceTagsApi, createTagApi, attachTagApi, detachTagApi } from '../../api/tagApi';
import { getDependenciesApi, addDependencyApi, deleteDependencyApi } from '../../api/dependencyApi';
import { taskStatusColors } from '../../utils/constants';
import { useNotification } from '../../contexts/NotificationContext';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import ConfirmDialog from '../../components/common/ConfirmDialog';

dayjs.extend(relativeTime);

const priorityColors = {
  LOW: { bg: '#EEF3F0', text: '#66736F' },
  MEDIUM: { bg: '#D9F3ED', text: '#115E59' },
  HIGH: { bg: '#FEF3C7', text: '#D97706' },
  URGENT: { bg: '#FEE2E2', text: '#DC2626' },
};

export default function TaskDetailModal({ taskId, open, initialEditMode = false, onClose, onUpdate, onDelete, isOwner, currentUserId }) {
  const { stompSubscribe } = useNotification();
  const stompCommentSubRef = useRef(null);
  const stompChecklistSubRef = useRef(null);

  const [task, setTask] = useState(null);
  const [comments, setComments] = useState([]);
  const [subtasks, setSubtasks] = useState([]);
  const [checklist, setChecklist] = useState([]);
  const [tags, setTags] = useState([]);
  const [workspaceTags, setWorkspaceTags] = useState([]);
  const [dependencies, setDependencies] = useState([]);
  const [projectTasks, setProjectTasks] = useState([]);

  const [loading, setLoading] = useState(true);
  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [editLoading, setEditLoading] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  // Subtask creation
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');
  const [subtaskLoading, setSubtaskLoading] = useState(false);

  // Checklist creation
  const [newChecklistTitle, setNewChecklistTitle] = useState('');
  const [checklistLoading, setChecklistLoading] = useState(false);

  // Tag creation / attach
  const [selectedTagId, setSelectedTagId] = useState('');
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState('#0F766E');
  const [createTagOpen, setCreateTagOpen] = useState(false);

  // Dependency creation
  const [depPrereqTaskId, setDepPrereqTaskId] = useState('');
  const [depType, setDepType] = useState('DEPENDS_ON');
  const [depLoading, setDepLoading] = useState(false);

  const fetchTask = useCallback(async () => {
    if (!taskId) return;
    setLoading(true);
    try {
      const [taskRes, commentsRes, subtasksRes, checklistRes, depsRes] = await Promise.all([
        getTaskByIdApi(taskId),
        getCommentsByTaskApi(taskId),
        getSubtasksApi(taskId).catch(() => ({ data: [] })),
        getChecklistApi(taskId).catch(() => ({ data: [] })),
        getDependenciesApi(taskId).catch(() => ({ data: [] })),
      ]);

      const taskData = taskRes?.data?.data ?? taskRes?.data;
      setTask(taskData);
      setTags(taskData?.tags ?? []);

      const loadedComments = commentsRes?.data?.data ?? commentsRes?.data ?? [];
      setComments(Array.isArray(loadedComments) ? loadedComments.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)) : []);

      const loadedSubtasks = subtasksRes?.data?.data ?? subtasksRes?.data ?? [];
      setSubtasks(Array.isArray(loadedSubtasks) ? loadedSubtasks : []);

      const loadedChecklist = checklistRes?.data?.data ?? checklistRes?.data ?? [];
      setChecklist(Array.isArray(loadedChecklist) ? loadedChecklist : []);

      const loadedDeps = depsRes?.data?.data ?? depsRes?.data ?? [];
      setDependencies(Array.isArray(loadedDeps) ? loadedDeps : []);

      setEditForm({
        title: taskData?.title || '',
        description: taskData?.description || '',
        priority: taskData?.priority || 'MEDIUM',
        status: taskData?.status || 'TODO',
        assigneeId: taskData?.assignee?.id || '',
        deadline: taskData?.deadline || '',
      });
      setEditMode(initialEditMode);

      // Fetch workspace tags if available
      if (taskData?.workspaceId) {
        try {
          const wsTagsRes = await getWorkspaceTagsApi(taskData.workspaceId);
          setWorkspaceTags(wsTagsRes?.data?.data ?? wsTagsRes?.data ?? []);
        } catch {
          setWorkspaceTags([]);
        }
      }

      // Fetch other project tasks for dependencies
      if (taskData?.projectId) {
        try {
          const pTasksRes = await getTasksApi({ projectId: taskData.projectId, size: 100 });
          const allTasks = pTasksRes?.data?.data?.content ?? pTasksRes?.data?.content ?? [];
          setProjectTasks(allTasks.filter((t) => t.id !== Number(taskId)));
        } catch {
          setProjectTasks([]);
        }
      }
    } catch {
      setSnackbar({ open: true, message: 'Failed to load task', severity: 'error' });
    } finally {
      setLoading(false);
    }
  }, [taskId, initialEditMode]);

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- tải task khi mở modal
    if (open) fetchTask();
  }, [open, fetchTask]);

  // STOMP subscriptions
  useEffect(() => {
    if (stompCommentSubRef.current) { stompCommentSubRef.current.unsubscribe(); stompCommentSubRef.current = null; }
    if (stompChecklistSubRef.current) { stompChecklistSubRef.current.unsubscribe(); stompChecklistSubRef.current = null; }

    if (!open || !task?.workspaceId) return;

    // Comments subscription
    stompCommentSubRef.current = stompSubscribe(
      `/topic/workspace/${task.workspaceId}/tasks/${taskId}/comments`,
      (comment) => {
        setComments((prev) => {
          if (prev.some((c) => c.id === comment.id)) return prev;
          return [comment, ...prev];
        });
      }
    );

    // Checklist subscription
    stompChecklistSubRef.current = stompSubscribe(
      `/topic/workspace/${task.workspaceId}/tasks/${taskId}/checklist`,
      (updatedList) => {
        if (Array.isArray(updatedList)) {
          setChecklist(updatedList);
        }
      }
    );

    return () => {
      if (stompCommentSubRef.current) { stompCommentSubRef.current.unsubscribe(); stompCommentSubRef.current = null; }
      if (stompChecklistSubRef.current) { stompChecklistSubRef.current.unsubscribe(); stompChecklistSubRef.current = null; }
    };
  }, [open, task?.workspaceId, taskId, stompSubscribe]);

  const handleSave = async () => {
    setEditLoading(true);
    try {
      const payload = {
        title: editForm.title,
        description: editForm.description,
        priority: editForm.priority,
        status: editForm.status,
        assigneeId: editForm.assigneeId ? Number(editForm.assigneeId) : null,
      };
      if (editForm.deadline) {
        payload.deadline = editForm.deadline;
      } else {
        payload.clearDeadline = true;
      }
      const { data: res } = await updateTaskApi(taskId, payload);
      const updated = res?.data ?? res;
      setTask(updated);
      setEditMode(false);
      onUpdate(updated);
      setSnackbar({ open: true, message: 'Task updated', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Update failed', severity: 'error' });
    } finally { setEditLoading(false); }
  };

  const handleDelete = async () => {
    setDeleteLoading(true);
    try {
      await deleteTaskApi(taskId);
      onDelete(taskId);
      setDeleteOpen(false);
      onClose();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Delete failed', severity: 'error' });
    } finally { setDeleteLoading(false); }
  };

  const handleClaim = async () => {
    if (!currentUserId) return;
    setEditLoading(true);
    try {
      const { data: res } = await updateTaskApi(taskId, { assigneeId: Number(currentUserId) });
      const updated = res?.data ?? res;
      setTask(updated);
      onUpdate(updated);
      setSnackbar({ open: true, message: 'Task claimed', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Claim failed', severity: 'error' });
    } finally { setEditLoading(false); }
  };

  const handleStatusChange = async (newStatus) => {
    setEditLoading(true);
    try {
      const { data: res } = await updateTaskApi(taskId, { status: newStatus });
      const updated = res?.data ?? res;
      setTask(updated);
      onUpdate(updated);
      setSnackbar({ open: true, message: 'Status updated', severity: 'success' });
    } catch (err) {
      setSnackbar({
        open: true,
        message: err.response?.data?.message || 'Cannot update status. Check dependencies.',
        severity: 'error',
      });
    } finally { setEditLoading(false); }
  };

  const handleAddComment = async () => {
    if (!commentText.trim()) return;
    setCommentLoading(true);
    try {
      const { data: res } = await createCommentApi({ taskId: Number(taskId), content: commentText });
      setComments((prev) => {
        const newComment = res?.data ?? res;
        if (prev.some((c) => c.id === newComment.id)) return prev;
        return [newComment, ...prev];
      });
      setCommentText('');
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to add comment', severity: 'error' });
    } finally { setCommentLoading(false); }
  };

  // Subtask handlers
  const handleAddSubtask = async () => {
    if (!newSubtaskTitle.trim()) return;
    setSubtaskLoading(true);
    try {
      const { data: res } = await createTaskApi({
        title: newSubtaskTitle.trim(),
        projectId: task.projectId,
        parentTaskId: Number(taskId),
      });
      const newSub = res?.data ?? res;
      setSubtasks((prev) => [...prev, newSub]);
      setNewSubtaskTitle('');
      setSnackbar({ open: true, message: 'Subtask added', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to add subtask', severity: 'error' });
    } finally { setSubtaskLoading(false); }
  };

  // Checklist handlers
  const handleToggleChecklist = async (item) => {
    try {
      const { data: res } = await updateChecklistItemApi(taskId, item.id, {
        isCompleted: !item.isCompleted,
      });
      const updated = res?.data ?? res;
      setChecklist((prev) => prev.map((it) => (it.id === item.id ? updated : it)));
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to update checklist item', severity: 'error' });
    }
  };

  const handleAddChecklistItem = async () => {
    if (!newChecklistTitle.trim()) return;
    setChecklistLoading(true);
    try {
      const { data: res } = await addChecklistItemApi(taskId, { title: newChecklistTitle.trim() });
      const newItem = res?.data ?? res;
      setChecklist((prev) => [...prev, newItem]);
      setNewChecklistTitle('');
      setSnackbar({ open: true, message: 'Checklist item added', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to add item', severity: 'error' });
    } finally { setChecklistLoading(false); }
  };

  const handleDeleteChecklistItem = async (itemId) => {
    try {
      await deleteChecklistItemApi(taskId, itemId);
      setChecklist((prev) => prev.filter((it) => it.id !== itemId));
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to delete item', severity: 'error' });
    }
  };

  // Tag handlers
  const handleAttachTag = async (tagId) => {
    if (!tagId) return;
    try {
      await attachTagApi(taskId, tagId);
      const attached = workspaceTags.find((t) => t.id === Number(tagId));
      if (attached) {
        setTags((prev) => [...prev, attached]);
      }
      setSelectedTagId('');
      setSnackbar({ open: true, message: 'Tag attached', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to attach tag', severity: 'error' });
    }
  };

  const handleDetachTag = async (tagId) => {
    try {
      await detachTagApi(taskId, tagId);
      setTags((prev) => prev.filter((t) => t.id !== tagId));
      setSnackbar({ open: true, message: 'Tag detached', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to detach tag', severity: 'error' });
    }
  };

  const handleCreateAndAttachTag = async () => {
    if (!newTagName.trim()) return;
    try {
      const { data: res } = await createTagApi(task.workspaceId, {
        name: newTagName.trim(),
        color: newTagColor,
      });
      const createdTag = res?.data ?? res;
      await attachTagApi(taskId, createdTag.id);
      setWorkspaceTags((prev) => [...prev, createdTag]);
      setTags((prev) => [...prev, createdTag]);
      setCreateTagOpen(false);
      setNewTagName('');
      setSnackbar({ open: true, message: 'Tag created and attached', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to create tag', severity: 'error' });
    }
  };

  // Dependency handlers
  const handleAddDependency = async () => {
    if (!depPrereqTaskId) return;
    setDepLoading(true);
    try {
      const { data: res } = await addDependencyApi(taskId, {
        prerequisiteTaskId: Number(depPrereqTaskId),
        dependencyType: depType,
      });
      const newDep = res?.data ?? res;
      setDependencies((prev) => [...prev, newDep]);
      setDepPrereqTaskId('');
      setSnackbar({ open: true, message: 'Dependency added', severity: 'success' });
    } catch (err) {
      setSnackbar({
        open: true,
        message: err.response?.data?.message || 'Failed to add dependency (cycle detected or invalid)',
        severity: 'error',
      });
    } finally { setDepLoading(false); }
  };

  const handleDeleteDependency = async (depId) => {
    try {
      await deleteDependencyApi(taskId, depId);
      setDependencies((prev) => prev.filter((d) => d.id !== depId));
      setSnackbar({ open: true, message: 'Dependency removed', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to remove dependency', severity: 'error' });
    }
  };

  const sc = (s) => taskStatusColors[s] || { bg: '#EEF3F0', text: '#66736F' };
  const pc = (p) => priorityColors[p] || { bg: '#EEF3F0', text: '#66736F' };

  const isOverdue = task?.deadline && task?.status !== 'DONE' && dayjs(task.deadline).isBefore(dayjs(), 'day');
  const isDueSoon = task?.deadline && task?.status !== 'DONE' && !isOverdue && dayjs(task.deadline).diff(dayjs(), 'day') <= 2;

  const completedSubtasks = subtasks.filter((s) => s.status === 'DONE').length;
  const subtaskProgress = subtasks.length > 0 ? Math.round((completedSubtasks / subtasks.length) * 100) : 0;

  const completedChecklist = checklist.filter((it) => it.isCompleted).length;

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth
        PaperProps={{ sx: { borderRadius: 4, overflow: 'hidden' } }}>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 3, pt: 2.5, pb: 1.5, fontWeight: 700, color: '#17201F' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, overflow: 'hidden' }}>
            <Typography variant="h6" fontWeight={700} noWrap>
              {loading ? 'Loading...' : task?.title}
            </Typography>
            {task?.parentTaskId && (
              <Chip size="small" icon={<SubtaskIcon sx={{ fontSize: 14 }} />} label="Subtask" sx={{ height: 22, fontSize: '0.9625rem' }} />
            )}
          </Box>
          <Box>
            {!loading && isOwner && (
              <IconButton onClick={() => setDeleteOpen(true)} size="small" sx={{ mr: 0.5, color: '#EF4444' }}>
                <DeleteIcon />
              </IconButton>
            )}
            <IconButton onClick={onClose} size="small" sx={{ color: '#84918D' }}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <Divider />
        <DialogContent sx={{ pt: 2.5, px: 3 }}>
          {loading ? (
            <Box>
              <Skeleton variant="text" width="60%" height={32} sx={{ borderRadius: 2 }} />
              <Skeleton variant="text" width="40%" height={24} sx={{ mt: 1, borderRadius: 2 }} />
              <Skeleton variant="rounded" height={120} sx={{ mt: 2, borderRadius: 3 }} />
            </Box>
          ) : editMode ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField fullWidth label="Title" value={editForm.title}
                onChange={(e) => setEditForm((p) => ({ ...p, title: e.target.value }))} />
              <TextField fullWidth label="Description" value={editForm.description}
                onChange={(e) => setEditForm((p) => ({ ...p, description: e.target.value }))} multiline rows={3} />
              <FormControl fullWidth><InputLabel>Priority</InputLabel>
                <Select value={editForm.priority} label="Priority" onChange={(e) => setEditForm((p) => ({ ...p, priority: e.target.value }))}>
                  <MenuItem value="LOW">Low</MenuItem>
                  <MenuItem value="MEDIUM">Medium</MenuItem>
                  <MenuItem value="HIGH">High</MenuItem>
                  <MenuItem value="URGENT">Urgent</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth><InputLabel>Status</InputLabel>
                <Select value={editForm.status} label="Status" onChange={(e) => setEditForm((p) => ({ ...p, status: e.target.value }))}>
                  <MenuItem value="TODO">To Do</MenuItem>
                  <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                  <MenuItem value="DONE">Done</MenuItem>
                </Select>
              </FormControl>
              <TextField fullWidth type="date" value={editForm.deadline}
                onChange={(e) => setEditForm((p) => ({ ...p, deadline: e.target.value }))} />
              <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                <Button onClick={() => setEditMode(false)} disabled={editLoading} sx={{ color: '#66736F' }}>Cancel</Button>
                <Button variant="contained" onClick={handleSave} disabled={editLoading}
                  sx={{ borderRadius: 2, background: '#0F766E' }}>
                  {editLoading ? 'Saving...' : 'Save'}
                </Button>
              </Box>
            </Box>
          ) : (
            <Box>
              {/* Status, Priority, Assignee, Deadlines & Badges */}
              <Box sx={{ display: 'flex', gap: 1.5, mb: 2, flexWrap: 'wrap', alignItems: 'center' }}>
                <FormControl size="small" sx={{ minWidth: 130 }}>
                  <Select
                    value={task?.status || 'TODO'}
                    onChange={(e) => handleStatusChange(e.target.value)}
                    disabled={editLoading}
                    sx={{
                      fontWeight: 600, fontSize: '1.0625rem', borderRadius: 2,
                      '& .MuiSelect-select': { py: 0.5 },
                      bgcolor: sc(task?.status).bg, color: sc(task?.status).text,
                      '& .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
                      '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#CBD5D1' },
                    }}
                  >
                    <MenuItem value="TODO">To Do</MenuItem>
                    <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                    <MenuItem value="DONE">Done</MenuItem>
                  </Select>
                </FormControl>

                <Chip label={task?.priority} size="small"
                  sx={{ fontWeight: 600, fontSize: '1rem', bgcolor: pc(task?.priority).bg, color: pc(task?.priority).text }} />

                {task?.assignee && (
                  <Chip
                    avatar={<Avatar sx={{ width: 22, height: 22, bgcolor: '#0F766E', fontSize: 16 }}>{task.assignee.username?.[0]?.toUpperCase()}</Avatar>}
                    label={task.assignee.username} size="small" variant="outlined" sx={{ fontWeight: 500, borderColor: '#CBD5D1' }}
                  />
                )}

                {task?.deadline && (
                  <Chip label={`Due: ${task.deadline}`} size="small" variant="outlined" sx={{ fontWeight: 500, borderColor: '#CBD5D1' }} />
                )}

                {isOverdue && (
                  <Chip label="OVERDUE" size="small" sx={{ bgcolor: '#FEE2E2', color: '#DC2626', fontWeight: 700, fontSize: '0.9625rem' }} />
                )}
                {isDueSoon && (
                  <Chip label="DUE SOON" size="small" sx={{ bgcolor: '#FEF3C7', color: '#D97706', fontWeight: 700, fontSize: '0.9625rem' }} />
                )}
              </Box>

              {/* Tags Section */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', mb: 2.5 }}>
                <TagIcon sx={{ fontSize: 18, color: '#84918D' }} />
                {tags.map((tg) => (
                  <Chip
                    key={tg.id}
                    label={tg.name}
                    size="small"
                    onDelete={() => handleDetachTag(tg.id)}
                    sx={{
                      bgcolor: tg.color ? `${tg.color}20` : '#EEF3F0',
                      color: tg.color || '#0F766E',
                      fontWeight: 600,
                      borderColor: tg.color || '#CBD5D1',
                      borderWidth: 1,
                      borderStyle: 'solid',
                    }}
                  />
                ))}

                {/* Attach existing tag dropdown */}
                <FormControl size="small" sx={{ minWidth: 110 }}>
                  <Select
                    value={selectedTagId}
                    displayEmpty
                    onChange={(e) => {
                      setSelectedTagId(e.target.value);
                      if (e.target.value) handleAttachTag(e.target.value);
                    }}
                    sx={{ height: 28, fontSize: '1.0625rem', borderRadius: 2 }}
                  >
                    <MenuItem value="" disabled>+ Attach Tag</MenuItem>
                    {workspaceTags
                      .filter((wt) => !tags.some((t) => t.id === wt.id))
                      .map((wt) => (
                        <MenuItem key={wt.id} value={wt.id}>{wt.name}</MenuItem>
                      ))}
                  </Select>
                </FormControl>

                <Button
                  size="small"
                  onClick={() => setCreateTagOpen(true)}
                  sx={{ textTransform: 'none', fontSize: '1.0625rem', color: '#0F766E' }}
                >
                  + New Tag
                </Button>
              </Box>

              {task?.description && (
                <Typography variant="body2" sx={{ color: '#42504C', mb: 3, whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>
                  {task.description}
                </Typography>
              )}

              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mb: 2 }}>
                {!isOwner && !task?.assignee && currentUserId && (
                  <Button size="small" onClick={handleClaim} disabled={editLoading}
                    startIcon={<ClaimIcon sx={{ fontSize: 16 }} />}
                    sx={{ color: '#10B981', fontWeight: 600, textTransform: 'none' }}>
                    Claim
                  </Button>
                )}
                <Button size="small" onClick={() => setEditMode(true)}
                  startIcon={<EditIcon sx={{ fontSize: 16 }} />}
                  sx={{ color: '#0F766E', fontWeight: 600, textTransform: 'none' }}>
                  Edit
                </Button>
              </Box>

              <Divider sx={{ my: 2 }} />

              {/* Subtasks Section */}
              {!task.parentTaskId && (
                <Box sx={{ mb: 3 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <SubtaskIcon sx={{ fontSize: 20, color: '#0F766E' }} />
                      <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#17201F' }}>
                        Subtasks ({completedSubtasks}/{subtasks.length})
                      </Typography>
                    </Box>
                    <Typography variant="caption" sx={{ color: '#84918D', fontWeight: 600 }}>
                      {subtaskProgress}%
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={subtaskProgress}
                    sx={{ height: 6, borderRadius: 3, bgcolor: '#EEF3F0', mb: 1.5, '& .MuiLinearProgress-bar': { bgcolor: '#0F766E' } }}
                  />
                  <List disablePadding dense>
                    {subtasks.map((st) => (
                      <ListItem
                        key={st.id}
                        sx={{
                          py: 0.5,
                          px: 1,
                          borderRadius: 2,
                          bgcolor: '#F7FAF8',
                          mb: 0.5,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                        }}
                      >
                        <Typography
                          variant="body2"
                          sx={{
                            textDecoration: st.status === 'DONE' ? 'line-through' : 'none',
                            color: st.status === 'DONE' ? '#84918D' : '#17201F',
                            fontWeight: 500,
                          }}
                        >
                          {st.title}
                        </Typography>
                        <Chip
                          label={st.status}
                          size="small"
                          sx={{ fontSize: '0.9625rem', height: 20, bgcolor: sc(st.status).bg, color: sc(st.status).text }}
                        />
                      </ListItem>
                    ))}
                  </List>
                  <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                    <TextField
                      size="small"
                      placeholder="Add subtask..."
                      value={newSubtaskTitle}
                      onChange={(e) => setNewSubtaskTitle(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleAddSubtask(); } }}
                      sx={{ flex: 1 }}
                    />
                    <Button
                      variant="outlined"
                      size="small"
                      onClick={handleAddSubtask}
                      disabled={subtaskLoading || !newSubtaskTitle.trim()}
                      sx={{ borderColor: '#DCE4E0', color: '#0F766E' }}
                    >
                      Add
                    </Button>
                  </Box>
                </Box>
              )}

              {/* Checklist Section */}
              <Box sx={{ mb: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <ChecklistIcon sx={{ fontSize: 20, color: '#0F766E' }} />
                  <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#17201F' }}>
                    Checklist ({completedChecklist}/{checklist.length})
                  </Typography>
                </Box>
                <List disablePadding dense>
                  {checklist.map((item) => (
                    <ListItem
                      key={item.id}
                      sx={{
                        py: 0.25,
                        px: 0.5,
                        borderRadius: 2,
                        '&:hover': { bgcolor: '#F7FAF8' },
                        display: 'flex',
                        alignItems: 'center',
                      }}
                    >
                      <Checkbox
                        size="small"
                        checked={item.isCompleted}
                        onChange={() => handleToggleChecklist(item)}
                        sx={{ color: '#CBD5D1', '&.Mui-checked': { color: '#0F766E' }, p: 0.5, mr: 1 }}
                      />
                      <Typography
                        variant="body2"
                        sx={{
                          flex: 1,
                          textDecoration: item.isCompleted ? 'line-through' : 'none',
                          color: item.isCompleted ? '#84918D' : '#17201F',
                        }}
                      >
                        {item.title}
                      </Typography>
                      <IconButton size="small" onClick={() => handleDeleteChecklistItem(item.id)} sx={{ color: '#84918D', p: 0.5 }}>
                        <CloseIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                    </ListItem>
                  ))}
                </List>
                <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                  <TextField
                    size="small"
                    placeholder="Add checklist item..."
                    value={newChecklistTitle}
                    onChange={(e) => setNewChecklistTitle(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleAddChecklistItem(); } }}
                    sx={{ flex: 1 }}
                  />
                  <Button
                    variant="outlined"
                    size="small"
                    onClick={handleAddChecklistItem}
                    disabled={checklistLoading || !newChecklistTitle.trim()}
                    sx={{ borderColor: '#DCE4E0', color: '#0F766E' }}
                  >
                    Add
                  </Button>
                </Box>
              </Box>

              {/* Task Dependencies Section */}
              <Box sx={{ mb: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <DependencyIcon sx={{ fontSize: 20, color: '#0F766E' }} />
                  <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#17201F' }}>
                    Task Dependencies ({dependencies.length})
                  </Typography>
                </Box>
                {dependencies.length === 0 ? (
                  <Typography variant="caption" sx={{ color: '#84918D', display: 'block', mb: 1 }}>
                    No dependencies set
                  </Typography>
                ) : (
                  <List disablePadding dense sx={{ mb: 1 }}>
                    {dependencies.map((dep) => (
                      <ListItem
                        key={dep.id}
                        sx={{
                          py: 0.5,
                          px: 1,
                          borderRadius: 2,
                          bgcolor: '#F7FAF8',
                          mb: 0.5,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                        }}
                      >
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Chip
                            label={dep.dependencyType}
                            size="small"
                            sx={{
                              fontSize: '0.9625rem',
                              height: 20,
                              fontWeight: 700,
                              bgcolor: dep.dependencyType === 'BLOCKS' ? '#FEF3C7' : '#E0F2FE',
                              color: dep.dependencyType === 'BLOCKS' ? '#D97706' : '#0369A1',
                            }}
                          />
                          <Typography variant="body2" fontWeight={500}>
                            {dep.prerequisiteTaskTitle || `Task #${dep.prerequisiteTaskId}`}
                          </Typography>
                        </Box>
                        <IconButton size="small" onClick={() => handleDeleteDependency(dep.id)} sx={{ color: '#EF4444' }}>
                          <CloseIcon sx={{ fontSize: 16 }} />
                        </IconButton>
                      </ListItem>
                    ))}
                  </List>
                )}
                {projectTasks.length > 0 && (
                  <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                    <FormControl size="small" sx={{ flex: 1 }}>
                      <Select
                        value={depPrereqTaskId}
                        displayEmpty
                        onChange={(e) => setDepPrereqTaskId(e.target.value)}
                        sx={{ fontSize: '1.125rem' }}
                      >
                        <MenuItem value="" disabled>Select Prerequisite Task</MenuItem>
                        {projectTasks
                          .filter((pt) => !dependencies.some((d) => d.prerequisiteTaskId === pt.id))
                          .map((pt) => (
                            <MenuItem key={pt.id} value={pt.id}>{pt.title}</MenuItem>
                          ))}
                      </Select>
                    </FormControl>
                    <FormControl size="small" sx={{ width: 140 }}>
                      <Select
                        value={depType}
                        onChange={(e) => setDepType(e.target.value)}
                        sx={{ fontSize: '1.125rem' }}
                      >
                        <MenuItem value="DEPENDS_ON">Depends On</MenuItem>
                        <MenuItem value="BLOCKS">Blocks</MenuItem>
                      </Select>
                    </FormControl>
                    <Button
                      variant="outlined"
                      size="small"
                      onClick={handleAddDependency}
                      disabled={depLoading || !depPrereqTaskId}
                      sx={{ borderColor: '#DCE4E0', color: '#0F766E' }}
                    >
                      Add
                    </Button>
                  </Box>
                )}
              </Box>

              <Divider sx={{ mb: 2 }} />

              {/* Comments Section */}
              <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5, color: '#17201F' }}>
                Comments ({comments.length})
              </Typography>

              <Box sx={{ mb: 2, maxHeight: 220, overflow: 'auto' }}>
                {comments.length === 0 ? (
                  <Typography variant="body2" sx={{ py: 3, textAlign: 'center', color: '#84918D' }}>No comments yet</Typography>
                ) : (
                  <List disablePadding dense>
                    {comments.map((comment) => (
                      <ListItem key={comment.id} alignItems="flex-start" disableGutters sx={{ px: 0, py: 1 }}>
                        <ListItemAvatar sx={{ minWidth: 40 }}>
                          <Avatar sx={{ width: 32, height: 32, bgcolor: '#0F766E', fontSize: 17, fontWeight: 700 }}>
                            {comment.user?.username?.[0]?.toUpperCase() || '?'}
                          </Avatar>
                        </ListItemAvatar>
                        <ListItemText
                          primary={
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Typography variant="subtitle2" fontSize={18} fontWeight={600} sx={{ color: '#17201F' }}>
                                {comment.user?.username || 'Unknown'}
                              </Typography>
                              <Typography variant="caption" sx={{ color: '#CBD5D1' }}>
                                {dayjs(comment.createdAt).fromNow()}
                              </Typography>
                            </Box>
                          }
                          secondary={<Typography variant="body2" sx={{ color: '#42504C', mt: 0.25 }}>{comment.content}</Typography>}
                        />
                      </ListItem>
                    ))}
                  </List>
                )}
              </Box>

              <Box sx={{ display: 'flex', gap: 1, pt: 1, borderTop: '1px solid #EEF3F0' }}>
                <TextField fullWidth size="small" placeholder="Write a comment..."
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAddComment(); } }} />
                <IconButton color="primary" onClick={handleAddComment} disabled={commentLoading || !commentText.trim()}
                  sx={{ bgcolor: '#EEF3F0', '&:hover': { bgcolor: '#D9F3ED' }, color: '#0F766E' }}>
                  <SendIcon />
                </IconButton>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={onClose} sx={{ color: '#66736F', fontWeight: 600 }}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Create New Tag Dialog */}
      <Dialog open={createTagOpen} onClose={() => setCreateTagOpen(false)} maxWidth="xs" fullWidth
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}>
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F' }}>Create New Tag</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            label="Tag Name"
            value={newTagName}
            onChange={(e) => setNewTagName(e.target.value)}
            sx={{ mt: 1, mb: 2 }}
          />
          <TextField
            fullWidth
            label="Color Hex"
            value={newTagColor}
            onChange={(e) => setNewTagColor(e.target.value)}
            helperText="e.g. #0F766E, #DC2626, #2563EB"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCreateTagOpen(false)} sx={{ color: '#66736F' }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateAndAttachTag} disabled={!newTagName.trim()}
            sx={{ borderRadius: 2, background: '#0F766E' }}>
            Create & Attach
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog open={deleteOpen} title="Delete Task" message="Are you sure you want to delete this task?" confirmText="Delete"
        onConfirm={handleDelete} onCancel={() => setDeleteOpen(false)} loading={deleteLoading} />

      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar((p) => ({ ...p, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setSnackbar((p) => ({ ...p, open: false }))} severity={snackbar.severity}
          variant="filled" sx={{ borderRadius: 3 }}>{snackbar.message}</Alert>
      </Snackbar>
    </>
  );
}
